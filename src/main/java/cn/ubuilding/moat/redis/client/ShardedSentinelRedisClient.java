package cn.ubuilding.moat.redis.client;

import cn.ubuilding.moat.redis.RedisAction;
import cn.ubuilding.moat.redis.RedisClient;
import cn.ubuilding.moat.redis.node.SentinelNode;
import cn.ubuilding.moat.redis.pool.ShardedJedisSentinelPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.exceptions.JedisException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Wu Jianfeng
 * @since 16/1/13 08:38
 */

public class ShardedSentinelRedisClient extends RedisClient<ShardedJedis> {

    private ShardedJedisSentinelPool pool;

    public ShardedSentinelRedisClient(JedisPoolConfig poolConfig, List<String> masterNames, Set<SentinelNode> sentinels) {
        if (null == poolConfig) throw new IllegalArgumentException("jedis pool config is required");
        if (null == masterNames || masterNames.size() == 0)
            throw new IllegalArgumentException("master names are required");
        if (null == sentinels || sentinels.size() == 0)
            throw new IllegalArgumentException("sentinel nodes are required");
        this.pool = new ShardedJedisSentinelPool(poolConfig, masterNames, initSentinel(sentinels));
    }

    @Override
    protected <T> T execute(RedisAction<T, ShardedJedis> action) {
        ShardedJedis jedis = null;
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
    protected void closeJedis(ShardedJedis jedis) {
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
