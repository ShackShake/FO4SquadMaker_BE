package com.shackshake.fo4squardmaker.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity @Getter @ToString
@NoArgsConstructor
public class Season {
    @Id
    private Long id;// 피파
    private String name;

    @Builder
    public Season(Long id, String name) {
        this.id = id;
        this.name = name;
    }
}
