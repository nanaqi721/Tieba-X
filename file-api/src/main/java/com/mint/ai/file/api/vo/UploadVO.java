package com.mint.ai.file.api.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 上传接口返回
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadVO {

    private String url;
}
