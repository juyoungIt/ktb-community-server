package com.ktb.howard.ktb_community_server.post.dto;

import com.querydsl.core.annotations.QueryProjection;

import java.time.LocalDateTime;

public record PostDetailWithLikeInfoDto(
        Long postId,
        String title,
        String content,
        Integer likeCount,
        Long viewCount,
        Long commentCount,
        Integer writerId,
        String writerEmail,
        String writerNickname,
        LocalDateTime createdAt
) {
    @QueryProjection
    public PostDetailWithLikeInfoDto { }
}
