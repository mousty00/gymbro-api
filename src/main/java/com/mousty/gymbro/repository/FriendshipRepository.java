package com.mousty.gymbro.repository;

import com.mousty.gymbro.entity.Friendship;
import com.mousty.gymbro.dto.friendship.FriendshipDTO;
import com.mousty.gymbro.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FriendshipRepository extends JpaRepository<Friendship, UUID> {

    String user(User user);

    Page<FriendshipDTO> findByUser_UsernameAndStatus(String userUsername, String status, Pageable pageable);

    List<Friendship> findAllByUser_UsernameAndStatus(String userUsername, String status);
}
