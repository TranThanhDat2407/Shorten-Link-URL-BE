package com.example.short_link.repository;

import com.example.short_link.entity.Link;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LinkRepository extends JpaRepository<Link, Long>, JpaSpecificationExecutor<Link> {
    boolean existsByShortCode (String shortCode);
    Optional<Link> findByShortCode(String shortCode);
}
