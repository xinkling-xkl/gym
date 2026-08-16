package org.example.plan.common;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 * 统一处理 Sentinel 限流异常、越权异常、业务异常
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Sentinel 限流/熔断异常 */
    @ExceptionHandler(BlockException.class)
    public Result<Void> handleBlockException(BlockException e) {
        return Result.error(429, "请求过于频繁，请稍后再试");
    }

    /** 越权操作异常 */
    @ExceptionHandler(SecurityException.class)
    public Result<Void> handleSecurityException(SecurityException e) {
        return Result.error(403, e.getMessage());
    }

    /** 参数校验异常 */
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleIllegalArgument(IllegalArgumentException e) {
        return Result.error(400, e.getMessage());
    }

    /** 其他未捕获异常 */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        return Result.error(500, "服务器内部错误：" + e.getMessage());
    }
}
