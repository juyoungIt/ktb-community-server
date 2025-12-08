package com.ktb.howard.ktb_community_server.post.dto;

import com.ktb.howard.ktb_community_server.member.dto.MemberInfoResponseDto;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
public class PostDetailDto {

    private Long postId;

    private MemberInfoResponseDto writer;

    private List<PostImageInfoDto> postImages;

    private String title;

    private String content;

    @Setter
    private Integer likeCount;

    @Setter
    private Long viewCount;

    private Long commentCount;

    @Setter
    private Boolean isLiked;

    private LocalDateTime createdAt;

    public PostDetailDto(
            Long postId,
            MemberInfoResponseDto writer,
            List<PostImageInfoDto> postImages,
            String title,
            String content,
            Long commentCount,
            LocalDateTime createdAt
    ) {
        this.postId = postId;
        this.writer = writer;
        this.postImages = postImages;
        this.title = title;
        this.content = content;
        this.commentCount = commentCount;
        this.createdAt = createdAt;
    }

}
