package com.ktb.howard.ktb_community_server.member.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MemberInfoResponseDto {
    private String email;
    private String nickname;
    private Long imageId;
    private String profileImageUrl;

    public MemberInfoResponseDto(String email, String nickname, Long imageId, String profileImageUrl) {
        this.email = email;
        this.nickname = nickname;
        this.imageId = imageId;
        this.profileImageUrl = profileImageUrl;
    }
}
