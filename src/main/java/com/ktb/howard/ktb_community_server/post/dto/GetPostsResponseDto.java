package com.ktb.howard.ktb_community_server.post.dto;

import com.ktb.howard.ktb_community_server.member.dto.MemberInfoResponseDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class GetPostsResponseDto {
    private Long postId;
    private String title;
    private Integer likeCount;
    private Long commentCount;
    private Long viewCount;
    private LocalDateTime createdAt;
    private MemberInfoResponseDto writer;

    public GetPostsResponseDto(
            Long postId,
            String title,
            Integer likeCount,
            Long commentCount,
            Long viewCount,
            LocalDateTime createdAt,
            MemberInfoResponseDto writer
    ) {
        this.postId = postId;
        this.title = title;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
        this.viewCount = viewCount;
        this.createdAt = createdAt.plusHours(9); // UTC to KST
        this.writer = writer;
    }
}
