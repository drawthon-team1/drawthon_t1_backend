package com.drawthon.team1.team.repository;

import com.drawthon.team1.team.domain.Team;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRepository extends JpaRepository<Team, Long> {
}
