package com.mousty.gymbro.mapper;

import com.mousty.gymbro.dto.post_comment.CommentDTO;
import com.mousty.gymbro.dto.post_comment.CommentInput;
import com.mousty.gymbro.dto.post_comment.SimpleCommentDTO;
import com.mousty.gymbro.generic.GenericMapper;
import com.mousty.gymbro.entity.Post;
import com.mousty.gymbro.entity.PostComment;
import com.mousty.gymbro.entity.User;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.UUID;

@Mapper(componentModel = "spring",
        uses = UserMapper.class,
        builder = @Builder(disableBuilder = true)
)
public interface CommentMapper extends GenericMapper<PostComment, CommentDTO> {

    @Mapping(target = "postId", source = "post.id")
    CommentDTO toDTO(PostComment comment);

    @Mapping(target = "post", source = "postId", qualifiedByName = "idToPost")
    @Mapping(target = "user",  source = "user")
    PostComment toEntity(CommentDTO dto);

    @Mapping(target = "id", source = "dto.id")
    @Mapping(target = "content", source = "dto.content")
    @Mapping(target = "createdAt", source = "dto.createdAt")
    @Mapping(target = "post", source = "post")
    @Mapping(target = "user", source = "user")
    PostComment toEntity(SimpleCommentDTO dto, User user, Post post);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "content", source = "request.content")
    @Mapping(target = "post", source = "post")
    @Mapping(target = "user", source = "user")
    @Mapping(target = "createdAt", expression = "java(java.time.Instant.now())")
    PostComment toNewEntity(CommentInput request, User user, Post post);

    @Named("idToPost")
    default Post idToPost(UUID postId) {
        if (postId == null) {
            return null;
        }
        return Post.builder().id(postId).build();
    }

}