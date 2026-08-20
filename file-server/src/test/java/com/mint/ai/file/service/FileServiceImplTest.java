package com.mint.ai.file.service;

import com.aliyun.oss.OSS;
import com.mint.ai.file.config.OssProperties;
import com.mint.ai.file.exception.ClientException;
import com.mint.ai.file.service.impl.FileServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ThreadPoolExecutor;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class FileServiceImplTest {

    @Mock
    private OSS ossClient;

    @Mock
    private ThreadPoolExecutor uploadExecutor;

    @Test
    void upload_whenJpegExtensionContainsNonImageData_shouldReject() {
        FileService service = new FileServiceImpl(ossClient, new OssProperties(), uploadExecutor);
        MockMultipartFile disguisedFile = new MockMultipartFile(
                "file",
                "disguised.jpg",
                "image/jpeg",
                "this is not an image".getBytes(StandardCharsets.UTF_8)
        );

        assertThatThrownBy(() -> service.upload(disguisedFile))
                .isInstanceOf(ClientException.class);
        verifyNoInteractions(ossClient);
    }
}
