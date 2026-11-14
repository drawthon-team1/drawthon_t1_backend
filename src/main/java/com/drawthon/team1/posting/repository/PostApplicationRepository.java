package com.drawthon.team1.posting.repository;

import com.drawthon.team1.posting.entity.PostApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PostApplicationRepository extends JpaRepository<PostApplication, Long> {

    boolean existsByPostIdAndMemberId(Long postId, Long memberId);

    Optional<PostApplication> findByPostIdAndMemberId(Long postId, Long memberId);

    long countByPostId(Long postId);

    /** 공고별 신청 인원. 신청이 없는 공고는 결과에 없다. */
    @Query("select a.post.id as postId, count(a) as applicantCount from PostApplication a group by a.post.id")
    List<PostApplicantCount> countGroupByPost();

    interface PostApplicantCount {
        Long getPostId();

        long getApplicantCount();
    }

    void deleteByPostId(Long postId);
}
