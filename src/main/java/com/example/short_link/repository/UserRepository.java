package com.example.short_link.repository;

import com.example.short_link.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User,Long> {
    //find user by email
    Optional<User> findByEmail(String email);

    //check if user exist
    boolean existsByEmail (String email);


}
