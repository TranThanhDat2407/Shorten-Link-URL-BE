package com.example.short_link.repository;

import com.example.short_link.entity.Token;
import com.example.short_link.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TokenRepository extends JpaRepository<Token, Long> {
    List<Token> findAllByUserAndRevokedFalseAndExpiredFalse(User user);
    Optional<Token> findByToken(String token);
    void deleteAllByUser(User user);
}
