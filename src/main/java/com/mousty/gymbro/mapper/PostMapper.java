package com.mousty.gymbro.mapper;

import com.mousty.gymbro.aws.S3Service;
import com.mousty.gymbro.entity.Post;
import com.mousty.gymbro.dto.post.PostAddDTO;
import com.mousty.gymbro.dto.post.PostDTO;
import com.mousty.gymbro.entity.PostComment;
import com.mousty.gymbro.dto.post_comment.SimpleCommentDTO;
import com.mousty.gymbro.entity.PostLike;
import com.mousty.gymbro.dto.post_like.SimpleLikeDTO;
import com.mousty.gymbro.entity.User;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Mapper(
        componentModel = "spring",
        uses = {UserMapper.class},
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        builder = @Builder(disableBuilder = true)
)
public abstract class PostMapper {

    @Autowired
    protected S3Service s3Service;

    @Mapping(target = "user", source = "user")
    @Mapping(target = "likes", source = "likes", qualifiedByName = "mapPostLikesToSimpleLikeDTOs")
    @Mapping(target = "comments", source = "comments", qualifiedByName = "mapPostCommentsToSimpleCommentDTOs")
    @Mapping(target = "numLikes", expression = "java(post.getLikes() != null ? post.getLikes().size() : 0)")
    @Mapping(target = "numComments", expression = "java(post.getComments() != null ? post.getComments().size() : 0)")
    @Mapping(target = "image", expression = "java(s3Service.generatePresignedUrl(post.getImageUrl()))")
    @Mapping(target = "content", source = "content")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    public abstract PostDTO toDTO(Post post);

    @Named("mapPostLikesToSimpleLikeDTOs")
    protected List<SimpleLikeDTO> mapPostLikes(List<PostLike> likes) {
        if (likes == null) {
            return null;
        }
        return likes.stream()
                .map(this::toBasicLikeDTO)
                .toList();
    }

    @Named("mapPostCommentsToSimpleCommentDTOs")
    protected List<SimpleCommentDTO> mapPostComments(List<PostComment> comments) {
        if (comments == null) {
            return null;
        }
        return comments.stream()
                .map(this::toBasicCommentDTO)
                .toList();
    }

    @Mapping(target = "id", source = "comment.id")
    @Mapping(target = "content", source = "comment.content")
    @Mapping(target = "postId", source = "comment.post.id")
    @Mapping(target = "username", source = "comment.user.username")
    @Mapping(target = "createdAt", source = "comment.createdAt")
    protected abstract SimpleCommentDTO toBasicCommentDTO(PostComment comment);

    @Mapping(target = "id", source = "like.id")
    @Mapping(target = "username", source = "like.user.username")
    @Mapping(target = "createdAt", source = "like.createdAt")
    protected abstract SimpleLikeDTO toBasicLikeDTO(PostLike like);

    @Mapping(target = "id", source = "dto.id")
    @Mapping(target = "user", source = "user")
    @Mapping(target = "likes", expression = "java(mapSimpleLikeDTOsToPostLikes(dto.getLikes(), user))")
    @Mapping(target = "comments", expression = "java(mapSimpleCommentDTOsToPostComments(dto.getComments(), user))")
    @Mapping(target = "content", source = "dto.content")
    @Mapping(target = "createdAt", source = "dto.createdAt")
    @Mapping(target = "updatedAt", source = "dto.updatedAt")
    @Mapping(target = "imageUrl", source = "dto.image")
    public abstract Post toEntity(PostDTO dto, User user);

    protected List<PostLike> mapSimpleLikeDTOsToPostLikes(List<SimpleLikeDTO> likes, User user) {
        if (likes == null) {
            return null;
        }
        return likes.stream()
                .map(like -> toBasicLikeEntity(like, user))
                .toList();
    }

    protected List<PostComment> mapSimpleCommentDTOsToPostComments(List<SimpleCommentDTO> comments, User user) {
        if (comments == null) {
            return null;
        }
        return comments.stream()
                .map(comment -> toBasicCommentEntity(comment, user))
                .toList();
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", source = "user")
    @Mapping(target = "likes", ignore = true)
    @Mapping(target = "comments", ignore = true)
    @Mapping(target = "content", source = "dto.content")
    @Mapping(target = "createdAt", expression = "java(java.time.Instant.now())")
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "imageUrl", constant = "")
    public abstract Post toNewEntity(PostAddDTO dto, User user);

    @Mapping(target = "id", source = "dto.id")
    @Mapping(target = "content", source = "dto.content")
    @Mapping(target = "user", source = "user")
    @Mapping(target = "post", ignore = true)
    @Mapping(target = "createdAt", source = "dto.createdAt")
    protected abstract PostComment toBasicCommentEntity(SimpleCommentDTO dto, User user);

    @Mapping(target = "id", source = "dto.id")
    @Mapping(target = "user", source = "user")
    @Mapping(target = "post", ignore = true)
    @Mapping(target = "createdAt", source = "dto.createdAt")
    protected abstract PostLike toBasicLikeEntity(SimpleLikeDTO dto, User user);
}