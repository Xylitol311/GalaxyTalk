package com.galaxytalk.letter.dto;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@AllArgsConstructor
@Getter
@Entity
@Table(name="Planets")
public class Planets {

    public Planets(){}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String avatar;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    // 행성 1 : 유저 N 관계
    @OneToMany(mappedBy = "planets")
    private List<Users> users;
}
