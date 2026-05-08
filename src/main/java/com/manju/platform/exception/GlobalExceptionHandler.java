package com.manju.platform.exception;

import com.manju.platform.common.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器 GlobalExceptionHandler
 * 统一处理 BusinessException，将异常消息返回给前端。
 * 其他未预期异常返回通用错误。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public Result handleBusinessException(BusinessException e) {
        return Result.fail(e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result handleException(Exception e) {
        logger.error("系统异常", e);
        if (e.getMessage() != null && e.getMessage().contains("超时")) {
            return Result.fail("AI服务响应超时，请稍后重试");
        }
        return Result.fail("系统错误，请稍后重试");
    }
}
