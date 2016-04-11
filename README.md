## NoSQL封装：
  从一个[Redis练手项目](https://github.com/jianfeng-parker/UBuilding/tree/master/ub-ocean)中抽出来
------

### Redis:

  > * 基于ShardedJedisPool封装一个分片客户端
      该pool的局限是对主从切换无感知: 分片中的每个节点都是可读/写的，若该分片是 主/从 部署结构，则在该分片发生主从切换后
      对该分片的操作将失败；
       
  > * 基于JedisSentinelPool封装一个对主从切换有感知的客户端
      该pool的局限是只能使用sentinel监控中的一个master-slave组: 从pool的使用方式可以看出只使用了一个masterName,即:
      即使sentinel监控了多个master-slave，在使用pool的时候也无法像使用分片的方式同时使用多个master-slave组；
      
  > * 实现一个ShardedJedisSentinelPool，以分片的方式使用多个master-slave组，且对每个m-s组的主从切换有感知，
      可动态调整连接池；基于该pool实现客户端；
      
#### 使用示例:
      
  ```xml
     
     <!--使用Redis分片配置:
          1. 连接的两个Redis服务都是Master节点，此处代码中对Redis的读写没有做区分；
          2. 若Redis存在主从切换，此处是无法感知的，那么原先连接的主节点就变成了从节点，所以会导致写操作失败；
          3. 此处暂时忽略主从切换的情况，默认连接的都是可 读/写 的节点(一般情况下若Redis以主从模式部署的话Master节点都是可 读/写)
         -->
         <bean id="shardRedisClient" class="cn.ubuilding.moat.redis.client.ShardedRedisClient">
             <constructor-arg index="0" ref="jedisPoolConfig"/>
             <constructor-arg index="1">
                 <list>
                     <bean class="cn.ubuilding.moat.redis.node.ShardedNode">
                         <constructor-arg index="0" value="192.168.1.105"/>
                         <constructor-arg index="1" value="6479"/>
                         <constructor-arg index="2" value="shard105"/>
                     </bean>
                     <bean class="cn.ubuilding.moat.redis.node.ShardedNode">
                         <constructor-arg index="0" value="192.168.1.104"/>
                         <constructor-arg index="1" value="6379"/>
                         <constructor-arg index="2" value="shard104"/>
                     </bean>
                 </list>
             </constructor-arg>
         </bean>
     
         <!--==============Jedis哨兵模式连接池(对主从切换有感知)============-->
         <!--使用哨兵模式的Jedis客户端实例-->
         <bean id="sentinelRedisClient" class="cn.ubuilding.moat.redis.client.SentinelRedisClient">
             <constructor-arg index="0" ref="jedisPoolConfig"/>
             <constructor-arg index="1" value="master_105"/>
             <constructor-arg index="2">
                 <!--sentinel节点信息(此处注意:不是Redis的ip和port)-->
                 <set>
                     <bean class="cn.ubuilding.moat.redis.node.SentinelNode">
                         <constructor-arg index="0" value="192.168.1.105"/>
                         <constructor-arg index="1" value="26379"/>
                     </bean>
                     <bean class="cn.ubuilding.moat.redis.node.SentinelNode">
                         <constructor-arg index="0" value="192.168.1.105"/>
                         <constructor-arg index="1" value="26479"/>
                     </bean>
                     <bean class="cn.ubuilding.moat.redis.node.SentinelNode">
                         <constructor-arg index="0" value="192.168.1.105"/>
                         <constructor-arg index="1" value="26579"/>
                     </bean>
                 </set>
             </constructor-arg>
         </bean>
     
         <!--================使用ShardedJedisSentinelPool================-->
         <bean id="shardSentinelRedisClient"
               class="cn.ubuilding.moat.redis.client.ShardedSentinelRedisClient">
             <constructor-arg index="0" ref="jedisPoolConfig"/>
             <constructor-arg index="1">
                 <list>
                     <value>master_104</value>
                     <value>master_105</value>
                 </list>
             </constructor-arg>
             <constructor-arg index="2">
                 <set>
                     <bean class="cn.ubuilding.moat.redis.node.SentinelNode">
                         <constructor-arg index="0" value="192.168.1.104"/>
                         <constructor-arg index="1" value="26379"/>
                     </bean>
                     <bean class="cn.ubuilding.moat.redis.node.SentinelNode">
                         <constructor-arg index="0" value="192.168.1.104"/>
                         <constructor-arg index="1" value="26479"/>
                     </bean>
                     <bean class="cn.ubuilding.moat.redis.node.SentinelNode">
                         <constructor-arg index="0" value="192.168.1.104"/>
                         <constructor-arg index="1" value="26579"/>
                     </bean>
                 </set>
             </constructor-arg>
         </bean>
      
  ```    
      
### Memcached:
  
  > * a.
      
  > * b.    
  
#### 使用示例:

  ```xml
  
     <bean id="" class=""/>
     
  ```