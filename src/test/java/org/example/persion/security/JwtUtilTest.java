package org.example.persion.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    @Test
    void testJwtGenerationAndParsing() {
        // Generate a secure key for HS256
        SecretKey key = Keys.secretKeyFor(SignatureAlgorithm.HS256);

        // Create a JWT token
        String token = Jwts.builder()
                .setSubject("test-user")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 60000)) // 1 minute expiration
                .signWith(key)
                .compact();

        // Parse the JWT token
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        // Validate the claims
        assertNotNull(claims);
        assertEquals("test-user", claims.getSubject());
    }
}
