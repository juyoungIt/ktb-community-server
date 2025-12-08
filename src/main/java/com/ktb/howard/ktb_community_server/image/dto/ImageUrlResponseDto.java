package com.ktb.howard.ktb_community_server.image.dto;

import java.time.Instant;

public record ImageUrlResponseDto(
        String url,
        Long imageId,
        Integer sequence,
        String httpMethod,
        Instant expiresAt
) { }
