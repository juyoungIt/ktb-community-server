package com.ktb.howard.ktb_community_server.post.repository;

import com.ktb.howard.ktb_community_server.post.domain.Post;
import com.ktb.howard.ktb_community_server.post.dto.CountInfoDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    @Query("select new com.ktb.howard.ktb_community_server.post.dto.CountInfoDto(p.likeCount, p.viewCount) " +
            "from Post p " +
            "where p.id = :postId")
    Optional<CountInfoDto> findPostCountInfoById(@Param("postId") Long postId);

    @Modifying(clearAutomatically = true)
    @Query("update Post p set p.viewCount = :count where p.id = :postId")
    void updateViewCount(@Param("postId") Long postId, @Param("count") Long count);

    @Modifying(clearAutomatically = true)
    @Query("update Post p set p.likeCount = :count where p.id = :postId")
    void updateLikeCount(@Param("postId") Long postId, @Param("count") Long count);

}
