package com.example.short_link.repository;

import com.example.short_link.entity.LinkClickLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LinkClickLogRepository extends JpaRepository<LinkClickLog,Long> {
}
