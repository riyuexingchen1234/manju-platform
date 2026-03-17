package com.manju.platform.common;

import lombok.Data;

@Data
public class Result {
    private int code; // 200成功， 500失败
    private String msg;
    private Object data;

    // 成功方法（无数据）
    public static Result success(String msg) {
        return new Result(200, msg, null);
    }

    // 成功方法（带数据）
    public static Result success(String msg, Object data) {
        return new Result(200, msg, data);
    }

    // 失败方法
    public static Result fail(String msg) {
        return new Result(500, msg, null);
    }

    private Result(int code, String msg, Object data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

}
