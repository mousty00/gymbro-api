package com.mousty.gymbro.repository;

import com.mousty.gymbro.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PostRepository extends JpaRepository<Post, UUID> {
    List<Post> findAllByUser_Username(String userUsername);
}
