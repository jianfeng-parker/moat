package cn.ubuilding.moat.redis;

import cn.ubuilding.moat.CacheKey;
import cn.ubuilding.moat.Serializer;
import redis.clients.util.MurmurHash;

/**
 * @author Wu Jianfeng
 * @since 15/12/27 17:39
 */

public class RedisKey extends CacheKey {

    public RedisKey(String family, String bizKey) {
        super(family, bizKey);
    }

    /**
     * 以对象实例的Hash值作为key
     *
     * @return redis key
     */
    public String getKey() {
        return String.valueOf(MurmurHash.hash64A(this.getBytes(), 1));

    }

    public byte[] getBytes() {
        return Serializer.serialize(this);
    }
}
