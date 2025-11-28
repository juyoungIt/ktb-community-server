package com.ktb.howard.ktb_community_server;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import javax.sql.DataSource;

@SpringBootTest
// TODO : CI 환경에서 우선 테스트 통과를 받기 위해 작성한 코드, 추후 개선되어야 함
class KtbCommunityServerApplicationTests {

    // Redis는 H2 같은 대체제가 마땅치 않으므로 계속 Mocking을 유지합니다.
    @MockitoBean
    private RedisConnectionFactory redisConnectionFactory;

    @Test
    void contextLoads() {

    }

}
