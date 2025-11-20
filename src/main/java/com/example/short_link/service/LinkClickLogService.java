package com.example.short_link.service;

import com.example.short_link.entity.Link;
import com.example.short_link.entity.LinkClickLog;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LinkClickLogService {
     void logClickDetails(Link link, HttpServletRequest request);
}
