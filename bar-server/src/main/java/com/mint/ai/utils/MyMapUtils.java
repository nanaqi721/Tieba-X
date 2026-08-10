package com.mint.ai.utils;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * map类型工具类
 */
public class MyMapUtils {
    /**
     * 将map<object,object>转换为map<string,string>
     * @return Map<String,String>
     */
    public static Map<String,String> mapToStingMap(Map<Object,Object> originalMap){

        return originalMap.entrySet().stream()
                .collect(Collectors.toMap(k -> String.valueOf(k.getKey()), v -> String.valueOf(v.getValue())));
    }
}
