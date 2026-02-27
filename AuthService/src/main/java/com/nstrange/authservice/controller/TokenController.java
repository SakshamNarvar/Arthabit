package com.nstrange.authservice.controller;

import com.nstrange.authservice.entities.RefreshToken;
import com.nstrange.authservice.exception.InvalidCredentialsException;
import com.nstrange.authservice.exception.TokenRefreshException;
import com.nstrange.authservice.request.AuthRequestDTO;
import com.nstrange.authservice.request.RefreshTokenRequestDTO;
import com.nstrange.authservice.response.JwtResponseDTO;
import com.nstrange.authservice.service.JwtService;
import com.nstrange.authservice.service.RefreshTokenService;
import com.nstrange.authservice.service.UserDetailsServiceImpl;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TokenController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @PostMapping("/auth/v1/login")
    public ResponseEntity<JwtResponseDTO> authenticateAndGetToken(@RequestBody @Valid AuthRequestDTO authRequestDTO) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authRequestDTO.getUsername(), authRequestDTO.getPassword()));

            if (!authentication.isAuthenticated()) {
                throw new InvalidCredentialsException("Invalid username or password");
            }
        } catch (UsernameNotFoundException | BadCredentialsException ex) {
            throw new InvalidCredentialsException("Invalid username or password");
        } catch (AuthenticationException ex) {
            throw new InvalidCredentialsException("Authentication failed: " + ex.getMessage());
        }

        RefreshToken refreshToken = refreshTokenService.createRefreshToken(authRequestDTO.getUsername());
        String userId = userDetailsService.getUserByUsername(authRequestDTO.getUsername());
        String accessToken = jwtService.generateToken(authRequestDTO.getUsername());

        return ResponseEntity.ok(JwtResponseDTO.builder()
                .accessToken(accessToken)
                .token(refreshToken.getToken())
                .userId(userId)
                .build());
    }

    @PostMapping("/auth/v1/refreshToken")
    public ResponseEntity<JwtResponseDTO> refreshToken(@RequestBody @Valid RefreshTokenRequestDTO refreshTokenRequestDTO) {
        return refreshTokenService.findByToken(refreshTokenRequestDTO.getToken())
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUserInfo)
                .map(userInfo -> {
                    String accessToken = jwtService.generateToken(userInfo.getUsername());
                    return ResponseEntity.ok(JwtResponseDTO.builder()
                            .accessToken(accessToken)
                            .token(refreshTokenRequestDTO.getToken())
                            .build());
                })
                .orElseThrow(() -> new TokenRefreshException(
                        refreshTokenRequestDTO.getToken(),
                        "Refresh token not found. Please login again"));
    }
}