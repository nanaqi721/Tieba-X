package com.mint.ai.service;

import com.mint.ai.common.vo.UploadVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文件上传服务
 */
public interface FileService {

    /**
     * 上传图片到 OSS，返回访问 URL
     */
    UploadVO upload(MultipartFile file);

    List<UploadVO> uploads(List<MultipartFile> files);
}
