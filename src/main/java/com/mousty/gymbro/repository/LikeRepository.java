package com.mousty.gymbro.repository;

import com.mousty.gymbro.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LikeRepository extends JpaRepository<PostLike, UUID> {
}
