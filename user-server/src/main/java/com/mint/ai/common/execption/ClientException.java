package com.mint.ai.common.execption;

import com.mint.ai.common.enums.BaseEnums;
import com.mint.ai.common.enums.IErrorCode;
import com.mint.ai.common.exception.BizException;

public class ClientException extends BizException {

    public ClientException(String message){
        super(message,null, BaseEnums.USER_ERROR);
    }

    public ClientException(String message,Throwable throwable){
        super(message,throwable,BaseEnums.USER_ERROR);
    }
    public ClientException(String message, Throwable throwable, IErrorCode errorCode) {
        super(message, throwable, errorCode);
    }
    @Override
    public String toString(){
        return "ClientException{" +
                "code:" + getCode() + "," +
                "messages:" + getMessage() +
                "}" ;
    }
}