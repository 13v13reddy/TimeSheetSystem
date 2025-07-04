package com.timesheet.offline.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service for handling JWT (JSON Web Token) operations like creation and validation.
 */
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String SECRET_KEY;

    @Value("${jwt.expiration.ms}")
    private long EXPIRATION_TIME;

    /**
     * Extracts the username (email) from a JWT.
     * @param token The JWT.
     * @return The username.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * A generic function to extract a specific claim from a JWT.
     * @param token The JWT.
     * @param claimsResolver A function to apply to the claims.
     * @return The extracted claim.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Generates a new JWT for a given user.
     * It includes user authorities (roles) in a custom claim.
     * @param userDetails The user details object from Spring Security.
     * @return A signed JWT string.
     */
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("auth", userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).collect(Collectors.toList()));
        return generateToken(claims, userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Validates a JWT.
     * It checks if the token belongs to the user and has not expired.
     * @param token The JWT.
     * @param userDetails The user details to validate against.
     * @return true if the token is valid, false otherwise.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * --- FIX FOR 500 ERROR ---
     * Creates the signing key from the plain-text secret in the configuration.
     * The previous method expected a Base64 encoded key, which caused the error.
     * This method correctly uses the raw bytes of the secret string.
     * @return A Key object for signing JWTs.
     */
    private Key getSignInKey() {
        byte[] keyBytes = SECRET_KEY.getBytes();
        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }
}
