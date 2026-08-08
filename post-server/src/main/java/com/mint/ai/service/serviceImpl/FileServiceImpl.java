package com.mint.ai.service.serviceImpl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.aliyun.oss.OSS;
import com.mint.ai.common.enums.PostErrorCode;
import com.mint.ai.common.exception.ClientException;
import com.mint.ai.common.exception.ServiceException;
import com.mint.ai.common.vo.UploadVO;
import com.mint.ai.config.OssProperties;
import com.mint.ai.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 文件上传服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "gif", "webp");

    private static final long MAX_SIZE = 5L * 1024 * 1024;

    private static final int MAX_NUM = 5;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final OSS ossClient;

    private final OssProperties ossProperties;

    private final ThreadPoolExecutor ossUploadExecutor;

    @Override
    public UploadVO upload(MultipartFile file) {
        validate(file);
        return doUpload(file);
    }

    @Override
    public List<UploadVO> uploads(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new ClientException(PostErrorCode.FILE_NOT_NULL.getMessage(),null,PostErrorCode.FILE_NOT_NULL);
        }
        if (files.size() > MAX_NUM) {
            throw new ClientException(PostErrorCode.FILE_EXCEED_MAX_NUM.getMessage(),null,PostErrorCode.FILE_EXCEED_MAX_NUM);
        }
        for (MultipartFile file : files) {
            validate(file);
        }
        // 并行上传：5 张图并发 putObject，总耗时≈单张；join 按入参顺序收集
        List<CompletableFuture<UploadVO>> futures = files.stream()
                .map(file -> CompletableFuture.supplyAsync(() -> doUpload(file), ossUploadExecutor))
                .toList();
        List<UploadVO> uploadVOList = new ArrayList<>(files.size());
        for (CompletableFuture<UploadVO> future : futures) {
            try {
                uploadVOList.add(future.get());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ServiceException("图片上传被中断", e);
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof ServiceException) {
                    throw (ServiceException) cause;
                }
                throw new ServiceException("图片上传失败", cause);
            }
        }
        return uploadVOList;
    }

    /**
     * 校验单个上传文件：非空、大小 ≤5MB、扩展名白名单
     */
    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ClientException(PostErrorCode.FILE_NOT_NULL.getMessage(),null,PostErrorCode.FILE_NOT_NULL);
        }
        if (file.getSize() > MAX_SIZE) {
            throw new ClientException(PostErrorCode.FILE_EXCEED_MAX_SIZE.getMessage(),null,PostErrorCode.FILE_EXCEED_MAX_SIZE);
        }
        String ext = extractExt(file.getOriginalFilename());
        if (!ALLOWED_EXT.contains(ext)) {
            throw new ClientException(PostErrorCode.FILE_NOT_ALLOW_TYPE.getMessage(),null,PostErrorCode.FILE_NOT_ALLOW_TYPE);
        }
    }

    /**
     * 上传单个文件到 OSS
     */
    private UploadVO doUpload(MultipartFile file) {
        // 生成 OSS key：comments/yyyy-MM-dd/uuid.ext
        String ext = extractExt(file.getOriginalFilename());
        String key = "comments/" + LocalDate.now().format(DATE_FMT) + "/" + IdUtil.fastSimpleUUID() + "." + ext;

        // 上传
        try (InputStream in = file.getInputStream()) {
            // 桶名 文件名
            ossClient.putObject(ossProperties.getBucket(), key, in);
        } catch (IOException e) {
            log.error("上传图片到OSS失败, key={}", key, e);
            throw new ServiceException("图片上传失败", e);
        }

        // 拼公共读 URL：https://{bucket}.{endpoint}/{key}
        String url = "https://" + ossProperties.getBucket() + "." + ossProperties.getEndpoint() + "/" + key;
        return UploadVO.builder().url(url).build();
    }

    /**
     * 解析文件后缀名
     * @param originalFilename 原始文件名
     * @return 后缀名称
     */
    private String extractExt(String originalFilename) {
        if (StrUtil.isBlank(originalFilename) || !originalFilename.contains(".")) {
            return "";
        }
        return originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
    }
}
