package com.shackshake.fo4squardmaker.repository;

import com.shackshake.fo4squardmaker.entity.Position;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PositionRepository extends JpaRepository<Position, Long> {
}
