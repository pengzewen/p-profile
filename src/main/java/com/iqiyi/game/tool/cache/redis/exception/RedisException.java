package com.iqiyi.game.tool.cache.redis.exception;

/**
 * Created by pengzewen on 9/7/18.
 */
public class RedisException extends RuntimeException {
    private static final long serialVersionUID = -1267826311634066132L;
    Integer code;
    public RedisException(Integer code, String message, Throwable e) {
        super(message, e);
        this.code = code;
    }
    public Integer getCode() {
        return code;
    }
    public void setCode(Integer code) {
        this.code = code;
    }
}
