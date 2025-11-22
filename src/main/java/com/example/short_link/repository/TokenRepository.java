package com.example.short_link.repository;

import com.example.short_link.entity.Token;
import com.example.short_link.entity.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TokenRepository extends JpaRepository<Token, Long> {
    // Lấy tất cả token chưa bị revoked của user (multi-device)
    List<Token> findAllByUserAndRevokedFalse(User user);

    // Lấy tất cả token của user (đã revoke hoặc chưa)
    List<Token> findAllByUser(User user);

    // Tìm token theo giá trị token
    Optional<Token> findByToken(String token);

    // Xóa token theo giá trị
    void deleteByToken(String token);

    @Transactional
    @Modifying
    @Query("DELETE FROM Token t " +
            "WHERE t.revoked = true " +
            "AND t.expiredAt < CURRENT_TIMESTAMP")
    void deleteAllRevokedAndExpired();


    @Query("SELECT t FROM Token t " +
            "WHERE t.user = :user AND t.revoked = false AND t.deviceName LIKE %:deviceType% " +
            "ORDER BY t.createdAt ASC")
    List<Token> findAllActiveTokensByUserAndDeviceType(
            @Param("user") User user,
            @Param("deviceType") String deviceType);
}
