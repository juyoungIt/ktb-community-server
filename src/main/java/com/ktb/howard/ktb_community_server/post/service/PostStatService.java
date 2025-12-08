package com.ktb.howard.ktb_community_server.post.service;

import com.ktb.howard.ktb_community_server.post.dto.CountInfoDto;
import com.ktb.howard.ktb_community_server.post.exception.PostNotFoundException;
import com.ktb.howard.ktb_community_server.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

import static com.ktb.howard.ktb_community_server.api.PostErrorCode.POST_NOT_FOUND;

@RequiredArgsConstructor
@Service
public class PostStatService {

    private final StringRedisTemplate redisTemplate;
    private final PostRepository postRepository;

    private static final String VIEW_COUNT_KEY_FORMAT = "post:%d:viewCount";
    private static final String LIKE_COUNT_KEY_FORMAT = "post:%d:likeCount";
    private static final Duration VIEW_COUNT_TTL = Duration.ofMinutes(10);
    private static final Duration LIKE_COUNT_TTL = Duration.ofMinutes(10);

    /**
     * 특정 게시글의 조회 수를 1증가
     */
    public void increaseViewCount(long postId) {
        String key = String.format(VIEW_COUNT_KEY_FORMAT, postId);
        if (!redisTemplate.hasKey(key)) {
            CountInfoDto countInfo = postRepository.findPostCountInfoById(postId)
                    .orElseThrow(() -> new PostNotFoundException(POST_NOT_FOUND));
            redisTemplate.opsForValue().setIfAbsent(key, String.valueOf(countInfo.viewCount()), VIEW_COUNT_TTL);
        }
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, VIEW_COUNT_TTL);
    }

    /**
     * 특정 게시글의 조회 수를 가져옴
     */
    public Long getViewCount(long postId) {
        String key = String.format(VIEW_COUNT_KEY_FORMAT, postId);
        String viewCount = redisTemplate.opsForValue().get(key);
        if (viewCount != null) {
            redisTemplate.expire(key, VIEW_COUNT_TTL);
            return Long.parseLong(viewCount);
        }
        CountInfoDto countInfo = postRepository.findPostCountInfoById(postId)
                .orElseThrow(() -> new PostNotFoundException(POST_NOT_FOUND));
        redisTemplate.opsForValue().setIfAbsent(key, String.valueOf(countInfo.viewCount()), VIEW_COUNT_TTL);
        return countInfo.viewCount();
    }

    /**
     * 특정 게시글의 좋아요 수를 삭제함
     */
    public void deleteViewCount(long postId) {
        String key = String.format(VIEW_COUNT_KEY_FORMAT, postId);
        redisTemplate.delete(key);
    }

    /**
     * 특정 게시글의 좋아요 수를 1증가 시킴
     */
    public Long increaseLikeCount(long postId) {
        String key = String.format(LIKE_COUNT_KEY_FORMAT, postId);
        if (!redisTemplate.hasKey(key)) {
            CountInfoDto countInfo = postRepository.findPostCountInfoById(postId)
                    .orElseThrow(() -> new PostNotFoundException(POST_NOT_FOUND));
            redisTemplate.opsForValue().setIfAbsent(key, String.valueOf(countInfo.likeCount()), LIKE_COUNT_TTL);
        }
        redisTemplate.expire(key, LIKE_COUNT_TTL);
        return redisTemplate.opsForValue().increment(key);
    }

    /**
     * 특정 게시글의 좋아요 수를 1감소 시킴
     */
    public Long decreaseLikeCount(long postId) {
        String key = String.format(LIKE_COUNT_KEY_FORMAT, postId);
        if (!redisTemplate.hasKey(key)) {
            CountInfoDto countInfo = postRepository.findPostCountInfoById(postId)
                    .orElseThrow(() -> new PostNotFoundException(POST_NOT_FOUND));
            redisTemplate.opsForValue().setIfAbsent(key, String.valueOf(countInfo.likeCount()), LIKE_COUNT_TTL);
        }
        redisTemplate.expire(key, LIKE_COUNT_TTL);
        return redisTemplate.opsForValue().decrement(key);
    }

    /**
     * 특정 게시글의 좋아요 수를 가져옴
     */
    public Integer getLikeCount(long postId) {
        String key = String.format(LIKE_COUNT_KEY_FORMAT, postId);
        String likeCount = redisTemplate.opsForValue().get(key);
        if (likeCount != null) {
            redisTemplate.expire(key, LIKE_COUNT_TTL);
            return Integer.parseInt(likeCount);
        }
        CountInfoDto countInfo = postRepository.findPostCountInfoById(postId)
                .orElseThrow(() -> new PostNotFoundException(POST_NOT_FOUND));
        redisTemplate.opsForValue().setIfAbsent(key, String.valueOf(countInfo.likeCount()), LIKE_COUNT_TTL);
        return countInfo.likeCount();
    }

    /**
     * 특정 게시글의 좋아요 수를 삭제함
     */
    public void deleteLikeCount(long postId) {
        String key = String.format(LIKE_COUNT_KEY_FORMAT, postId);
        redisTemplate.delete(key);
    }

}
