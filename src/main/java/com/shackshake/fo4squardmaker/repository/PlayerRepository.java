package com.shackshake.fo4squardmaker.repository;

import com.shackshake.fo4squardmaker.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Long> {

}
