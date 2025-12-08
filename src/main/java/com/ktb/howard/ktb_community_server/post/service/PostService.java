package com.ktb.howard.ktb_community_server.post.service;

import com.google.common.base.Strings;
import com.ktb.howard.ktb_community_server.exception.InvalidRequestException;
import com.ktb.howard.ktb_community_server.image.domain.Image;
import com.ktb.howard.ktb_community_server.image.dto.CreateImageViewUrlRequestDto;
import com.ktb.howard.ktb_community_server.image.dto.ImageUrlResponseDto;
import com.ktb.howard.ktb_community_server.image.service.ImageService;
import com.ktb.howard.ktb_community_server.member.domain.Member;
import com.ktb.howard.ktb_community_server.member.dto.MemberInfoResponseDto;
import com.ktb.howard.ktb_community_server.member.exception.MemberNotFoundException;
import com.ktb.howard.ktb_community_server.member.repository.MemberRepository;
import com.ktb.howard.ktb_community_server.member.service.MemberService;
import com.ktb.howard.ktb_community_server.post.domain.Post;
import com.ktb.howard.ktb_community_server.post.dto.*;
import com.ktb.howard.ktb_community_server.post.exception.PostImageNotFoundException;
import com.ktb.howard.ktb_community_server.post.exception.PostNotFoundException;
import com.ktb.howard.ktb_community_server.post.repository.PostQueryRepository;
import com.ktb.howard.ktb_community_server.post.repository.PostRepository;
import com.ktb.howard.ktb_community_server.post_like.domain.LikeType;
import com.ktb.howard.ktb_community_server.post_like.exception.InvalidLikeLogTypeException;
import com.ktb.howard.ktb_community_server.post_like.service.PostLikeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.ktb.howard.ktb_community_server.api.CommonErrorCode.INVALID_REQUEST;
import static com.ktb.howard.ktb_community_server.api.LikeLogErrorCode.INVALID_LIKE_LOG_TYPE;
import static com.ktb.howard.ktb_community_server.api.MemberErrorCode.*;
import static com.ktb.howard.ktb_community_server.api.PostErrorCode.*;
import static com.ktb.howard.ktb_community_server.image.domain.ImageType.*;

@Slf4j
@RequiredArgsConstructor
@Service
public class PostService {

    private final ImageService imageService;
    private final MemberService memberService;
    private final PostStatService postStatService;
    private final PostLikeService postLikeService;
    private final PostRepository postRepository;
    private final PostQueryRepository postQueryRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public CreatePostResponseDto createPost(
            Integer memberId,
            String title,
            String content,
            List<PostImageRequestInfoDto> postImages
    ) {
        // 1. 작성자 정보 조회
        Member writer = memberRepository.findById(memberId.longValue())
                .orElseThrow(() -> new MemberNotFoundException(MEMBER_NOT_FOUND));
        // 2. Post Entity 구성 및 저장
        Post post = Post.builder()
                .writer(writer)
                .title(title)
                .content(content)
                .build();
        postRepository.save(post);
        // 3. 임시 공간에 존재하는 게시글 이미지 영속화
        if (postImages != null && !postImages.isEmpty()) {
            postImages.forEach(i -> {
                if (!imageService.isExist(i.imageId())) {
                    log.error("이미지 {}가 존재하지 않습니다.", i.imageId());
                    throw new PostImageNotFoundException(POST_IMAGE_NOT_FOUND);
                }
                imageService.persistImage(i.imageId(), writer, post.getId(), i.sequence());
            });
        }
        return new CreatePostResponseDto(
                post.getId(),
                writer.getId(),
                post.getTitle(),
                post.getContent(),
                postImages
        );
    }

    @Cacheable(
            value = "posts",
            key = "{ #cursor, #size }",
            unless = "#result.isEmpty()")
    @Transactional(readOnly = true)
    public List<GetPostsResponseDto> getPosts(Long cursor, Integer size) {
        PageRequest pageRequest = PageRequest.of(0, size);
        Slice<GetPostsDto> posts = postQueryRepository.findPostsNextPage(cursor, pageRequest);
        return posts.stream()
                .map(post -> {
                    ImageUrlResponseDto imageViewUrl = null;
                    if (post.imageId() != null) {
                        imageViewUrl = imageService
                                .createImageViewUrl(post.imageId(), post.objectKey(), post.sequence());
                    }
                    return new GetPostsResponseDto(
                            post.postId(),
                            post.title(),
                            postStatService.getLikeCount(post.postId()),
                            post.commentCount(),
                            postStatService.getViewCount(post.postId()),
                            post.createdAt(),
                            new MemberInfoResponseDto(
                                    post.email(),
                                    post.nickname(),
                                    post.imageId(),
                                    imageViewUrl != null ? imageViewUrl.url() : null
                            )
                    );
                }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PostDetailDto getPostDetail(Long postId, Integer requestMemberId, Boolean isEdit) {
        PostDetailWithLikeInfoDto postDetail = postQueryRepository.getPostDetail(postId, requestMemberId)
                .orElseThrow(() -> {
                    log.error("찾을 수 없는 게시글 = {}", postId);
                    return new PostNotFoundException(POST_NOT_FOUND);
                });
        MemberInfoResponseDto profile = memberService.getProfile(postDetail.writerId());
        List<PostImageInfoDto> postImages = imageService.createImageViewUrl(new CreateImageViewUrlRequestDto(POST, postId))
                .stream()
                .map(pi -> new PostImageInfoDto(pi.imageId(), pi.url(), pi.sequence(), pi.expiresAt()))
                .toList();
        if (!isEdit) {
            postStatService.increaseViewCount(postId); // Cache에 조회수 갱신
        }
        return PostDetailDto.builder()
                .postId(postId)
                .writer(profile)
                .postImages(postImages)
                .title(postDetail.title())
                .content(postDetail.content())
                .likeCount(postStatService.getLikeCount(postId))
                .viewCount(postStatService.getViewCount(postId))
                .commentCount(postDetail.commentCount())
                .isLiked(postDetail.isLiked())
                .createdAt(postDetail.createdAt().plusHours(9)) // UTC to KST
                .build();
    }

    @Transactional
    public Long likePost(Long postId, Integer memberId, LikeType type) {
        postLikeService.updatePostLike(postId, memberId, type); // 게시글 좋아요 정보 업데이트
        // 캐시정보 갱신
        if (LikeType.LIKE.equals(type)) {
            return postStatService.increaseLikeCount(postId);
        } else if (LikeType.CANCEL.equals(type)) {
            return postStatService.decreaseLikeCount(postId);
        } else {
            log.error("유효하지 않은 좋아요 로그 타입 : {}", type);
            throw new InvalidLikeLogTypeException(INVALID_LIKE_LOG_TYPE);
        }
    }

    @Transactional
    public void updatePost(
            Integer loginMemberId,
            Long postId,
            String title,
            String content,
            List<PostImageRequestInfoDto> requestImages
    ) {
        // 1. 수정할 대상인 게시글 정보를 불러옴
        Post post = postRepository.findById(postId).orElseThrow(() -> {
            log.error("수정할 게시글을 찾을 수 없음 : postId={}", postId);
            return new PostNotFoundException(POST_NOT_FOUND);
        });
        // 2. 현재 요청자가 해당 게시글을 수정할 권한이 있는 지 확인
        if (!loginMemberId.equals(post.getWriter().getId())) {
            log.error("올바르지 않은 요청 : loginMemberId={}, postWriterId={}", loginMemberId, post.getWriter().getId());
            throw new InvalidRequestException(INVALID_REQUEST);
        }
        // 3. 제목에 대한 변경요청이 있는 경우 업데이트를 진행함
        if (!Strings.isNullOrEmpty(title)) {
            post.updateTitle(title);
        }
        // 4. 본문에 대한 변경요청이 있는 경우 업데이트를 진행함
        if (!Strings.isNullOrEmpty(content)) {
            post.updateContent(content);
        }
        // 5. 게시글 이미지에 대한 변경 요청이 있는 경우 업데이트를 진행함
        if (requestImages != null) {
            // 5-1. 기존 이미지들을 ID를 Key로 하는 Map으로 변환
            Map<Long, Image> existingImageMap = imageService.findImages(POST, postId).stream()
                    .collect(Collectors.toMap(Image::getId, Function.identity()));

            // 3-2. 요청된 이미지 ID Set 생성 (삭제 대상 식별용)
            Set<Long> requestImageIds = requestImages.stream()
                    .map(PostImageRequestInfoDto::imageId)
                    .collect(Collectors.toSet());

            // 3-3. 삭제 대상 처리 (기존 이미지 중 요청에 없는 것)
            existingImageMap.keySet().stream()
                    .filter(existingId -> !requestImageIds.contains(existingId))
                    .forEach(imageService::deleteImage); // imageId로 soft-delete

            // 3-4. 추가 및 순서 변경 처리
            for (PostImageRequestInfoDto requestImage : requestImages) {
                Long imageId = requestImage.imageId();
                Integer newSequence = requestImage.sequence();
                Image existingImage = existingImageMap.get(imageId);
                if (existingImage != null) {
                    // 수정 대상: 순서가 변경되었다면 업데이트
                    if (!existingImage.getSequence().equals(newSequence)) {
                        existingImage.updateSequence(newSequence);
                    }
                } else {
                    // 추가 대상: 이미지를 영속화하고 게시글과 연결
                    imageService.persistImage(imageId, post.getWriter(), postId, requestImage.sequence());
                }
            }
        }
    }

    @Transactional
    public void deletePostById(Integer loginMemberId, Long postId) {
        Post findPost = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(POST_NOT_FOUND));
        if (!loginMemberId.equals(findPost.getWriter().getId())) {
            throw new InvalidRequestException(INVALID_REQUEST);
        }
        postStatService.deleteLikeCount(postId); // 좋아요 수 캐시에서 해당 post 제거
        postStatService.deleteViewCount(postId); // 조회수 캐시에서 해당 post 제거
        postRepository.deleteById(postId);
    }

}
