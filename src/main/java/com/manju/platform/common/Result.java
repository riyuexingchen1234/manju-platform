package com.manju.platform.common;

import lombok.Data;
/**
 * 统一API响应结果封装类
 * 作用：前后端分离架构中，所有后端接口返回统一格式的数据，方便前端统一处理
 */
// Lombok注解：自动生成该类所有字段的getter、setter、toString、equals、hashCode方法
@Data
public class Result {
    // 响应状态码
    // 约定：200表示请求成功，500表示请求失败
    private int code;
    // 响应提示信息
    // 成功时返回"操作成功"等，失败时返回具体错误原因（如"积分不足"、"用户不存在"）
    private String msg;
    // 响应数据载体
    // 成功时存放需要返回给前端的数据（如用户信息、AI生成结果、列表数据等）
    private Object data;

    // 成功响应方法（无数据版本）
    // 用于不需要返回具体数据的操作（如"修改密码成功"、"删除成功"）
    public static Result success(String msg) {
        return new Result(200, msg, null);
    }

    // 成功响应方法（带数据版本）
    // 用于需要返回具体数据的操作（如"查询用户信息成功"、"AI生成成功"）
    public static Result success(String msg, Object data) {
        return new Result(200, msg, data);
    }

    // 失败响应方法
    // 用于所有请求失败的场景（如参数错误、积分不足、AI调用失败等）
    public static Result fail(String msg) {
        return new Result(500, msg, null);
    }

    // 私有构造方法，设计意图：禁止外部直接通过new关键字创建Result对象。
    // 必须通过上面的success()或fail()静态方法来创建对象，保证响应格式的统一性
    private Result(int code, String msg, Object data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }
}
