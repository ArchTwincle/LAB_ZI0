package com.example.tourism_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder // Именно эта аннотация создает UserSessionBuilder
public class UserSession {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String userEmail;

    // ДОБАВЬ ЭТО ПОЛЕ:
    @Column(length = 1024) // Увеличиваем длину, так как JWT может быть длинным
    private String accessToken;

    @Column(length = 1024)
    private String refreshToken;

    private Instant refreshTokenExpiry;

    @Enumerated(EnumType.STRING)
    private SessionStatus status;
}