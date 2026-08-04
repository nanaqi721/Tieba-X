package com.mint.ai.utils;

import com.mint.ai.common.Result;
import com.mint.ai.common.enums.BaseEnums;
import com.mint.ai.common.enums.IErrorCode;

/**
 * 返回实体工具类
 */
public class Results {

    public static Result<Void> success(){
        return new Result<Void>()
                .setCode(Result.SUCCESS_CODE);
    }

    public static <T> Result<T>  success(T data){

        return new Result<T>()
                .setCode(Result.SUCCESS_CODE)
                .setData(data);
    }

    public static Result<Void> error(){
        return error(BaseEnums.SYSTEM_ERROR);
    }

    public static Result<Void> error(IErrorCode errorCode){
        return new Result<Void>()
                .setCode(errorCode.getCode())
                .setMessage(errorCode.getMessage());
    }

    public static Result<Void> error(String errorCode,String message){
        return new Result<Void>()
                .setCode(errorCode)
                .setMessage(message);
    }
}
