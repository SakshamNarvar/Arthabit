package com.nstrange.authservice.repository;

import com.nstrange.authservice.entities.RefreshToken;
import com.nstrange.authservice.entities.UserInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Integer> {
    Optional<RefreshToken> findByToken(String token);
    void deleteByUserInfo(UserInfo userInfo);
}