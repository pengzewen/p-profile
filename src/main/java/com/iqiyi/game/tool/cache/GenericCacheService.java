package com.iqiyi.game.tool.cache;

import com.iqiyi.game.tool.cache.redis.service.RedisService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

/**
 * Created by pengzewen on 9/11/18.
 */
public class GenericCacheService<K, V> {

    private static Log logger = LogFactory.getLog(GenericCacheService.class);

    private String prefix;

    private int timeout;

    @Autowired
    private RedisService redisService;

    /**
     * 查询单个key的value
     * @param key
     * @return
     */
    public V get(K key) {
        // cache中获取值
        V val = getCache(key);
        if (val != null) {
            return val;
        }
        // 从源头获取值
        val = getObj(key);
        if (val == null) {
            val = createObj(key);
        }
        if (val != null) {
            update(key, val);
        }
        return val;
    }

    /**
     * 更新单个键值对
     * @param key
     * @param val
     */
    public void update(K key, V val) {
        try {
            if (redisService != null) {
                redisService.set(prefix + key, val, timeout);
            }
        } catch (Exception e) {
            logger.error("redis update error: " + e.getMessage(), e);
        }
    }

    /**
     * 批量获取值
     * @param keys
     * @return
     */
    public Map<K, V> batchGet(List<K> keys) {
        if (keys == null || keys.size() <= 0) {
            return null;
        }
        // 从缓存中获取值
        Map<K, V> valsMap = batchCacheGet(keys);
        if (valsMap != null && valsMap.size() == keys.size()) {
            return valsMap;
        }

        // 从源头获取值
        Map<K, V> missedMap = batchGetObj(getDiffKeys(new HashSet<K>(keys), valsMap.keySet()));
        if (missedMap != null) {
            valsMap.putAll(missedMap);
            batchUpdate(missedMap);
        }

        if (valsMap.size() == keys.size()) {
            return valsMap;
        }

        // 创建在源头也查询不到的值
        batchUpdate(batchCreateObj(getDiffKeys(new HashSet<K>(keys), valsMap.keySet())));

        return valsMap;
    }

    /**
     * 批量更新键值对
     * @param map
     */
    public void batchUpdate(Map<K, V> map) {
        if (redisService != null && map != null && map.size() > 0) {
            Map<String, V> kvMap = new HashMap<String, V>();
            for (Map.Entry<K, V> entry : map.entrySet()) {
                kvMap.put(prefix + entry.getKey(), entry.getValue());
            }
            redisService.batchSet(kvMap, timeout);
        }
    }

    private V getCache(K key) {
        try {
            if (redisService != null && key != null) {
                return redisService.get(prefix + key, Boolean.FALSE);
            }
        } catch (Exception e) {
            logger.error("Get cache value error: " + e.getMessage(), e);
        }
        return null;
    }

    private Map<K, V> batchCacheGet(List<K> keys) {
        if (redisService != null && keys != null && keys.size() > 0) {
            Map<K, V> retMap = null;
            List<String> strKeys = new ArrayList<String>();
            for (K key : keys) {
                strKeys.add(prefix + key);
            }
            Map<String, V> map = redisService.batchGet(strKeys, Boolean.FALSE);
            if (map != null) {
                retMap = new HashMap<K, V>();
                for (K key : keys) {
                    if (map.containsKey(prefix + key)) {
                        retMap.put(key, map.get(prefix + key));
                    }
                }
            }
            return retMap;
        }
        return new HashMap<K, V>();
    }

    private List<K> getDiffKeys(Set<K> keysSet, Set<K> minorSet) {
        if (keysSet != null) {
            keysSet.removeAll(minorSet);
            return new ArrayList<K>(keysSet);
        }
        return new ArrayList<K>();
    }

    /**
     * 继承者重写该方法，cache中批量查询不到时，从源头获取数据
     * @param keys
     * @return
     */
    protected Map<K, V> batchGetObj(List<K> keys) {
        return null;
    }

    /**
     * 继承者重写该方法，cache中批量查询不到时，重写该数据
     * @param keys
     * @return
     */
    protected Map<K, V> batchCreateObj(List<K> keys) {
        return null;
    }

    /**
     * 继承者重写该方法，cache中查询不到时，从源头获取数据
     * @param key
     * @return
     */
    protected V getObj(K key) {
        return null;
    }

    /**
     * 继承者重写该方法，查询不到数据时，重写该数据
     * @param key
     * @return
     */
    protected V createObj(K key) {
        return null;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public RedisService getRedisService() {
        return redisService;
    }

    public void setRedisService(RedisService redisService) {
        this.redisService = redisService;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
}
