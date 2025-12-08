package com.ktb.howard.ktb_community_server.comment.service;

import com.ktb.howard.ktb_community_server.comment.domain.Comment;
import com.ktb.howard.ktb_community_server.comment.dto.CommentResponseDto;
import com.ktb.howard.ktb_community_server.comment.dto.CreateCommentResponseDto;
import com.ktb.howard.ktb_community_server.comment.dto.GetCommentsDto;
import com.ktb.howard.ktb_community_server.comment.exception.CommentNotFoundException;
import com.ktb.howard.ktb_community_server.comment.repository.CommentQueryRepository;
import com.ktb.howard.ktb_community_server.comment.repository.CommentRepository;
import com.ktb.howard.ktb_community_server.exception.InvalidRequestException;
import com.ktb.howard.ktb_community_server.image.dto.ImageUrlResponseDto;
import com.ktb.howard.ktb_community_server.image.service.ImageService;
import com.ktb.howard.ktb_community_server.member.domain.Member;
import com.ktb.howard.ktb_community_server.member.exception.MemberNotFoundException;
import com.ktb.howard.ktb_community_server.member.repository.MemberRepository;
import com.ktb.howard.ktb_community_server.post.domain.Post;
import com.ktb.howard.ktb_community_server.post.exception.PostNotFoundException;
import com.ktb.howard.ktb_community_server.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import static com.ktb.howard.ktb_community_server.api.CommentErrorCode.*;
import static com.ktb.howard.ktb_community_server.api.CommonErrorCode.INVALID_REQUEST;
import static com.ktb.howard.ktb_community_server.api.MemberErrorCode.*;
import static com.ktb.howard.ktb_community_server.api.PostErrorCode.*;

@RequiredArgsConstructor
@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final CommentQueryRepository commentQueryRepository;
    private final PostRepository postRepository;
    private final MemberRepository memberRepository;
    private final ImageService imageService;

    @CacheEvict(value = "post:comment", key = "#postId")
    @Transactional
    public CreateCommentResponseDto createComment(
            Long postId,
            Integer memberId,
            Long parentCommentId,
            String content
    ) {
        // 1. 게시글, 작성자 정보 조회
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(POST_NOT_FOUND));
        Member member = memberRepository.findById(memberId.longValue())
                .orElseThrow(() -> new MemberNotFoundException(MEMBER_NOT_FOUND));
        // 2. 대댓글인 경우 상위 댓글 조회
        Comment parentComment = null;
        if (parentCommentId != null) {
            parentComment = commentRepository.findById(parentCommentId)
                    .orElseThrow(() -> new CommentNotFoundException(COMMENT_NOT_FOUND));
        }
        // 3. 댓글 정보 구성 및 저장
        Comment comment = Comment.builder()
                .post(post)
                .member(member)
                .parentComment(parentComment)
                .content(content)
                .build();
        Comment savedComment = commentRepository.save(comment);
        // 4. 댓글 수 1증가 처리
        post.increaseCommentCount();
        return new CreateCommentResponseDto(
                savedComment.getId(),
                savedComment.getPost().getId(),
                savedComment.getMember().getId(),
                parentComment != null ? savedComment.getParentComment().getId() : null,
                savedComment.getContent()
        );
    }

    @Cacheable(value = "post:comment", key = "#postId", condition = "#cursor == 0", unless = "#result.isEmpty()")
    @Transactional(readOnly = true)
    public List<CommentResponseDto> getComments(Long postId, Long cursor, Integer size) {
        PageRequest pageRequest = PageRequest.of(0, size);
        Slice<GetCommentsDto> comments = commentQueryRepository.findCommentsNextPage(postId, cursor, pageRequest);
        return comments.stream()
                .map(c -> {
                    ImageUrlResponseDto imageViewUrl = null;
                    if (c.imageId() != null) {
                        imageViewUrl = imageService.createImageViewUrl(c.imageId(), c.objectKey(), c.sequence());
                    }
                    return new CommentResponseDto(
                            c.commentId(),
                            c.content(),
                            c.createdAt(),
                            c.deletedAt(),
                            c.email(),
                            c.nickname(),
                            c.imageId(),
                            imageViewUrl != null ? imageViewUrl.url() : null
                    );
                }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CommentResponseDto> getChildComments(Long parentCommentId) {
        return commentQueryRepository.getChildComments(parentCommentId).stream()
                .map(c -> {
                    ImageUrlResponseDto imageViewUrl = null;
                    if (c.imageId() != null) {
                        imageViewUrl = imageService.createImageViewUrl(c.imageId(), c.objectKey(), c.sequence());
                    }
                    return new CommentResponseDto(
                            c.commentId(),
                            c.content(),
                            c.createdAt(),
                            c.deletedAt(),
                            c.email(),
                            c.nickname(),
                            c.imageId(),
                            imageViewUrl != null ? imageViewUrl.url() : null
                    );
                })
                .collect(Collectors.toList());
    }

    @CacheEvict(value = "post:comment", key = "#result.post.id")
    @Transactional
    public Comment updateComment(Integer loginMemberId, Long commentId, String content) {
        Comment findComment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException(COMMENT_NOT_FOUND));
        if (!loginMemberId.equals(findComment.getMember().getId())) {
            throw new InvalidRequestException(INVALID_REQUEST);
        }
        return findComment.updateContent(content);
    }

    @CacheEvict(value = "post:comment", key = "#result.post.id")
    @Transactional
    public Comment softDeleteByCommentId(Integer loginMemberId, Long commentId) {
        Comment findComment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException(COMMENT_NOT_FOUND));
        if (!loginMemberId.equals(findComment.getMember().getId())) {
            throw new InvalidRequestException(INVALID_REQUEST);
        }
        return findComment.deleteComment();
    }

}