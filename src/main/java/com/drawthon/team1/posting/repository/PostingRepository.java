package com.drawthon.team1.posting.repository;

import com.drawthon.team1.posting.entity.Post;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostingRepository extends JpaRepository<Post, Long> {

    boolean existsByProgramRegistrationNumber(String programRegistrationNumber);

    List<Post> findAllByOrderByIdDesc();

    /** 신청 인원 확인과 저장이 동시에 일어나 모집인원을 넘지 않도록 공고 행을 잠근다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Post p where p.id = :id")
    Optional<Post> findByIdForUpdate(@Param("id") Long id);
}
