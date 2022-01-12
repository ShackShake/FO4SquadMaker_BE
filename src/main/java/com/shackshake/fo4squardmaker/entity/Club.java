package com.shackshake.fo4squardmaker.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.*;
import java.util.List;

@Entity @Getter @ToString
@NoArgsConstructor
public class Club {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;
    private String name;

    @OneToMany(mappedBy = "club")
    private List<PlayerClub> clubPlayer;

    @Builder
    public Club(String name) {
        this.name = name;
    }
}
