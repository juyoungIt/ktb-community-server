package com.ktb.howard.ktb_community_server.scheduler;

import com.ktb.howard.ktb_community_server.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Component
public class CountCacheScheduler {

    private final StringRedisTemplate redisTemplate;
    private final PostRepository postRepository;

    private static final String VIEW_COUNT_PATTERN = "post:*:viewCount";
    private static final String LIKE_COUNT_PATTERN = "post:*:likeCount";

    @Scheduled(fixedDelay = 300000)
    @Transactional
    public void syncPostStats() {
        log.info("카운트 정보 동기화 시작...");
        long viewUpdateCount = syncViewCounts();
        long likeUpdateCount = syncLikeCounts();
        log.info("카운트 정보 동기화 완료... 조회 수={}건, 좋아요 수={}건", viewUpdateCount, likeUpdateCount);
    }

    private long syncViewCounts() {
        long count = 0;
        ScanOptions options = ScanOptions.scanOptions().match(VIEW_COUNT_PATTERN).count(100).build();
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                String key = cursor.next();
                String value = redisTemplate.opsForValue().get(key);
                if (value != null) {
                    long postId = extractPostId(key);
                    long viewCount = Long.parseLong(value);
                    postRepository.updateViewCount(postId, viewCount);
                    count++;
                }
            }
        } catch (Exception e) {
            log.error("조회 수 동기화 실패...", e);
        }
        return count;
    }

    private long syncLikeCounts() {
        long count = 0;
        ScanOptions options = ScanOptions.scanOptions().match(LIKE_COUNT_PATTERN).count(100).build();
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                String key = cursor.next();
                String value = redisTemplate.opsForValue().get(key);
                if (value != null) {
                    long postId = extractPostId(key);
                    long likeCount = Long.parseLong(value);
                    postRepository.updateLikeCount(postId, likeCount);
                    count++;
                }
            }
        } catch (Exception e) {
            log.error("좋아요 수 동기화 실패...", e);
        }
        return count;
    }

    private long extractPostId(String key) {
        try {
            String[] parts = key.split(":");
            return Long.parseLong(parts[1]);
        } catch (Exception e) {
            log.error("Post Id 획득 실패... key: {}", key);
            throw new IllegalArgumentException("Post Id 획득 실패...");
        }
    }

}
