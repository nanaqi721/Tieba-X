package com.mint.ai.util;

import cn.hutool.http.useragent.Platform;
import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 终端类型识别工具
 * <p>
 * 根据请求头 User-Agent 判断当前终端类型，用于 Sa-Token 多终端登录（同端互斥）。
 */
public class DeviceTypeUtil {

    public static final String PC = "pc";
    public static final String MOBILE = "mobile";
    public static final String TABLET = "tablet";

    private DeviceTypeUtil() {
    }

    /**
     * 识别终端类型
     * <p>
     * 局限：Android 平板会被归为 mobile；iPadOS 13+ 默认发送桌面 UA，可能识别为 pc。
     * @param request http 请求
     * @return pc / mobile / tablet
     */
    public static String detect(HttpServletRequest request) {
        UserAgent userAgent = UserAgentUtil.parse(request.getHeader("User-Agent"));
        if (userAgent == null) {
            return PC;
        }
        Platform platform = userAgent.getPlatform();
        if (platform.isIPad()) {
            return TABLET;
        }
        if (platform.isMobile()) {
            return MOBILE;
        }
        return PC;
    }
}
