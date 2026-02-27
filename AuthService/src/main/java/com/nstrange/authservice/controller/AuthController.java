package com.nstrange.authservice.controller;

import com.nstrange.authservice.entities.RefreshToken;
import com.nstrange.authservice.model.UserInfoDto;
import com.nstrange.authservice.response.JwtResponseDTO;
import com.nstrange.authservice.service.JwtService;
import com.nstrange.authservice.service.RefreshTokenService;
import com.nstrange.authservice.service.UserDetailsServiceImpl;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@AllArgsConstructor
@RestController
public class AuthController {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @PostMapping("/auth/v1/signup")
    public ResponseEntity<JwtResponseDTO> signUp(@RequestBody @Valid UserInfoDto userInfoDto) {
        String userId = userDetailsService.signupUser(userInfoDto);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(userInfoDto.getUsername());
        String jwtToken = jwtService.generateToken(userInfoDto.getUsername());
        return ResponseEntity.ok(JwtResponseDTO.builder()
                .accessToken(jwtToken)
                .token(refreshToken.getToken())
                .userId(userId)
                .build());
    }

    @GetMapping("/ping")
    public ResponseEntity<String> simplePing() {
        return ResponseEntity.ok("pong");
    }

    @GetMapping("/auth/v1/ping")
    public ResponseEntity<String> ping() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String userId = userDetailsService.getUserByUsername(authentication.getName());
            if (userId != null) {
                return ResponseEntity.ok("Ping Successful for user: " + userId);
            }
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
    }

    @GetMapping("/health")
    public ResponseEntity<Boolean> checkHealth() {
        return ResponseEntity.ok(true);
    }
}