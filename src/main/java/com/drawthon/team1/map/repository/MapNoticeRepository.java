package com.drawthon.team1.map.repository;

import com.drawthon.team1.map.domain.MapNotice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MapNoticeRepository extends JpaRepository<MapNotice, Long> {
}
