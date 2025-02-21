package com.galaxytalk.letter.dto;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class) //데이터 생성시 생성일자 자동 업로드 위한 어노테이션
@Table(name = "letter")
public class Letter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String senderId;
    @Column(nullable = false)
    private String receiverId;
    @Column(nullable = false)
    private String content;
    @Column(nullable = false)
    @CreatedDate
    private LocalDateTime createdAt;
    @Column(nullable = false)
    private String chatRoomId;
    @Column(nullable = false)
    @ColumnDefault("0")
    private int isHide;

    public Letter(String senderId, String receiverId, String content, String chatRoomId) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.content = content;
        this.chatRoomId = chatRoomId;
    }


}
