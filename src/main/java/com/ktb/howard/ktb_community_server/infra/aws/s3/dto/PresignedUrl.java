package com.ktb.howard.ktb_community_server.infra.aws.s3.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PresignedUrl {
    private String presignedUrl;
    private String httpMethod;
    private Instant expiresAt;
}
