package com.example.AIG_ForgeHub.repository;

import com.example.AIG_ForgeHub.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User,Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByRefreshTokenHash(String refreshTokenHash);

    List<User> findByRole(String role);

    Optional<User> findByFullName(String fullName);
}