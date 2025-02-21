package com.galaxytalk.auth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Builder
@AllArgsConstructor
@Getter
@Setter
@Entity
@EntityListeners(AuditingEntityListener.class) //데이터 생성시 생성일자 자동 업로드 위한 어노테이션
@Table(name="Users")
public class Users {

    public Users(){}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique=true)
    private String serialNumber;

    @Column
    private String mbti;

    // 행성 1 : 유저 N 관계, 지연로딩 설정, planet_id 로 join
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "planet_id")
    private Planets planets;

    @Column(nullable = false)
    @ColumnDefault("36")
    private int energy;

    @Column(nullable = false)
    @ColumnDefault("0")
    private int numberOfBlocks;

    //ADMIN, USER로 열거형 맵핑
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    @CreatedDate
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime withdrawnAt;

}
