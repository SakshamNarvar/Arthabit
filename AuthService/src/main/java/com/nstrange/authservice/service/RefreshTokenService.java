package com.nstrange.authservice.service;

import com.nstrange.authservice.entities.RefreshToken;
import com.nstrange.authservice.entities.UserInfo;
import com.nstrange.authservice.exception.TokenRefreshException;
import com.nstrange.authservice.repository.RefreshTokenRepository;
import com.nstrange.authservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {

    @Autowired
    RefreshTokenRepository refreshTokenRepository;

    @Autowired
    UserRepository userRepository;

    @Transactional
    public RefreshToken createRefreshToken(String username) {
        UserInfo userInfoExtracted = userRepository.findByUsername(username);
        // Delete any existing refresh token for this user (OneToOne constraint)
        refreshTokenRepository.deleteByUserInfo(userInfoExtracted);
        refreshTokenRepository.flush(); // Force DELETE SQL before INSERT to avoid unique constraint violation
        RefreshToken refreshToken = RefreshToken.builder()
                .userInfo(userInfoExtracted)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(6000000))
                .build();
        return refreshTokenRepository.save(refreshToken);
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        if(token.getExpiryDate().compareTo(Instant.now())<0) {
            refreshTokenRepository.delete(token);
            throw new TokenRefreshException(token.getToken(),
                    "Refresh token has expired. Please make a new sign-in request");
        }
        return token;
    }

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }
}