package com.ktb.howard.ktb_community_server.comment.dto;

import com.ktb.howard.ktb_community_server.member.dto.MemberInfoResponseDto;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor
@Getter
public class CommentResponseDto {

    private Long commentId;

    private String content;

    private MemberInfoResponseDto writerInfo;

    private LocalDateTime createdAt;

    private LocalDateTime deletedAt;

    public CommentResponseDto(
            Long commentId,
            String content,
            LocalDateTime createdAt,
            LocalDateTime deletedAt,
            String email,
            String nickname,
            Long imageId,
            String writerProfileImageUrl
    ) {
        this.commentId = commentId;
        this.content = content;
        this.writerInfo = new MemberInfoResponseDto(email, nickname, imageId, writerProfileImageUrl);
        this.createdAt = createdAt.plusHours(9); // UTC to KST (1)
        this.deletedAt = deletedAt == null ? deletedAt : deletedAt.plusHours(9); // UTC to KST (2)
    }

}
