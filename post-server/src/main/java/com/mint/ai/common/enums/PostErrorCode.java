package com.mint.ai.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 帖子模块错误码（业务模块内定义，实现 IErrorCode 接口）
 * <p>
 * 错误码格式：来源(A/B) + 模块号(02=帖子) + 具体错误编号
 *   用户端：A02xx
 *   系统端：B02xx
 */
@Getter
@AllArgsConstructor
public enum PostErrorCode implements IErrorCode {

    // ---- 用户端 A02xx ----
    POST_NOT_FOUND("A0201", "帖子不存在"),
    POST_DELETED("A0202", "帖子已删除"),
    POST_TITLE_EMPTY("A0203", "帖子标题不能为空"),
    POST_TITLE_LENGTH_INVALID("A0204", "帖子标题长度需在5-30字之间"),
    POST_CONTENT_EMPTY("A0205", "帖子内容不能为空"),
    POST_LOCKED("A0206", "帖子已锁定"),
    POST_NO_PERMISSION("A0207", "无权限操作该帖子"),
    FILE_NOT_NULL("A0251","上传文件不能为空"),
    FILE_EXCEED_MAX_SIZE("A0252","上传文件超出大小"),
    FILE_NOT_ALLOW_TYPE("A0253","上传文件格式不正确"),
    FILE_EXCEED_MAX_NUM("A0254","超出文件上传的数量"),

    // ---- 系统端 B02xx ----
    POST_SAVE_ERROR("B0201", "帖子保存失败"),
    POST_UPDATE_ERROR("B0202", "帖子更新失败"),
    POST_DELETE_ERROR("B0203", "帖子删除失败"),
    POST_QUERY_ERROR("B0204", "帖子查询失败");

    private final String code;

    private final String message;
}
