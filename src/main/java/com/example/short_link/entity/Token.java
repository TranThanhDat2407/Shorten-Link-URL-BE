package com.example.short_link.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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
    @Column(name = "token_type")
    private String token_type;

    @Column(name = "token", columnDefinition = "TEXT", nullable = false)
    private String token;

    @Column(name = "revoke")
    private Boolean revoke;

    @Column(name = "expired_at")
    private Instant expiredAt;

    @ManyToOne
    private User user;
}
