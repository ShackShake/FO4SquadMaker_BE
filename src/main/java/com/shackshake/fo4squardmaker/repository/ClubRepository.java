package com.shackshake.fo4squardmaker.repository;

import com.shackshake.fo4squardmaker.entity.Club;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClubRepository extends JpaRepository<Club, Long> {
    Optional<Club> findByName(String clubName);
}
