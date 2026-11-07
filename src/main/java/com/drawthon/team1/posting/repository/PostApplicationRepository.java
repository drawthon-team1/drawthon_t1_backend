package com.drawthon.team1.posting.repository;

import com.drawthon.team1.posting.entity.PostApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostApplicationRepository extends JpaRepository<PostApplication, Long> {

    boolean existsByPostIdAndUserId(Long postId, Long userId);

    Optional<PostApplication> findByPostIdAndUserId(Long postId, Long userId);

    long countByPostId(Long postId);

    void deleteByPostId(Long postId);

    void deleteByUserId(Long userId);
}
