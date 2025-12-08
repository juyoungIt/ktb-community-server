package com.ktb.howard.ktb_community_server.post_like.service;

import com.ktb.howard.ktb_community_server.member.domain.Member;
import com.ktb.howard.ktb_community_server.member.repository.MemberRepository;
import com.ktb.howard.ktb_community_server.post.domain.Post;
import com.ktb.howard.ktb_community_server.post.repository.PostRepository;
import com.ktb.howard.ktb_community_server.post_like.domain.LikeType;
import com.ktb.howard.ktb_community_server.post_like.domain.PostLike;
import com.ktb.howard.ktb_community_server.member.exception.MemberNotFoundException;
import com.ktb.howard.ktb_community_server.post_like.exception.InvalidLikeLogTypeException;
import com.ktb.howard.ktb_community_server.post_like.exception.PostLikeAlreadyExistException;
import com.ktb.howard.ktb_community_server.post_like.exception.PostLikeNotFoundException;
import com.ktb.howard.ktb_community_server.post.exception.PostNotFoundException;
import com.ktb.howard.ktb_community_server.post_like.repository.PostLikeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static com.ktb.howard.ktb_community_server.api.LikeLogErrorCode.*;
import static com.ktb.howard.ktb_community_server.api.MemberErrorCode.*;
import static com.ktb.howard.ktb_community_server.api.PostErrorCode.*;

@Slf4j
@RequiredArgsConstructor
@Service
public class PostLikeService {

    private final PostLikeRepository postLikeRepository;
    private final PostRepository postRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public void updatePostLike(Long postId, Integer memberId, LikeType type) {
        Optional<PostLike> postLikeOpt = postLikeRepository.findPostLikeByPostIdAndMemberId(postId, memberId);
        if (LikeType.LIKE.equals(type)) {
            if (postLikeOpt.isPresent()) {
                log.error("이미 '좋아요'한 게시글 : postId={}, member={}", postId, memberId);
                throw new PostLikeAlreadyExistException("이미 해당 게시글에 좋아요 한 상태입니다.");
            }
            Post post = postRepository.findById(postId)
                    .orElseThrow(() -> {
                        log.error("존재하지 않는 게시글 : postId={}", postId);
                        return new PostNotFoundException(POST_NOT_FOUND);
                    });
            Member member = memberRepository.findById(memberId.longValue())
                    .orElseThrow(() -> {
                        log.error("존재하지 않는 회원 : memberId={}", memberId);
                        return new MemberNotFoundException(MEMBER_NOT_FOUND);
                    });
            PostLike newPostLike = PostLike.builder()
                    .member(member)
                    .post(post)
                    .build();
            postLikeRepository.save(newPostLike);
        } else if (LikeType.CANCEL.equals(type)) {
            PostLike deletePostLike = postLikeOpt.orElseThrow(() -> {
                log.error("취소할 좋아요 없음 : postId={}, memberId={}", postId, memberId);
                return new PostLikeNotFoundException("취소할 좋아요가 없습니다.");
            });
            postLikeRepository.delete(deletePostLike);
        } else {
            log.error("유효하지 않은 좋아요 로그 타입 : {}", type);
            throw new InvalidLikeLogTypeException(INVALID_LIKE_LOG_TYPE);
        }
    }

}
