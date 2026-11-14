package com.drawthon.team1.team.repository;

import com.drawthon.team1.team.domain.TeamInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TeamInvitationRepository extends JpaRepository<TeamInvitation, Long> {

    @Query("""
            select ti from TeamInvitation ti
            join fetch ti.team t
            join fetch t.owner
            where ti.token = :token
            """)
    Optional<TeamInvitation> findWithTeamByToken(@Param("token") String token);
}
