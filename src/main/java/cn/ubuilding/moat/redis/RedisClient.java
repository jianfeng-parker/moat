package cn.ubuilding.moat.redis;

import cn.ubuilding.moat.CacheKey;
import cn.ubuilding.moat.CacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisCommands;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.exceptions.JedisException;

import java.util.List;
import java.util.Map;

/**
 * @author Wu Jianfeng
 * @since 15/12/22 08:46
 */
public abstract class RedisClient<J extends JedisCommands> implements CacheService {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    public String get(final CacheKey key) {
        return execute(new RedisAction<String, J>() {
            public String doInRedis(J jedis) {
                return jedis.get(key.getKey());
            }
        });
    }

    public String set(final CacheKey key, final String value) {
        return execute(new RedisAction<String, J>() {
            public String doInRedis(J jedis) {
                return jedis.set(key.getKey(), value);
            }
        });
    }

    public Map<String, String> getMap(final CacheKey key) {
        return execute(new RedisAction<Map<String, String>, J>() {
            public Map<String, String> doInRedis(J jedis) {
                return jedis.hgetAll(key.getKey());
            }
        });
    }

    public String setMap(final CacheKey key, final Map<String, String> value) {
        return execute(new RedisAction<String, J>() {
            public String doInRedis(J jedis) {
                return jedis.hmset(key.getKey(), value);
            }
        });
    }

    public List<String> getByMapKeys(final CacheKey key, final String... mapKeys) {
        return execute(new RedisAction<List<String>, J>() {
            public List<String> doInRedis(J jedis) {
                return jedis.hmget(key.getKey(), mapKeys);

            }
        });
    }

    public String getByMapKey(final CacheKey key, final String mapKey) {
        return execute(new RedisAction<String, J>() {
            public String doInRedis(J jedis) {
                return jedis.hget(key.getKey(), mapKey);
            }
        });
    }

    public Long setByMapKey(final CacheKey key, final String mapKey, final String mapValue) {
        return execute(new RedisAction<Long, J>() {
            public Long doInRedis(J jedis) {
                return jedis.hset(key.getKey(), mapKey, mapValue);
            }
        });
    }

    public Long delete(final CacheKey key) {
        return execute(new RedisAction<Long, J>() {
            public Long doInRedis(J jedis) {
                return jedis.del(key.getKey());
            }
        });
    }


    public Long expire(final CacheKey key, final int seconds) {
        return execute(new RedisAction<Long, J>() {
            public Long doInRedis(J jedis) {
                return jedis.expire(key.getKey(), seconds);
            }
        });
    }


    protected abstract <T> T execute(final RedisAction<T, J> action);

    /**
     * 归还连接资源到 Pool中
     * 参考: http://stackoverflow.com/questions/17082163/jedis-when-to-use-returnbrokenresource
     * 在以前的Jedis版本中需要使用者自行决定调用 returnResource(jedis),还是returnBrokenResource(jedis)
     * 在该版本中已经不推荐使用者显示调用这两个API，而是改为调用jedis实例的API close()
     *
     * @param jedis 需要归还连接资源的Jedis实例
     */
    protected abstract void closeJedis(J jedis);

    /**
     * JedisException handle.
     *
     * @param e JedisException
     */
    protected void handleJedisException(JedisException e) {
        if (e instanceof JedisConnectionException) {
            logger.error("[RedisClient] Redis connection lost.", e);
        } else if (e instanceof JedisDataException) {
            if (null != e.getMessage() && e.getMessage().contains("READONLY")) {
                logger.error("[RedisClient] Redis connection are readonly.", e);
            } else {
                // data exception
                logger.error("[RedisClient] Jedis exception happen.", e);
            }
        } else {
            logger.error("[RedisClient] Jedis exception happen.", e);
        }
    }

}
