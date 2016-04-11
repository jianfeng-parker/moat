package cn.ubuilding.moat.redis;


import redis.clients.jedis.JedisCommands;

/**
 * @author Wu Jianfeng
 * @since 15/12/27 16:07
 */

/**
 * @param <T> 方法返回值类型
 * @param <J> 执行Redis操作的类型(Jedis/ShardedJedis...)
 */
public interface RedisAction<T, J extends JedisCommands> {
    T doInRedis(J jedis);
}
