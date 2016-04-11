package cn.ubuilding.moat;


/**
 * @author Wu Jianfeng
 * @since 15/12/21 08:23
 * 用户生成Redis key的实现类
 */

public abstract class CacheKey {

    /**
     * 一个family代表一个业务,由用户自定义
     */
    protected String family;

    /**
     * 用户自定义的 和family一起组成key的一个原始值
     */
    protected String bizKey;

    public CacheKey(String family, String bizKey) {
        this.family = family;
        this.bizKey = bizKey;
    }

    /**
     * 具体的key值由各个子类具体实现
     *
     * @return redis key
     */
    public abstract String getKey();

    public abstract byte[] getBytes();

    /**
     * 次方法返回该组成该对象的key属性值
     * 注: 若想获取本对象值作为Redis的key值，请调用#getKey()
     *
     * @return 属性key的值
     */
    public String getBizKey() {
        return bizKey;
    }

    /**
     * 次方法返回该组成该对象的family属性值
     *
     * @return 属性family的值
     */
    public String getFamily() {
        return family;
    }
}
