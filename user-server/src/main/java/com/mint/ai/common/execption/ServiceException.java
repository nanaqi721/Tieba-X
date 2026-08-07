package com.mint.ai.common.execption;

import com.mint.ai.common.enums.BaseEnums;
import com.mint.ai.common.enums.IErrorCode;
import com.mint.ai.common.exception.BizException;

/**
 * 客户端异常
 */
public class ServiceException extends BizException {
    public ServiceException(String message){
        super(message,null, BaseEnums.SYSTEM_ERROR);
    }

    public ServiceException(String message, Throwable throwable){
        super(message,throwable,BaseEnums.SYSTEM_ERROR);
    }
    public ServiceException(String message, Throwable throwable, IErrorCode errorCode) {
        super(message, throwable, errorCode);
    }
    @Override
    public String toString(){
        return "ServiceException{" +
                "code:" + getCode() + "," +
                "messages:" + getMessage() +
                "}" ;
    }
}
