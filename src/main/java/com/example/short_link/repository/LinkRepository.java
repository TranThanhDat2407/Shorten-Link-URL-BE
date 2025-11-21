package com.example.short_link.repository;

import com.example.short_link.entity.Link;
import com.example.short_link.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;


import java.util.List;
import java.util.Optional;

@Repository
public interface LinkRepository extends JpaRepository<Link, Long>, JpaSpecificationExecutor<Link> {
    boolean existsByShortCode (String shortCode);
    Optional<Link> findByShortCode(String shortCode);
    Long countByUserId(Long userId);
}
