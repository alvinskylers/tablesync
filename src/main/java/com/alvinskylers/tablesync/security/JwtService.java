package com.alvinskylers.tablesync.security;

import com.alvinskylers.tablesync.exception.JwtValidationException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration-ms}")
    private long jwtExpirationMs;

    private SecretKey signingKey;

    @PostConstruct
    private void init() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException("jwt.secret is not configured");
        }

        try {
            byte[] bytes = Base64.getDecoder().decode(jwtSecret);
            this.signingKey = Keys.hmacShaKeyFor(bytes);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("jwt.secret is not base64", e);
        } catch (WeakKeyException e) {
            throw new IllegalStateException("jwt.secret is too short/weak for HMAC signing", e);
        }

        if (jwtExpirationMs < 0) {
            throw new IllegalArgumentException("jwt.expiration-ms must be positive");
        }
     }

    public String generateToken(UserPrincipal userDetails) {
        try {
            return Jwts.builder()
                    .subject(userDetails.getUsername())
                    .claim("id", userDetails.getUser().getId().toString())
                    .claim("role", userDetails.getUser().getRole().name())
                    .issuedAt(new Date(System.currentTimeMillis()))
                    .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                    .signWith(signingKey)
                    .compact();
        } catch (Exception e) {
            throw new JwtException("Could not generate JWT", e);
        }
    }

    public Claims parseClaims(String token) {
        try {
            return  Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new JwtValidationException("jwt token has expired", e);
        } catch (UnsupportedJwtException e) {
            throw new JwtValidationException("unsupported jwt token", e);
        } catch (MalformedJwtException e) {
            throw new JwtValidationException("malformed jwt token.", e);
        } catch (SignatureException  e) {
            throw new JwtValidationException("invalid jwt token signature", e);
        } catch (IllegalArgumentException e) {
            throw new JwtValidationException("jwt claims is empty or null", e);
        }
    }

    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtValidationException e) {
            return false;
        }
    }


}
