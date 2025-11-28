package com.ktb.howard.ktb_community_server;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import javax.sql.DataSource;

@SpringBootTest
// TODO : CI 환경에서 우선 테스트 통과를 받기 위해 작성한 코드, 추후 개선되어야 함
@TestPropertySource(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect", // 1. 방언 강제 설정
        "spring.jpa.hibernate.ddl-auto=none", // 2. 스키마 검증/생성 끄기 (핵심!)
        "spring.sql.init.mode=never"          // 3. data.sql, schema.sql 실행 끄기 (혹시 있을 경우 대비)
})
class KtbCommunityServerApplicationTests {

    // 1. MySQL (DB) 연결을 가로채서 껍데기(Mock)만 남김
    // -> CI 환경에서 1차적으로 테스트를 통과 시키기 위함(1)
    @MockitoBean
    private DataSource dataSource;

    // 2. Redis 연결을 가로채서 껍데기(Mock)만 남김
    // -> CI 환경에서 1차적으로 테스트를 통과 시키기 위함(2)
    @MockitoBean
    private RedisConnectionFactory redisConnectionFactory;

    @Test
    void contextLoads() {

    }

}
