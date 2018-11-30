package com.iqiyi.game.tool.cache.redis.client;

import com.iqiyi.game.tool.cache.redis.consts.ErrorCode;
import com.iqiyi.game.tool.cache.redis.exception.RedisException;
import net.spy.memcached.compat.CloseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Response;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPipeline;
import redis.clients.jedis.ShardedJedisPool;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.io.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by pengzewen on 9/6/18.
 */
public class RedisClient {

    private static final Logger log = LoggerFactory.getLogger(RedisClient.class);

    private String name;

    private ShardedJedisPool redisDataSource;

    RedisClient(String name, ShardedJedisPool redisDataSource) {
        this.name = name;
        this.redisDataSource = redisDataSource;
    }

    /**
     * 获取redis信息
     */
    public String getRedisInfo() {
        return redisDataSource.getNumActive()
                + "|" + redisDataSource.getNumIdle()
                + "|" + redisDataSource.getNumWaiters();
    }
    /**
     * 回收资源
     */
    public void returnResource(ShardedJedis shardedJedis, boolean broken) {
        if (broken) {
            redisDataSource.returnBrokenResource(shardedJedis);
        } else {
            redisDataSource.returnResource(shardedJedis);
        }
    }
    /**
     * 填充单个值
     */
    public void set(byte[] key, byte[] value, int timeOut) {
        ShardedJedis shardedJedis = null;
        boolean broken = false;
        try {
            shardedJedis = redisDataSource.getResource();
            shardedJedis.setex(key, timeOut, value);
        } catch (JedisConnectionException e) {
            broken = true;
            throw new RedisException(ErrorCode.CONNECTION_ERROR.getId(), "RedisClient [" + name + "] " + ErrorCode.CONNECTION_ERROR.getDesc(), e);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            broken = true;
        } finally {
            this.returnResource(shardedJedis, broken);
        }
    }
    /**
     * 批量塞值
     */
    public <V> void batchSet(Map<String, V> data, int timeOut) {
        ShardedJedis shardedJedis = null;
        boolean broken = false;
        try {
            shardedJedis = redisDataSource.getResource();
            ShardedJedisPipeline pipeline = shardedJedis.pipelined();
            for (Map.Entry<String, V> entry : data.entrySet()) {
                byte[] key = entry.getKey().getBytes();
                byte[] value = serialize(entry.getValue());
                pipeline.setex(key, timeOut, value);
            }
            pipeline.sync();
        } catch (JedisConnectionException e) {
            broken = true;
            throw new RedisException(ErrorCode.CONNECTION_ERROR.getId(), "RedisClient [" + name + "] " + ErrorCode.CONNECTION_ERROR.getDesc(), e);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            broken = true;
        } finally {
            this.returnResource(shardedJedis, broken);
        }
    }
    /**
     * 获取key保存值类型
     */
    public String type(String key) {
        String result = null;
        ShardedJedis shardedJedis = null;
        boolean broken = false;
        try {
            shardedJedis = redisDataSource.getResource();
            result = shardedJedis.type(key);
        } catch (JedisConnectionException e) {
            broken = true;
            throw new RedisException(ErrorCode.CONNECTION_ERROR.getId(), "RedisClient [" + name + "] " + ErrorCode.CONNECTION_ERROR.getDesc(), e);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            broken = true;
        } finally {
            this.returnResource(shardedJedis, broken);
        }
        return result;
    }

    /**
     * 返回当前KEY的过期时间
     */
    public Long ttl(String key) {
        Long result = null;
        ShardedJedis shardedJedis = null;
        boolean broken = false;
        try {
            shardedJedis = redisDataSource.getResource();
            result = shardedJedis.ttl(key);
        } catch (JedisConnectionException e) {
            broken = true;
            throw new RedisException(ErrorCode.CONNECTION_ERROR.getId(), "RedisClient [" + name + "] " + ErrorCode.CONNECTION_ERROR.getDesc(), e);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            broken = true;
        } finally {
            this.returnResource(shardedJedis, broken);
        }
        return result;
    }

    /**
     * hash填充
     */
    public void hset(String key, String field, String value) {
        ShardedJedis shardedJedis = null;
        boolean broken = false;
        try {
            shardedJedis = redisDataSource.getResource();
            shardedJedis.hset(key, field, value);
        } catch (JedisConnectionException e) {
            broken = true;
            throw new RedisException(ErrorCode.CONNECTION_ERROR.getId(), "RedisClient [" + name + "] " + ErrorCode.CONNECTION_ERROR.getDesc(), e);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            broken = true;
        } finally {
            this.returnResource(shardedJedis, broken);
        }
    }

    public Long del(String key) {
        Long result = null;
        ShardedJedis shardedJedis = null;
        boolean broken = false;
        try {
            shardedJedis = redisDataSource.getResource();
            result = shardedJedis.del(key);
        } catch (JedisConnectionException e) {
            broken = true;
            throw new RedisException(ErrorCode.CONNECTION_ERROR.getId(), "RedisClient [" + name + "] " + ErrorCode.CONNECTION_ERROR.getDesc(), e);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            broken = true;
        } finally {
            this.returnResource(shardedJedis, broken);
        }
        return result;
    }
    /**
     * 序列号
     */
    public static byte[] serialize(Object value) {
        if (value == null) {
            throw new NullPointerException("Can't serialize null");
        }
        byte[] rv = null;
        ByteArrayOutputStream bos = null;
        ObjectOutputStream os = null;
        try {
            bos = new ByteArrayOutputStream();
            os = new ObjectOutputStream(bos);
            os.writeObject(value);
            os.close();
            bos.close();
            rv = bos.toByteArray();
        } catch (IOException e) {
            throw new IllegalArgumentException("Non-serializable object", e);
        } finally {
            CloseUtil.close(os);
            CloseUtil.close(bos);
        }
        return rv;
    }

    /**
     * 获取单个值
     */
    public byte[] get(byte[] key) {
        byte[] result = null;
        ShardedJedis shardedJedis = null;
        boolean broken = false;
        try {
            shardedJedis = redisDataSource.getResource();
            result = shardedJedis.get(key);
        } catch (JedisConnectionException e) {
            broken = true;
            throw new RedisException(ErrorCode.CONNECTION_ERROR.getId(), "RedisClient [" + name + "] " + ErrorCode.CONNECTION_ERROR.getDesc(), e);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            broken = true;
        } finally {
            this.returnResource(shardedJedis, broken);
        }
        return result;
    }

    /**
     * 批量获取某个key对应的值
     */
    public HashMap<String, byte[]> batchGet(Collection<String> keys) {
        HashMap<String, byte[]> result = null;
        ShardedJedis shardedJedis = null;
        boolean broken = false;
        try {
            shardedJedis = redisDataSource.getResource();
            ShardedJedisPipeline pipeline = shardedJedis.pipelined();
            HashMap<String, Response<byte[]>> newMap = new HashMap<String, Response<byte[]>>();
            for (String key : keys) {
                newMap.put(key, pipeline.get(key.getBytes()));
            }
            pipeline.sync();
            result = new HashMap<String, byte[]>();
            Response<byte[]> sResponse = null;
            for (Map.Entry<String, Response<byte[]>> entry :newMap.entrySet()) {
                sResponse = entry.getValue();
                if (sResponse != null){
                    result.put(entry.getKey(), sResponse.get());
                }
            }
        } catch (JedisConnectionException e) {
            broken = true;
            throw new RedisException(ErrorCode.CONNECTION_ERROR.getId(), "RedisClient [" + name + "] " + ErrorCode.CONNECTION_ERROR.getDesc(), e);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            broken = true;
        } finally {
            this.returnResource(shardedJedis, broken);
        }
        return result;
    }
    /**
     * 获取hash数据
     */
    public Map<String, String> hgetAll(String key) {
        Map<String, String> result = null;
        ShardedJedis shardedJedis = null;
        boolean broken = false;
        try {
            shardedJedis = redisDataSource.getResource();
            result = shardedJedis.hgetAll(key);
        } catch (JedisConnectionException e) {
            broken = true;
            throw new RedisException(ErrorCode.CONNECTION_ERROR.getId(), "RedisClient [" + name + "] " + ErrorCode.CONNECTION_ERROR.getDesc(), e);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            broken = true;
        } finally {
            this.returnResource(shardedJedis, broken);
        }
        return result;
    }
    /**
     * 反序列化
     */
    public static Object deserialize(byte[] in) {
        Object rv = null;
        ByteArrayInputStream bis = null;
        ObjectInputStream is = null;
        try {
            if (in != null) {
                bis = new ByteArrayInputStream(in);
                is = new ObjectInputStream(bis);
                rv = is.readObject();
                is.close();
                bis.close();
            }
        } catch (IOException e) {
            log.warn("Caught IOException decoding %d bytes of data", in == null ? 0 : in.length, e);
        } catch (ClassNotFoundException e) {
            log.warn("Caught CNFE decoding %d bytes of data", in == null ? 0 : in.length, e);
        } finally {
            CloseUtil.close(is);
            CloseUtil.close(bis);
        }
        return rv;
    }
}
