package com.ktb.howard.ktb_community_server.post.dto;

import com.ktb.howard.ktb_community_server.member.dto.MemberInfoResponseDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GetPostsResponseDto {
    private Long postId;
    private String title;
    private Integer likeCount;
    private Long commentCount;
    private Long viewCount;
    private LocalDateTime createdAt;
    private MemberInfoResponseDto writer;
}
