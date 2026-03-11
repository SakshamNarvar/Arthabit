package com.nstrange.expenseservice.client;

import com.nstrange.expenseservice.exception.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * Client that calls AuthService /auth/v1/ping to validate JWTs and retrieve the trusted userId.
 */
@Component
public class AuthClient {

    private static final Logger log = LoggerFactory.getLogger(AuthClient.class);

    private final RestTemplate restTemplate;
    private final String authServiceBaseUrl;

    @Autowired
    public AuthClient(RestTemplate restTemplate,
                      @Value("${auth-service.base-url}") String authServiceBaseUrl) {
        this.restTemplate = restTemplate;
        this.authServiceBaseUrl = authServiceBaseUrl;
    }

    /**
     * Forwards the caller's Authorization header to AuthService /auth/v1/ping
     * and returns the trusted userId from the response body.
     *
     * @param authorizationHeader the full "Bearer <token>" header value
     * @return trusted userId (UUID string) returned by AuthService
     * @throws UnauthorizedException if the token is missing, invalid, or expired
     */
    public String authenticateAndGetUserId(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("Missing or malformed Authorization header");
        }

        String pingUrl = authServiceBaseUrl + "/auth/v1/ping";
        log.debug("Calling AuthService ping at {}", pingUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, authorizationHeader);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    pingUrl, HttpMethod.GET, request, String.class);

            String userId = response.getBody();
            if (userId == null || userId.isBlank()) {
                throw new UnauthorizedException("AuthService returned empty userId");
            }

            log.debug("AuthService authenticated userId={}", userId);
            return userId;

        } catch (HttpClientErrorException.Unauthorized ex) {
            log.warn("AuthService returned 401: {}", ex.getMessage());
            throw new UnauthorizedException("Authentication failed: invalid or expired token", ex);
        } catch (HttpClientErrorException ex) {
            log.error("AuthService returned HTTP {}: {}", ex.getStatusCode(), ex.getMessage());
            throw new UnauthorizedException("Authentication failed: " + ex.getMessage(), ex);
        } catch (Exception ex) {
            log.error("Error calling AuthService: {}", ex.getMessage(), ex);
            throw new UnauthorizedException("Unable to verify authentication", ex);
        }
    }
}

