package com.example.short_link.entity;

import jakarta.persistence.*;

import java.time.Instant;

@MappedSuperclass
public abstract class BaseEntityWIthUpdate extends BaseEntity {
    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        super.onCreate();
        updatedAt = getCreatedAt();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    // getter
    public Instant getUpdatedAt() {
        return updatedAt;
    }

}
