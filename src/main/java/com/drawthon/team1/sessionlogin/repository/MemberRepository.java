package com.drawthon.team1.sessionlogin.repository;

import com.drawthon.team1.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long>  {
    Optional<Member> findByEmail(String email);
    boolean existsByEmail(String email);
}
