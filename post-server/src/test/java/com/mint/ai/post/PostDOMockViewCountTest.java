package com.mint.ai.post;

import com.mint.ai.mock.factory.PostMockFactory;
import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
/*  @MockitoBean 会用 Mockito mock 替换 XxlJobSpringExecutor bean，
    真实 bean 的 afterPropertiesSet（也就是 bind 9999 那步）不会执行。
    这样应用在跑着也不影响测试，一劳永逸。将来写刷库任务测试类时同样加这个注解。
 */
public class PostDOMockViewCountTest {

    @Autowired
    private PostMockFactory postMockFactory;

    @MockitoBean
    private XxlJobSpringExecutor xxlJobSpringExecutor;


    @Test
    public void mockPostCount(){
        postMockFactory.mockFavoriteCount("2084980578478198785",10);
    }
}
