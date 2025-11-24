package rto.intelfit.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${jwt.secret:mySecretKeyForJwtTokenGenerationAndValidation1234567890}")
    private String secretKey;

    @Value("${jwt.access-token-expiration:3600000}") // 1시간 (3600000ms)
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration:604800000}") // 7일 (604800000ms)
    private long refreshTokenExpiration;

    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";
    private static final String BLACKLIST_TOKEN_PREFIX = "blacklist_token:";

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    // Access Token 생성
    public String generateAccessToken(String userId, Long userPk) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpiration);

        return Jwts.builder()
                .setSubject(userId)
                .claim("userPk", userPk)
                .claim("tokenType", "access")
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // Refresh Token 생성 및 Redis 저장
    public String generateRefreshToken(String userId, Long userPk) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenExpiration);

        String refreshToken = Jwts.builder()
                .setSubject(userId)
                .claim("userPk", userPk)
                .claim("tokenType", "refresh")
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();

        // Redis에 Refresh Token 저장
        String key = REFRESH_TOKEN_PREFIX + userId;
        redisTemplate.opsForValue().set(key, refreshToken, refreshTokenExpiration, TimeUnit.MILLISECONDS);

        return refreshToken;
    }

    // 토큰에서 사용자 ID 추출
    public String getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.getSubject();
    }

    // 토큰에서 사용자 PK 추출
    public Long getUserPkFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("userPk", Long.class);
    }

    // 토큰 파싱
    private Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // 토큰 유효성 검증
    public boolean validateToken(String token) {
        try {
            log.debug("토큰 검증 시작 - 토큰 길이: {}", token != null ? token.length() : 0);

            // 블랙리스트 확인
            if (isTokenBlacklisted(token)) {
                log.warn("블랙리스트에 등록된 토큰");
                return false;
            }

            Claims claims = parseToken(token);
            log.debug("토큰 파싱 성공 - 사용자: {}, 만료시간: {}",
                    claims.getSubject(), claims.getExpiration());
            return true;
        } catch (Exception e) {
            log.error("토큰 검증 실패: {}", e.getMessage());
            return false;
        }
    }

    // Refresh Token 유효성 검증
    public boolean validateRefreshToken(String refreshToken, String userId) {
        try {
            String key = REFRESH_TOKEN_PREFIX + userId;
            String storedToken = redisTemplate.opsForValue().get(key);
            return refreshToken.equals(storedToken) && validateToken(refreshToken);
        } catch (Exception e) {
            log.error("Refresh 토큰 검증 실패: {}", e.getMessage());
            return false;
        }
    }

    // 토큰 만료 확인
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    // 토큰 블랙리스트 등록 (로그아웃 시)
    public void blacklistToken(String token) {
        try {
            Claims claims = parseToken(token);
            Date expiration = claims.getExpiration();
            long ttl = expiration.getTime() - System.currentTimeMillis();

            if (ttl > 0) {
                String key = BLACKLIST_TOKEN_PREFIX + token;
                redisTemplate.opsForValue().set(key, "blacklisted", ttl, TimeUnit.MILLISECONDS);
            }
        } catch (Exception e) {
            log.error("토큰 블랙리스트 등록 실패: {}", e.getMessage());
        }
    }


    private boolean isTokenBlacklisted(String token) {
        String key = BLACKLIST_TOKEN_PREFIX + token;
        return redisTemplate.hasKey(key);
    }

    // Refresh Token 삭제 (로그아웃 시)
    public void deleteRefreshToken(String userId) {
        String key = REFRESH_TOKEN_PREFIX + userId;
        redisTemplate.delete(key);
    }

    // Access Token에서 토큰 타입 확인
    public String getTokenType(String token) {
        Claims claims = parseToken(token);
        return claims.get("tokenType", String.class);
    }
}