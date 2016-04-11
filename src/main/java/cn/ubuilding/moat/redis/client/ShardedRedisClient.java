package cn.ubuilding.moat.redis.client;

import cn.ubuilding.moat.redis.RedisAction;
import cn.ubuilding.moat.redis.RedisClient;
import cn.ubuilding.moat.redis.node.ShardedNode;
import redis.clients.jedis.*;
import redis.clients.jedis.exceptions.JedisException;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Wu Jianfeng
 * @since 16/1/9 13:41
 * 封装对ShardedJedisPool的使用，提供一个对Redis分片的使用方式
 */

public class ShardedRedisClient extends RedisClient<ShardedJedis> {

    private ShardedJedisPool pool;

    /**
     * 初始化分片redis client实例
     */
    public ShardedRedisClient(JedisPoolConfig poolConfig, List<ShardedNode> nodes) {
        if (null == poolConfig) throw new IllegalArgumentException("jedis pool config must not be null");
        if (null == nodes || nodes.size() == 0)
            throw new IllegalArgumentException("redis sharded nodes must not be null");
        this.pool = new ShardedJedisPool(poolConfig, initShards(nodes));
    }

    protected <T> T execute(final RedisAction<T, ShardedJedis> action) {
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

    protected void closeJedis(ShardedJedis jedis) {
        try {
            if (null != jedis) {
                jedis.close();
            }
        } catch (JedisException e) {
            logger.debug("[ShardRedisClient]close jedis error:" + e.getMessage());
        }
    }

    private List<JedisShardInfo> initShards(List<ShardedNode> nodes) {
        if (null == nodes || nodes.size() == 0)
            throw new IllegalArgumentException("jedis pool config must not be null");
        List<JedisShardInfo> shards = new ArrayList<JedisShardInfo>(nodes.size());
        for (ShardedNode node : nodes) {
            shards.add(new JedisShardInfo(node.getHost(), node.getPort(), node.getName()));
        }
        return shards;
    }
}
