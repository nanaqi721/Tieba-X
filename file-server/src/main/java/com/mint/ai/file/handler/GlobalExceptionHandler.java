package com.mint.ai.file.handler;

import com.mint.ai.common.Result;
import com.mint.ai.common.enums.BaseEnums;
import com.mint.ai.common.exception.BizException;
import com.mint.ai.utils.Results;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 全局业务异常处理器
     * @param request http请求
     * @param ex 拦截错误
     * @return 错误异常统一返回实体
     */
    @ExceptionHandler(value = BizException.class)
    public Result<Void> bizExceptionHandler(HttpServletRequest request,BizException ex){
        if(ex.getCause() == null){
            log.warn("[{}]{}[ex]{}",request.getMethod(),request.getRequestURL().toString(),ex.getMessage());
        }else {
            // 出现了异常链说明底层真的有问题，不是直接抛出的异常
            String errorMessagesChain = builderErrorMessagesChain(ex);
            log.error(errorMessagesChain);
        }
        return Results.error(ex.getCode(),ex.getMessage());
    }

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public Result<Void> methodArgumentNoValidExceptionHandler(HttpServletRequest request,MethodArgumentNotValidException ex){
        StringBuilder errorMessage = new StringBuilder();
        ex.getBindingResult().getAllErrors().stream()
                .forEach(e -> {
                    errorMessage.append(((FieldError)e).getField() + ":");
                    errorMessage.append(e.getDefaultMessage() + ";");
                });
        log.error("参数校验失败，路径：{}，错误信息{}",request.getMethod() + request.getRequestURI().toString(),errorMessage.toString());
        return Results.error(BaseEnums.USER_ERROR.getCode(),errorMessage.toString());
    }

    /**
     * 拦截未匹配的路径
     * @param ex 拦截的异常
     * @return 错误异常统一返回实体
     */
    @ExceptionHandler(value = NoResourceFoundException.class)
    public Result<Void> noResourceFoundExceptionHandler(NoResourceFoundException ex){
        log.warn("资源未找到：{}",ex.getResourcePath());
        return Results.error(BaseEnums.USER_ERROR.getCode(),"请求资源不存在");
    }

    // 兜底：日志打全，返回藏住
    @ExceptionHandler(value = Exception.class)
    public Result<Void> exceptionHandler(Exception ex) {
        log.error("系统异常", ex);
        return Results.error(BaseEnums.SYSTEM_ERROR.getCode(), BaseEnums.SYSTEM_ERROR.getMessage());
    }


    private String builderErrorMessagesChain(Throwable e){
        StringBuilder s = new StringBuilder();
        Throwable current = e;
        while(current != null){
            s.append(current.getClass().getSimpleName())
                    .append(":")
                    .append(current.getMessage());
            if(current != null){
                s.append("->");
            }
            current = current.getCause();
        }
        return s.toString();
    }
}
