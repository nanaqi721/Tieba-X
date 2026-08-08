package com.mint.ai.controller;

import com.mint.ai.common.Result;
import com.mint.ai.common.vo.UploadVO;
import com.mint.ai.service.FileService;
import com.mint.ai.utils.Results;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文件上传控制层
 */
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    /**
     * 上传图片，返回访问 URL
     */
    @PostMapping("/v1/upload")
    public Result<UploadVO> upload(@RequestParam("file") MultipartFile file) {
        return Results.success(fileService.upload(file));
    }

    /**
     * 批量上传图片（一次最多 5 张），返回 URL 列表，顺序与入参一致
     */
    @PostMapping("/v1/uploads")
    public Result<List<UploadVO>> uploads(@RequestParam("files") List<MultipartFile> files){
        return Results.success(fileService.uploads(files));
    }
}
