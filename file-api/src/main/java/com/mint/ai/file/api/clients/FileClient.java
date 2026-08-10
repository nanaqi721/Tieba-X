package com.mint.ai.file.api.clients;

import com.mint.ai.common.Result;
import com.mint.ai.file.api.vo.UploadVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文件上传模块的 feign
 */
@FeignClient(name = "file-server", path = "/api/files")
public interface FileClient {

    /**
     * 上传单张图片，返回访问 URL
     */
    @PostMapping("/v1/upload")
    Result<UploadVO> upload(@RequestPart("file") MultipartFile file);

    /**
     * 批量上传图片（一次最多 5 张），返回 URL 列表，顺序与入参一致
     */
    @PostMapping("/v1/uploads")
    Result<List<UploadVO>> uploads(@RequestPart("files") List<MultipartFile> files);
}
