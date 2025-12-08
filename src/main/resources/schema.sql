CREATE TABLE IF NOT EXISTS member (
    member_id	       INT	          NOT NULL AUTO_INCREMENT              COMMENT '회원에게 부여하는 ID',
    email	           VARCHAR(254)   NOT NULL                             COMMENT '회원이 로그인 시 사용하는 이메일',
    password	       VARCHAR(255)	  NOT NULL	                           COMMENT '회원이 로그인 시 사용하는 비밀번호',
    nickname	       VARCHAR(10)	  NOT NULL	                           COMMENT '커뮤니티 서비스 내에서 사용되는 닉네임',
    created_at	       TIMESTAMP	  NOT NULL DEFAULT CURRENT_TIMESTAMP   COMMENT '회원가입을 진행한 일자, 시각',
    modified_at	       TIMESTAMP	      NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '가장 마지막으로 회원정보를 수정한 일자, 시각',
    deleted_at	       TIMESTAMP	      NULL	                           COMMENT 'NULL이 아닌 경우 삭제된 회원임을 의미',

    PRIMARY KEY (member_id),
    UNIQUE (email),
    UNIQUE (nickname),
    CHECK (REGEXP_LIKE(email, '^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$')),
    CHECK (CHAR_LENGTH(nickname) <= 10 AND nickname NOT LIKE '% %')
) COMMENT = '회원정보';

CREATE TABLE IF NOT EXISTS post_like (
    post_like_id BIGINT	    NOT NULL AUTO_INCREMENT	           COMMENT '게시글 좋아요 상태에 부여되는 ID',
    member_id	 INT	    NOT NULL	                       COMMENT '좋아요를 클릭한 회원의 ID',
    post_id	     BIGINT	    NOT NULL	                       COMMENT '좋아요를 받은 게시글의 ID',
    created_at	 TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '좋아요 기록을 생성한 일자, 시각',

    PRIMARY KEY (post_like_id),
    UNIQUE (member_id, post_id),
    FOREIGN KEY (member_id) REFERENCES member (member_id),
    FOREIGN KEY (post_id) REFERENCES post (post_id)
) COMMENT = '게시글 좋아요 상태';

CREATE TABLE IF NOT EXISTS post (
    post_id	      BIGINT	  NOT NULL AUTO_INCREMENT              COMMENT '게시글에 부여되는 ID',
    member_id	  INT	      NOT NULL	                           COMMENT '작성자의 ID',
    title	      VARCHAR(26) NOT NULL	                           COMMENT '게시글 제목',
    content	      LONGTEXT	  NOT NULL	                           COMMENT '게시글 본문',
    like_count	  INT	      NOT NULL DEFAULT 0	               COMMENT '해당 게시글에 대한 좋아요 수',
    view_count	  BIGINT	  NOT NULL DEFAULT 0	               COMMENT '해당 게시글에 대한 조회 수',
    comment_count BIGINT	  NOT NULL DEFAULT 0	               COMMENT '해당 게시글에 달린 댓글의 수',
    created_at	  TIMESTAMP	  NOT NULL DEFAULT CURRENT_TIMESTAMP   COMMENT '게시글을 최초로 생성한 일자, 시각',
    modified_at	  TIMESTAMP	      NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '게시글을 마지막으로 수정한 일자, 시각',
    deleted_at	  TIMESTAMP	      NULL	                           COMMENT 'NULL이 아닌 경우 삭제된 게시글임을 의미',

    PRIMARY KEY (post_id),
    FOREIGN KEY (member_id) REFERENCES member (member_id),
    CHECK (CHAR_LENGTH(title) <= 26)
) COMMENT = '게시글';
## Index 설정
CREATE INDEX idx_post_deleted_at_created_at ON post (deleted_at, created_at);

CREATE TABLE IF NOT EXISTS comment (
    comment_id	        BIGINT	  NOT NULL AUTO_INCREMENT              COMMENT '댓글에 부여되는 ID',
    post_id	            BIGINT	  NOT NULL	                           COMMENT '해당 댓글을 포함하는 게시글의 ID',
    member_id	        INT	          NULL	                           COMMENT '댓글 작성자의 ID',
    parent_comment_id	BIGINT	      NULL	                           COMMENT '댓글에 부여되는 대댓글 기능 사용 시 부모 댓글의 ID',
    content	            TEXT	  NOT NULL	                           COMMENT '댓글 본문',
    created_at	        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP   COMMENT '댓글 최초 작성일, 시각',
    modified_at   	    TIMESTAMP	  NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '댓글을 마지막으로 수정한 일자, 시각',
    deleted_at	        TIMESTAMP	  NULL                             COMMENT 'NULL이 아닌 경우 삭제된 댓글임을 의미',

    PRIMARY KEY (comment_id),
    FOREIGN KEY (post_id) REFERENCES post (post_id),
    FOREIGN KEY (member_id) REFERENCES member (member_id),
    FOREIGN KEY (parent_comment_id) REFERENCES comment (comment_id)
) COMMENT = '댓글';
## Index 설정
CREATE INDEX idx_comment_post_id_deleted_at_created_at ON comment (post_id, deleted_at, created_at);

CREATE TABLE IF NOT EXISTS image (
    image_id	        BIGINT	      NOT NULL AUTO_INCREMENT              COMMENT '이미지에 부여되는 ID',
    owner_id            INT               NULL                             COMMENT '해당 이미지의 소유자 ID',
    image_type          VARCHAR(20)   NOT NULL DEFAULT 'NONE'              COMMENT '이미지 타입 (PROFILE, POST 등)',
    bucket_name         VARCHAR(255)  NOT NULL                             COMMENT 'S3 버킷 이름',
    region              VARCHAR(100)  NOT NULL                             COMMENT 'S3 버킷의 리전',
    object_key          VARCHAR(512)  NOT NULL                             COMMENT 'S3 버킷에 저장된 객체 키 (파일 경로)',
    file_name           VARCHAR(255)  NOT NULL                             COMMENT '원본 파일명',
    file_size           BIGINT        NOT NULL                             COMMENT '파일 사이즈',
    mime_type           VARCHAR(128)  NOT NULL                             COMMENT 'MIME 타입',
    reference_id        BIGINT            NULL                             COMMENT '해당 이미지가 연결되는 도메인의 ID (회원, 게시글 등)',
    sequence	        INT	          NOT NULL DEFAULT 1	               COMMENT '이미지 노출순서',
    status              VARCHAR(20)   NOT NULL                             COMMENT '이미지 상태 (RESERVED, TEMPORAL, PERSIST)',
    created_at	        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP   COMMENT '이미지 최초 생성일시',
    modified_at   	    TIMESTAMP	      NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '이미지 최근 수정일시',
    deleted_at	        TIMESTAMP	      NULL                             COMMENT '이미지 삭제 일시. NULL이 아닌 경우 삭제된 이미지임을 의미',

    PRIMARY KEY (image_id),
    UNIQUE (bucket_name, object_key),
    FOREIGN KEY (owner_id) REFERENCES member (member_id)
) COMMENT = '시스템에서 관리하는 이미지 테이블';
## Index 설정
CREATE INDEX idx_image_owner_id ON image (owner_id);
CREATE INDEX idx_image_type_reference_id ON image (image_type, reference_id);