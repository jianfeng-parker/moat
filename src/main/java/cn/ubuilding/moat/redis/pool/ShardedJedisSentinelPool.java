package cn.ubuilding.moat.redis.pool;

import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.*;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.util.Hashing;
import redis.clients.util.Pool;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * @author Wu Jianfeng
 * @since 16/1/10 20:38
 * <p/>
 * 以分片的方式使用sentinel，即:既可以使用多个master分片节点，又可以感知主从切换
 */

public class ShardedJedisSentinelPool extends Pool<ShardedJedis> {

    protected Logger log = Logger.getLogger(getClass().getName());

    public static final int MAX_RETRY_SENTINEL = 5;

    private int sentinelRetry = 1;

    protected GenericObjectPoolConfig poolConfig;

    protected int timeout = Protocol.DEFAULT_TIMEOUT;

    protected int database = Protocol.DEFAULT_DATABASE;

    protected String password;

    protected Set<MasterListener> masterListeners = new HashSet<MasterListener>();

    private volatile List<HostAndPort> currentMasters;

    public ShardedJedisSentinelPool(List<String> masters, Set<String> sentinels) {
        this(masters, sentinels, new GenericObjectPoolConfig(),
                Protocol.DEFAULT_TIMEOUT, null, Protocol.DEFAULT_DATABASE);
    }

    public ShardedJedisSentinelPool(List<String> masters, Set<String> sentinels, String password) {
        this(masters, sentinels, new GenericObjectPoolConfig(),
                Protocol.DEFAULT_TIMEOUT, password);
    }

    public ShardedJedisSentinelPool(final GenericObjectPoolConfig poolConfig, List<String> masters, Set<String> sentinels) {
        this(masters, sentinels, poolConfig, Protocol.DEFAULT_TIMEOUT, null, Protocol.DEFAULT_DATABASE);
    }

    public ShardedJedisSentinelPool(List<String> masters, Set<String> sentinels,
                                    final GenericObjectPoolConfig poolConfig, int timeout, final String password) {
        this(masters, sentinels, poolConfig, timeout, password,
                Protocol.DEFAULT_DATABASE);
    }

    public ShardedJedisSentinelPool(List<String> masters, Set<String> sentinels,
                                    final GenericObjectPoolConfig poolConfig, final int timeout) {
        this(masters, sentinels, poolConfig, timeout, null,
                Protocol.DEFAULT_DATABASE);
    }

    public ShardedJedisSentinelPool(List<String> masters, Set<String> sentinels,
                                    final GenericObjectPoolConfig poolConfig, final String password) {
        this(masters, sentinels, poolConfig, Protocol.DEFAULT_TIMEOUT,
                password);
    }


    public ShardedJedisSentinelPool(List<String> masterNames, Set<String> sentinels,
                                    final GenericObjectPoolConfig poolConfig, int timeout,
                                    final String password, final int database) {
        this.poolConfig = poolConfig;
        this.timeout = timeout;
        this.password = password;
        this.database = database;

        List<HostAndPort> masterList = initSentinels(sentinels, masterNames);
        initPool(masterList); // currentMasters中对象的顺序与List<String> masterNames中相应的名称顺序相同(index相同)
    }

    public void destroy() {
        for (MasterListener listener : masterListeners) {
            listener.shutdown();
        }
        super.destroy();
    }

    public List<HostAndPort> getCurrentMasters() {
        return currentMasters;
    }

    private List<HostAndPort> initSentinels(Set<String> sentinels, List<String> masterNames) {
        Map<String, HostAndPort> masterMap = new HashMap<String,HostAndPort>();
        List<HostAndPort> shardedMasters = new ArrayList<HostAndPort>();
        log.info("Trying to find all master from available Sentinels...");
        for (String masterName : masterNames) {
            HostAndPort master = null;
            boolean fetched = false;
            while (!fetched && sentinelRetry <= MAX_RETRY_SENTINEL) {
                for (String sentinel : sentinels) {
                    final HostAndPort hap = toHostAndPort(Arrays.asList(sentinel.split(":")));
                    log.info("Connecting to sentinel:" + hap);
                    try {
                        master = masterMap.get(masterName);
                        if (null == master) {
                            Jedis jedis = new Jedis(hap.getHost(), hap.getPort());
                            List<String> masterAddr = jedis.sentinelGetMasterAddrByName(masterName);
                            if (null != masterAddr && masterAddr.size() == 2) {
                                master = toHostAndPort(masterAddr);
                                log.fine("Found redis master:" + master);
                                masterMap.put(masterName, master);
                                shardedMasters.add(master); // 该list中的master顺序与 masterNames中的对应名称一致(及index相同)
                                // 根据当前masterName在sentinels中找到了master，则本次循环完成
                                // 进入下一个masterName的循环
                                fetched = true;
                                jedis.disconnect();
                                break;
                            } else {
                                log.warning("Can not get master address, master name: " + masterName + ". Sentinel: " + hap + ".");
                            }
                        }
                    } catch (JedisException e) {
                        log.warning("Cannot connect to sentinel @" + hap + ",reason is :" + e.getMessage() + ".Trying next one.");
                    }
                }
                // 所有sentinels循环结束还是没有找到当前masterName对应的master节点
                // 则暂停1秒钟，while循环会继续尝试根据当前masterName在sentinels中获取master节点
                // 直到达到最大尝试次数(MAX_RETRY_SENTINEL),若还未找到，则放弃当前masterName
                if (null == master) {
                    try {
                        log.warning("Have tried to find master by name(" + masterName + ") within all sentinels " + sentinelRetry + " times, but not found any one.");
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {
                    }
                    fetched = false;
                    sentinelRetry++;
                }
            }
            // 没有有效master节点，且每个masterName都超出了重试次数，则表明初始化连接池的参数有误
            if (!fetched && sentinelRetry > MAX_RETRY_SENTINEL) {
                log.severe("All sentinels down, and not found any available master(" + masterName + ") after trying " + MAX_RETRY_SENTINEL + " times. Abort!");
                throw new JedisConnectionException("cannot connect any sentinels:" + sentinels.toString());
            }
        }
        // 所有的master分片必须都可访问
        if (masterNames.size() > 0 && masterMap.size() == masterNames.size() && shardedMasters.size() == masterNames.size()) {
            log.info("Starting sentinels listeners...");
            for (String sentinel : sentinels) {
                final HostAndPort hap = toHostAndPort(Arrays.asList(sentinel.split(":")));
                MasterListener listener = new MasterListener(masterNames, hap.getHost(), hap.getPort());
                masterListeners.add(listener);
                listener.start();
            }
        }
        return shardedMasters;
    }

    private void initPool(List<HostAndPort> masters) {
        if (!sameHostAndPorts(currentMasters, masters)) {
            StringBuilder sb = new StringBuilder();
            for (HostAndPort master : masters) {
                sb.append(master.toString());
                sb.append(" ");
            }
            log.info("Create ShardedJedisSentinelPool to masters at[" + sb.toString() + "]");
            List<JedisShardInfo> shards = makeShards(masters);
            initPool(poolConfig, new ShardedJedisSentinelFactory(shards, Hashing.MURMUR_HASH, null));
            currentMasters = masters;
        }
    }

    /**
     * 将 "ip:port"格式的字符串转换成HostAndPort对象
     * 参考 JedisSentinel#toHostAndPort
     *
     * @param getMasterAddrByNameResult "ip:port"列表
     * @return HostAndPort 对象列表
     */
    private HostAndPort toHostAndPort(List<String> getMasterAddrByNameResult) {
        String host = getMasterAddrByNameResult.get(0);
        int port = Integer.parseInt(getMasterAddrByNameResult.get(1));
        return new HostAndPort(host, port);
    }

    private boolean sameHostAndPorts(List<HostAndPort> currentMasters, List<HostAndPort> masters) {
        if (null != currentMasters && null != masters) {
            if (currentMasters.size() == masters.size()) {
                for (int i = 0; i < currentMasters.size(); i++) {
                    // 从上下文看 两个List中存放的hap的顺序是一样的
                    // 这是由初始化该Pool的参数List<String> masterNames的顺序决定的
                    if (!currentMasters.get(i).equals(masters.get(i))) return false;
                }
                return true;
            }
        }
        return false;
    }

    private List<JedisShardInfo> makeShards(List<HostAndPort> masters) {
        List<JedisShardInfo> shardMasters = new ArrayList<JedisShardInfo>();
        for (HostAndPort hap : masters) {
            JedisShardInfo shardInfo = new JedisShardInfo(hap.getHost(), hap.getPort(), timeout);
            shardInfo.setPassword(password);
            shardMasters.add(shardInfo);
        }
        return shardMasters;
    }

    protected class MasterListener extends Thread {

        protected List<String> masterNames;

        protected String host;

        protected int port;

        protected long subscribeRetryWaitTimeMillis = 5000;

        protected Jedis jedis;

        protected AtomicBoolean running = new AtomicBoolean(false);

        public MasterListener(List<String> masterNames, String host, int port) {
            this.masterNames = masterNames;
            this.host = host;
            this.port = port;
        }

        public MasterListener(List<String> masterNames, String host, int port, long subscribeRetryWaitTimeMillis) {
            this(masterNames, host, port);
            this.subscribeRetryWaitTimeMillis = subscribeRetryWaitTimeMillis;
        }

        public void run() {
            running.set(true);
            while (running.get()) {
                jedis = new Jedis(host, port); // sentinel客户端
                try {
                    jedis.subscribe(new JedisPubSub() {
                        @Override
                        public void onMessage(String channel, String message) {
                            log.fine("Received message:" + message + " from sentinel(" + host + ":" + port + ")");
                            String[] switchMasterMsg = message.split(" ");
                            if (switchMasterMsg.length > 3) {
                                int index = masterNames.indexOf(switchMasterMsg[0]);// 找到发生主从切换的master节点名称在 masterNames中的index
                                if (index >= 0) {
                                    HostAndPort newMaster = toHostAndPort(Arrays.asList(switchMasterMsg[3], switchMasterMsg[4]));
                                    List<HostAndPort> newMasters = new ArrayList<HostAndPort>(currentMasters);
                                    newMasters.set(index, newMaster); // 根据index替换当前master分片列表中发生主从切换的master
                                    initPool(newMasters); // 重新初始化连接池
                                } else {
                                    StringBuilder sb = new StringBuilder();
                                    for (String masterName : masterNames) {
                                        sb.append(masterName);
                                        sb.append(",");
                                    }
                                    log.fine("Ignoring message on +switch-master for master name " + switchMasterMsg[0] + ", our master name is " + sb.toString());
                                }
                            } else {
                                log.severe("Invalid message received on Sentinel " + host + ":" + port + " on channel +switch-master: " + message);
                            }
                        }
                    }, "+switch-master");
                } catch (JedisException e) {
                    if (running.get()) {
                        log.severe("Lost connection to Sentinel at " + host + ":" + port + ". Sleeping " + subscribeRetryWaitTimeMillis + "ms and retrying.");
                        try {
                            Thread.sleep(subscribeRetryWaitTimeMillis);
                        } catch (InterruptedException ignored) {
                        }
                    } else {
                        log.fine("UnSubscribing from Sentinel at " + host + ":" + port);
                    }
                }
            }
        }

        public void shutdown() {
            try {
                log.fine("Shutdown listener on sentinel(" + host + ":" + port + ").");
                running.set(false);
                jedis.disconnect();
            } catch (Exception e) {
                log.severe("Caught exception while shutting down sentinel(" + host + ":" + port + ").");
            }
        }
    }

    /**
     * 该Factory的功能与ShardedJedisPool中的ShardedJedisFactory一致
     */
    protected static class ShardedJedisSentinelFactory implements PooledObjectFactory<ShardedJedis> {

        private List<JedisShardInfo> shards;

        private Hashing algo;

        private Pattern keyTagPattern;

        public ShardedJedisSentinelFactory(List<JedisShardInfo> shards, Hashing algo, Pattern keyTagPattern) {
            this.shards = shards;
            this.algo = algo;
            this.keyTagPattern = keyTagPattern;
        }

        public PooledObject<ShardedJedis> makeObject() throws Exception {
            ShardedJedis jedis = new ShardedJedis(shards, algo, keyTagPattern);
            return new DefaultPooledObject<ShardedJedis>(jedis);
        }

        public void destroyObject(PooledObject<ShardedJedis> pooledObject) throws Exception {
            final ShardedJedis shardedJedis = pooledObject.getObject();
            for (Jedis jedis : shardedJedis.getAllShards()) {
                try {
                    jedis.quit();
                } catch (Exception ignored) {

                }
                jedis.disconnect();
            }
        }

        public boolean validateObject(PooledObject<ShardedJedis> pooledObject) {
            try {
                ShardedJedis jedis = pooledObject.getObject();
                for (Jedis shard : jedis.getAllShards()) {
                    if (!"PONG".equals(shard.ping())) return false;
                }
                return true;

            } catch (Exception e) {
                return false;
            }
        }

        public void activateObject(PooledObject<ShardedJedis> pooledObject) throws Exception {

        }

        public void passivateObject(PooledObject<ShardedJedis> pooledObject) throws Exception {

        }
    }
}
