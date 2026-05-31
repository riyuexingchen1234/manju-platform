package com.manju.platform.exception;

/**
 * 专门用来抛出「业务逻辑错误」的自定义异常类
 * 为了区分业务异常和系统异常，让全局异常处理器可以分别处理！
 * 系统错误（空指针、数据库连不上）用 Java 自带的异常
 * 业务错误（积分不够、用户未登录）用这个BusinessException
 * */
public class BusinessException extends RuntimeException{
    public BusinessException(String message){
        super(message);         // 把错误信息传给父类RuntimeException
    }
}
