package com.example.short_link.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@MappedSuperclass
@Setter
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

}
