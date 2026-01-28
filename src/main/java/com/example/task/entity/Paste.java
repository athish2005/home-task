package com.example.task.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "pastes")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Paste {

    @Id
    @Column(length = 50)
    private String id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    private Instant createdAt;

    private Instant expiresAt;   // nullable

    private Integer maxViews;    // nullable

    private Integer viewCount;

}
