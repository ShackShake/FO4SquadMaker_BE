package com.shackshake.fo4squardmaker.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.*;

@Entity @Getter @ToString
@NoArgsConstructor
public class PlayerClub {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "player_id")
    private Player player;
    @ManyToOne
    @JoinColumn(name = "club_id")
    private Club club;

    @Builder
    public PlayerClub(Player player, Club club) {
        this.player = player;
        this.club = club;
    }
}
