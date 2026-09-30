package com.mousty.gymbro.repository;

import com.mousty.gymbro.entity.Friendship;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface FriendshipRepository extends JpaRepository<Friendship, UUID> {

    // A friendship row is directional (requester -> recipient) but the relation is mutual:
    // these match the user on either side.
    String INVOLVING = "from Friendship f where f.status = :status "
            + "and (f.user.username = :username or f.friend.username = :username)";

    @Query(value = "select f " + INVOLVING, countQuery = "select count(f) " + INVOLVING)
    Page<Friendship> findAllInvolving(@Param("username") String username, @Param("status") String status, Pageable pageable);

    @Query("select f " + INVOLVING)
    List<Friendship> findAllInvolving(@Param("username") String username, @Param("status") String status);

    /** Pending requests the user has received. */
    List<Friendship> findAllByFriend_UsernameAndStatus(String friendUsername, String status);

    @Query("select count(f) > 0 from Friendship f where (f.user.username = :a and f.friend.username = :b) "
            + "or (f.user.username = :b and f.friend.username = :a)")
    boolean existsBetween(@Param("a") String a, @Param("b") String b);
}
