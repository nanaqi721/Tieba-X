package com.mint.ai.common.coontext;

/**
 * 用户上下文
 */
public class UserContext {

    private static final ThreadLocal<String> USER_ID = new ThreadLocal<>();

    public static void setUserId(String id){
        USER_ID.set(id);
    }

    public static String getUserId(){
        return USER_ID.get();
    }

    public static void removeUserId(){
        USER_ID.remove();
    }
}
