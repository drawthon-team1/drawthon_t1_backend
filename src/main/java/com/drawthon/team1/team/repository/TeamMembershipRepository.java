package com.drawthon.team1.team.repository;

import com.drawthon.team1.team.domain.TeamMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TeamMembershipRepository extends JpaRepository<TeamMembership, Long> {

    boolean existsByTeamIdAndMemberId(Long teamId, Long memberId);

    Optional<TeamMembership> findByTeamIdAndMemberId(Long teamId, Long memberId);

    long countByTeamId(Long teamId);

    @Query("""
            select tm from TeamMembership tm
            join fetch tm.team t
            join fetch t.owner
            where tm.member.id = :memberId
            order by tm.createdAt desc
            """)
    List<TeamMembership> findAllWithTeamByMemberId(@Param("memberId") Long memberId);

    @Query("""
            select tm from TeamMembership tm
            join fetch tm.member
            where tm.team.id = :teamId
            order by tm.createdAt asc
            """)
    List<TeamMembership> findAllWithMemberByTeamId(@Param("teamId") Long teamId);
}
