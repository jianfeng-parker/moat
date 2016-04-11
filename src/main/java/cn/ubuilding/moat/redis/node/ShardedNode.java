package cn.ubuilding.moat.redis.node;


import java.io.Serializable;

/**
 * @author Wu Jianfeng
 * @since 16/1/9 21:28
 */

public class ShardedNode implements Serializable {
    private static final long serialVersionUID = -5729240962727618011L;

    private String host;

    private int port;

    private String name;

    /**
     * Redis节点
     *
     * @param host 主机
     * @param port 端口
     */
    public ShardedNode(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public ShardedNode(String host, int port, String name) {
        this(host, port);
        this.name = name;
    }


    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getName() {
        return name;
    }

    public String toString() {
        return (null != name && name.trim().length() > 0) ? name + " " + host + ":" + port : host + ":" + port;
    }
}
