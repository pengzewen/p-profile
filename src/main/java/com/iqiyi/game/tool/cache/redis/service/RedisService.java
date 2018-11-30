package com.iqiyi.game.tool.cache.redis.service;

import com.iqiyi.game.tool.cache.redis.client.RedisClient;
import com.iqiyi.game.tool.cache.redis.consts.Ttl;
import com.iqiyi.game.tool.cache.redis.exception.RedisException;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Created by pengzewen on 9/6/18.
 */
public class RedisService {

    private static final Logger logger = LoggerFactory.getLogger(RedisService.class);

    private RedisClient masterClient;

    private RedisClient slaveClient;

    private static boolean useRandom = true;

    public RedisService(RedisClient master, RedisClient slave) {
        this.masterClient = master;
        this.slaveClient = slave;
    }

    public RedisService(RedisClient master) {
        this.masterClient = master;
    }

    /**
     * 获取当前redis情况
     * @return
     */
    public String getRedisInfo() {
        return masterClient.getRedisInfo()
                + "#" + slaveClient.getRedisInfo();
    }

    /**
     * 往游戏中心redis填充数据，过期固定timeout
     */
    public boolean set(String key, Object val, int timeout) {
        try {
            if (null != val) {
                masterClient.set(key.getBytes(), masterClient.serialize(val), timeout);
            }
            return true;
        } catch (RedisException e) {
            logger.error(e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("Error set redis : key = " + key + " val = " + val + " timeout = " + timeout, e);
        }
        return false;
    }
    /**
     * 获取游戏中心redis中的数据
     */
    public <T> T get(String key, boolean useMaster) {
        if (StringUtils.isBlank(key)) {
            return null;
        }
        RedisClient local = useMaster ? masterClient : slaveClient;
        try {
            byte[] in = local.get(key.getBytes());
            Object val = local.deserialize(in);
            if (val == null) {
                logger.debug("Redis miss: key: {}", key);
                return null;
            }
            return (T) val;
        } catch (RedisException e) {
            logger.error(e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("exception when get from redis : key " + key, e);
        }
        logger.debug("Redis miss: key: {}", key);
        return null;
    }

    /**
     * 批量往游戏中心redis填充数据，过期固定timeout时间
     */
    public <V> boolean batchSet(Map<String, V> map, int timeout) {
        try {
            if (MapUtils.isNotEmpty(map)) {
                masterClient.batchSet(map, timeout);
            }
            return true;
        } catch (RedisException e) {
            logger.error("redis set error" + e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("Error batch set redis: timeout = " + timeout, e);
        }
        return false;
    }

    /**
     * 批量查询同一类型的
     * 数据会少哦
     */
    public <V> Map<String, V> batchGet(Collection<String> keys, boolean useMaster) {
        Map<String, V> result = new HashMap<String, V>();
        if (keys == null || keys.isEmpty()) {
            return result;
        }
        RedisClient local = useMaster ? masterClient : slaveClient;
        try {
            HashMap<String, byte[]> rst = local.batchGet(keys);
            Object val = null;
            for (Map.Entry<String, byte[]> entry :rst.entrySet()) {
                val = local.deserialize(entry.getValue());
                if (val == null) {
                    continue;
                }
                result.put(entry.getKey(), (V)val);
            }
            return result;
        } catch (RedisException e) {
            logger.error(e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("exception when get from redis : keys " + keys.toString(), e);
        }
        return result;
    }
    /**
     * 往游戏中心redis填充数据，过期在timeout让前推移随机数
     */
    public boolean setRandom(String key, Object val, int timeout) {
        try {
            if (null != val) {
                if (useRandom) {
                    timeout = getRandomTimeout(timeout);
                }
                masterClient.set(key.getBytes(), masterClient.serialize(val), timeout);
            }
            return true;
        } catch (RedisException e) {
            logger.error(e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("Error set redis : key = " + key + " val = " + val + " timeout = " + timeout, e);
        }
        return false;
    }
    /**
     * 往游戏中心redis获取hash中的数据
     */
    public Map<String, String> hgetAll(String key, boolean useMaster) {
        return useMaster ? masterClient.hgetAll(key) : slaveClient.hgetAll(key);
    }
    /**
     * 返回给定key的剩余生存时间(time to live)(以秒为单位)
     */
    public Long ttl(String key) {
        if (StringUtils.isBlank(key)) {
            return 0L;
        }
        return slaveClient.ttl(key);
    }
    /**
     * 删除游戏中心redis中的指定key
     */
    public boolean delete(String key) {
        if (StringUtils.isBlank(key)) {
            return true;
        }
        try {
            masterClient.del(key);
            return true;
        } catch (RedisException e) {
            logger.error(e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("Error delete Redis : key = " + key, e);
        }
        return false;
    }
    /**
     * 判断KEY保存的值是什么类型的，hash还是正常的
     */
    public String type(String key) {
        return masterClient.type(key);
    }
    //--------------------------以下为红点redis方法-----------------------------

    //--------------------------以下为redis公共方法-----------------------------
    /**
     * 获取redis随机时间
     * 一个月随机一个小时，一周随机一个小时，一天随机30分钟，1小时随机5分钟，30分钟随机1分钟，5分钟随机30秒，5分钟内固定
     */
    public int getRandomTimeout(int timeout) {
        int randomTime = 0;
        if (timeout >= Ttl.TTL_1_MONTH) {
            randomTime = Ttl.TTL_1_HOUR;
        } else if (timeout >= Ttl.TTL_1_WEEK) {
            randomTime = Ttl.TTL_1_HOUR;
        } else if (timeout >= Ttl.TTL_1_DAY) {
            randomTime =Ttl.TTL_30_MIN;
        } else if (timeout >= Ttl.TTL_1_HOUR) {
            randomTime = Ttl.TTL_5_MIN;
        } else if (timeout >= Ttl.TTL_30_MIN) {
            randomTime = Ttl.TTL_1_MIN;
        } else if (timeout >= Ttl.TTL_5_MIN) {
            randomTime = Ttl.TTL_30_SEC;
        }
        timeout = timeout - new Random(randomTime).nextInt(randomTime);
        return timeout;
    }

    public RedisClient getMaster() {
        return masterClient;
    }

    public RedisClient getSlave(boolean useMaster) {

        return useMaster ? getMaster() : (slaveClient != null ? slaveClient : getMaster());
    }

}
