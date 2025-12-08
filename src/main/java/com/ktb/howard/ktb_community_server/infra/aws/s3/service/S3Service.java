package com.ktb.howard.ktb_community_server.infra.aws.s3.service;

import com.ktb.howard.ktb_community_server.infra.aws.s3.dto.ObjectMetadata;
import com.ktb.howard.ktb_community_server.infra.aws.s3.dto.PresignedUrl;
import com.ktb.howard.ktb_community_server.infra.aws.s3.exception.FileStorageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
public class S3Service {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${app.s3.bucket}")
    private String bucket;
    @Value("${app.s3.get-ttl-min}")
    private Integer getTtlMin;
    @Value("${app.s3.put-ttl-min}")
    private Integer putTtlMin;

    public PresignedUrl createPutObjectPresignedUrl(String objectKey, String contentType, Long contentLength) {
        try {
            PutObjectRequest objectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .contentType(contentType)
                    .contentLength(contentLength)
                    .build();

            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(putTtlMin))
                    .putObjectRequest(objectRequest)
                    .build();

            PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignRequest);
            return new PresignedUrl(
                    presigned.url().toString(),
                    HttpMethod.PUT.toString(),
                    presigned.expiration()
            );
        } catch (SdkException e) {
            log.error("Put Object 용 Presigned URL 발급실패 : {}, {}, {}", objectKey, contentType, contentLength, e);
            throw new FileStorageException("Put Object 용 Presigned URL 발급 실패. FileStorage 상태를 확인하세요.", e);
        }
    }

    @Cacheable(value = "s3:get-presigned-url", key = "#objectKey", unless = "#result == null")
    public PresignedUrl createGetObjectPresignedUrl(String objectKey) {
        try {
            GetObjectRequest objectRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(getTtlMin))
                    .getObjectRequest(objectRequest)
                    .build();

            PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(presignRequest);
            return new PresignedUrl(
                    presigned.url().toString(),
                    HttpMethod.GET.toString(),
                    presigned.expiration()
            );
        } catch (SdkException e) {
            log.error("Get Object 용 Presigned URL 발급실패 : {}", objectKey, e);
            throw new FileStorageException("Get Object 용 Presigned URL 발급 실패. FileStorage 상태를 확인하세요.", e);
        }
    }

    public Optional<ObjectMetadata> getObjectMetaData(String objectKey) {
        try {
            HeadObjectRequest objectRequest = HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .build();
            HeadObjectResponse response = s3Client.headObject(objectRequest);
            return Optional.of(
                    new ObjectMetadata(
                            objectKey,
                            response.contentLength(),
                            response.contentType(),
                            response.eTag(),
                            response.lastModified()
                    )
            );
        } catch (NoSuchKeyException e) {
            log.info("ObjectKey = {}에 대응하는 Object 없음", objectKey);
            return Optional.empty();
        } catch (SdkException e) {
            log.error("ObjectKey = {}에 대응하는 Object 확인실패", objectKey, e);
            throw new FileStorageException("Head Object 요청처리 실패. FileStorage 상태를 확인하세요.", e);
        }
    }

    public void moveObject(String sourceObjectKey, String destinationObjectKey) {
        if (sourceObjectKey.equals(destinationObjectKey)) {
            log.warn("Source와 Destination ObjectKey가 동일하여 이동 작업을 중단: {}", sourceObjectKey);
            return;
        }
        try {
            CopyObjectRequest copyRequest = CopyObjectRequest.builder()
                    .sourceBucket(bucket)
                    .sourceKey(sourceObjectKey)
                    .destinationBucket(bucket)
                    .destinationKey(destinationObjectKey)
                    .build();
            s3Client.copyObject(copyRequest);
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(sourceObjectKey)
                    .build();
            s3Client.deleteObject(deleteRequest);
        } catch (SdkException e) {
            log.error("Object 이동 실패 {} -> {}", sourceObjectKey, destinationObjectKey, e);
            throw new FileStorageException("Object 이동 요청처리 실패. FileStorage 상태를 확인하세요.", e);
        }
    }

}
