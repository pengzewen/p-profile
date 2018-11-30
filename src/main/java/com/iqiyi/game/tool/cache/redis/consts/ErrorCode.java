package com.iqiyi.game.tool.cache.redis.consts;

/**
 * Created by pengzewen on 9/7/18.
 */
public enum ErrorCode {

    UNKNOWN(0, "未知异常"),
    CONNECTION_ERROR(1, "连接异常");

    private int id;
    private String desc;

    ErrorCode(int id, String desc) {
        this.id = id;
        this.desc = desc;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}
