package cn.ubuilding.moat.redis.client;

import cn.ubuilding.moat.redis.RedisAction;
import cn.ubuilding.moat.redis.RedisClient;
import cn.ubuilding.moat.redis.node.SentinelNode;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisSentinelPool;
import redis.clients.jedis.exceptions.JedisException;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Wu Jianfeng
 * @since 16/1/9 14:53
 * 该客户端通过使用SentinelPool的方式获取Redis连接，即:
 * 适合单个Master-slave的部署方式，可感知主备切换，而无需客户端干预
 */

public class SentinelRedisClient extends RedisClient<Jedis> {

    private JedisSentinelPool pool;

    /**
     * 实例化哨兵方式的Redis客户端
     *
     * @param poolConfig jedis连接池配置
     * @param masterName master节点名称
     * @param sentinels  所有哨兵节点,格式 ip:port
     */
    public SentinelRedisClient(JedisPoolConfig poolConfig, String masterName, Set<SentinelNode> sentinels) {
        if (null == poolConfig) throw new IllegalArgumentException("jedis pool config must not be null");
        if (null == masterName || masterName.length() == 0)
            throw new IllegalArgumentException("masterName is required");
        if (null == sentinels || sentinels.size() == 0) throw new IllegalArgumentException("sentinel must not be null");
        this.pool = new JedisSentinelPool(masterName, initSentinel(sentinels), poolConfig);
    }

    @Override
    protected <T> T execute(RedisAction<T, Jedis> action) {
        Jedis jedis = null;
        try {
            jedis = pool.getResource();
            return action.doInRedis(jedis);
        } catch (JedisException e) {
            handleJedisException(e);
            return null;
        } finally {
            closeJedis(jedis);
        }
    }

    @Override
    protected void closeJedis(Jedis jedis) {
        try {
            if (null != jedis) {
                jedis.close();
            }
        } catch (JedisException e) {
            logger.debug("[SentinelRedisClient]close jedis error:" + e.getMessage());
        }
    }

    private Set<String> initSentinel(Set<SentinelNode> sentinels) {
        if (null == sentinels || sentinels.size() == 0) throw new IllegalArgumentException("sentinel must not be null");
        Set<String> set = new HashSet<String>(sentinels.size());
        for (SentinelNode sentinel : sentinels) {
            set.add(sentinel.toString());
        }
        return set;
    }

}
