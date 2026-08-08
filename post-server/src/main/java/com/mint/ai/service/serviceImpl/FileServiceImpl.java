package com.mint.ai.service.serviceImpl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.aliyun.oss.OSS;
import com.mint.ai.common.exception.ClientException;
import com.mint.ai.common.exception.ServiceException;
import com.mint.ai.common.vo.UploadVO;
import com.mint.ai.config.OssProperties;
import com.mint.ai.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;

/**
 * 文件上传服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "gif", "webp");

    private static final long MAX_SIZE = 5L * 1024 * 1024;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final OSS ossClient;

    private final OssProperties ossProperties;

    @Override
    public UploadVO upload(MultipartFile file) {
        // 1. 校验
        if (file == null || file.isEmpty()) {
            throw new ClientException("上传文件不能为空");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new ClientException("图片大小不能超过5MB");
        }
        String ext = extractExt(file.getOriginalFilename());
        if (!ALLOWED_EXT.contains(ext)) {
            throw new ClientException("仅支持 jpg/jpeg/png/gif/webp 格式");
        }

        // 2. 生成 OSS key：comments/yyyyMMdd/uuid.ext
        // 评论/日期/图片名
        String key = "comments/" + LocalDate.now().format(DATE_FMT) + "/" + IdUtil.fastSimpleUUID() + "." + ext;

        // 3. 上传
        try (InputStream in = file.getInputStream()) {
            // 桶名 文件名
            ossClient.putObject(ossProperties.getBucket(), key, in);
        } catch (IOException e) {
            log.error("上传图片到OSS失败, key={}", key, e);
            throw new ServiceException("图片上传失败", e);
        }

        // 4. 拼公共读 URL：https://{bucket}.{endpoint}/{key}
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
