package com.mousty.gymbro.repository;

import com.mousty.gymbro.entity.PostComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CommentRepository extends JpaRepository<PostComment, UUID> {
}
