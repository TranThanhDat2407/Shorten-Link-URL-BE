package com.example.short_link.entity;

import com.example.short_link.enums.TokenType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Token extends BaseEntityWIthUpdate{

    @Enumerated(EnumType.STRING)
    @Column(name = "token_type")
    private TokenType tokenType;

    @Column(name = "token", columnDefinition = "TEXT", nullable = false)
    private String token;

    @Column(name = "revoked")
    private Boolean revoked;

    @Column(name = "expired")
    private Boolean expired;

    @Column(name = "expired_at")
    private Instant expiredAt;

    @ManyToOne
    private User user;
}
