package com.ktb.howard.ktb_community_server.comment.domain;

import com.ktb.howard.ktb_community_server.BaseEntity;
import com.ktb.howard.ktb_community_server.member.domain.Member;
import com.ktb.howard.ktb_community_server.post.domain.Post;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
@Table(
        name = "comment",
        indexes = @Index(
                name = "idx_comment_post_id_deleted_at_created_at",
                columnList = "post_id, deleted_at, created_at"
        )
)
public class Comment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private Comment parentComment;

    @Lob
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Builder
    public Comment(Post post, Member member, Comment parentComment, String content) {
        this.post = post;
        this.member = member;
        this.parentComment = parentComment;
        this.content = content;
    }

    public Comment updateContent(String content) {
        this.content = content;
        return this;
    }

    public Comment deleteComment() {
        this.getPost().decreaseCommentCount(); // 댓글 갯수 1감소
        this.updateDeletedAt(LocalDateTime.now());
        return this;
    }

}
