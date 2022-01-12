package com.shackshake.fo4squardmaker.entity;

import lombok.*;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import java.util.List;

@Entity @Getter @ToString
@NoArgsConstructor
public class Player {
    @Id
    private Long id;
    private String name;
    private Integer ovr;
    private Integer pay;
    private Boolean isSpidImgSrc;

    @OneToMany(mappedBy = "player")
    private List<PlayerClub> clubPlayerList;

    @Builder
    public Player(Long id, String name, Integer ovr, Integer pay, Boolean isSpidImgSrc) {
        this.id = id;
        this.name = name;
        this.ovr = ovr;
        this.pay = pay;
        this.isSpidImgSrc = isSpidImgSrc;
    }

    // 삭제 예정
    @Builder
    public Player(Long id, String name) {
        this.id = id;
        this.name = name;
    }
}
