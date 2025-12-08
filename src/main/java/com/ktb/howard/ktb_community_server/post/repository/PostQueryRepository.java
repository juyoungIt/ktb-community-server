package com.ktb.howard.ktb_community_server.post.repository;

import com.ktb.howard.ktb_community_server.image.domain.ImageType;
import com.ktb.howard.ktb_community_server.post.dto.GetPostsDto;
import com.ktb.howard.ktb_community_server.post.dto.PostDetailWithLikeInfoDto;
import com.ktb.howard.ktb_community_server.post.dto.QGetPostsDto;
import com.ktb.howard.ktb_community_server.post.dto.QPostDetailWithLikeInfoDto;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static com.ktb.howard.ktb_community_server.image.domain.QImage.image;
import static com.ktb.howard.ktb_community_server.member.domain.QMember.member;
import static com.ktb.howard.ktb_community_server.post.domain.QPost.post;

@AllArgsConstructor
@Repository
public class PostQueryRepository {

    private final JPAQueryFactory queryFactory;

    public Slice<GetPostsDto> findPostsNextPage(Long lastPostId, PageRequest pageRequest) {
        BooleanBuilder whereClause = new BooleanBuilder();
        if (lastPostId != null && lastPostId > 0) {
            whereClause.and(post.id.lt(lastPostId));
        }
        List<GetPostsDto> posts = queryFactory
                .select(new QGetPostsDto(
                        post.id,
                        post.title,
                        post.likeCount,
                        post.viewCount,
                        post.commentCount,
                        member.id,
                        member.email,
                        member.nickname,
                        image.id,
                        image.objectKey,
                        image.sequence,
                        post.createdAt
                ))
                .from(post)
                    .leftJoin(member).on(post.writer.id.eq(member.id))
                    .leftJoin(image).on(member.id.eq(image.owner.id).and(image.imageType.eq(ImageType.PROFILE)))
                .where(whereClause)
                .orderBy(post.createdAt.desc())
                .limit(pageRequest.getPageSize() + 1)
                .fetch();
        boolean hasNext = false;
        if (posts.size() > pageRequest.getPageSize()) {
            posts.remove(pageRequest.getPageSize());
            hasNext = true;
        }
        return new SliceImpl<>(posts, pageRequest, hasNext);
    }

    public Optional<PostDetailWithLikeInfoDto> getPostDetail(Long postId) {
        PostDetailWithLikeInfoDto postDetail = queryFactory
                .select(new QPostDetailWithLikeInfoDto(
                        post.id,
                        post.title,
                        post.content,
                        post.likeCount,
                        post.viewCount,
                        post.commentCount,
                        post.writer.id,
                        post.writer.email,
                        post.writer.nickname,
                        post.createdAt
                ))
                .from(post)
                .where(post.id.eq(postId))
                .fetchFirst();
        return Optional.ofNullable(postDetail);
    }

}
