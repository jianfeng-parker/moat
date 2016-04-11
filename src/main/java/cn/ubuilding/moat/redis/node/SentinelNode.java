package cn.ubuilding.moat.redis.node;


import java.io.Serializable;

/**
 * @author Wu Jianfeng
 * @since 16/1/9 21:30
 * Redis 哨兵节点信息类
 */

public class SentinelNode implements Serializable {
    private static final long serialVersionUID = 6696608367651045304L;

    private String host;

    private int port;

    /**
     * Redis 哨兵节点信息
     *
     * @param host sentinel所在主机
     * @param port sentinel端口
     */
    public SentinelNode(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String toString() {
        return host + ":" + port;
    }

}
