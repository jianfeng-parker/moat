package cn.ubuilding.moat;

import java.util.List;
import java.util.Map;

/**
 * @author Wu Jianfeng
 * @since 15/12/22 08:18
 */

public interface CacheService {

    /**
     * 根据Key获取值
     *
     * @param key redis key
     * @return value, 若不存在则返回null
     */
    String get(final CacheKey key);

    /**
     * 向Redis中存值String 类型的值
     *
     * @param key   redis key
     * @param value value
     * @return "OK"
     * @
     */
    String set(final CacheKey key, final String value);

    /**
     * 根据key 从Redis中取出整个Map
     *
     * @param key redis key
     * @return Map
     */
    Map<String, String> getMap(CacheKey key);

    /**
     * 向Redis中存入  整个Map
     *
     * @param value 待插入的Map
     * @return 操作结果
     * @
     */
    String setMap(CacheKey key, Map<String, String> value);

    /**
     * 从指定的Redis Key中取出 Map 多个key对应的值
     *
     * @param key     redis key
     * @param mapKeys Map 的key(即setMap()方法中 value的key)
     * @return map 的value
     */
    List<String> getByMapKeys(CacheKey key, String... mapKeys);

    /**
     * 从指定的Redis key中取出一个Map key对应的值
     *
     * @param key    redis key
     * @param mapKey redis key 对map的key
     * @return 操作结果
     */
    String getByMapKey(CacheKey key, String mapKey);

    /**
     * 向指定的Redis key中存入一个Map的 key-value
     *
     * @param key      redis key
     * @param mapKey   redis key 对map的key
     * @param mapValue redis key 对map的value
     * @return 操作结果
     */
    Long setByMapKey(CacheKey key, String mapKey, String mapValue);

    /**
     * 根据String形式的key 从Redis中删除数据
     *
     * @return 删除成功则返回一个大于零的整数，若key不存在，则返回0；
     * @
     */
    Long delete(final CacheKey key);

    /**
     * 为制定的key 设置超时时间
     *
     * @param key     redis key
     * @param seconds 秒数
     * @return true => 设置成功， false => 设置失败
     * @
     */
    Long expire(final CacheKey key, int seconds);

}
