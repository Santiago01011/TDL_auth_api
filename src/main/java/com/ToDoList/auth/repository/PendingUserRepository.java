package com.ToDoList.auth.repository;

import com.ToDoList.auth.model.PendingUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PendingUserRepository extends JpaRepository<PendingUser, UUID> {
    Optional<PendingUser> findByVerificationCode(String verificationCode);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    Optional<PendingUser> findByEmail(String email);
}
