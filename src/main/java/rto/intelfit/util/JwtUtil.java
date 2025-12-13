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
    private static final String FORCE_LOGOUT_USER_PREFIX = "force_logout_user:";

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }


    public String generateAccessToken(Long userPk) {
        return Jwts.builder()
                .setSubject(String.valueOf(userPk))
                .claim("tokenType", "access")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }




    public String generateRefreshToken(Long userPk) {
        String token = Jwts.builder()
                .setSubject(String.valueOf(userPk))
                .claim("tokenType", "refresh")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + refreshTokenExpiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();

        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + userPk,
                token,
                refreshTokenExpiration,
                TimeUnit.MILLISECONDS
        );

        return token;
    }


    // 토큰에서 사용자 ID 추출
    public String getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.getSubject();
    }

    public Long getUserPkFromToken(String token) {
        return Long.valueOf(parseToken(token).getSubject());
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

            // 사용자 강제 로그아웃 여부 확인
            String userId = claims.getSubject();
            if (isUserForceLoggedOut(userId)) {
                log.warn("사용자가 강제 로그아웃 상태입니다: {}", userId);
                return false;
            }
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

    // 토큰 블랙리스트 확인
    private boolean isTokenBlacklisted(String token) {
        String key = BLACKLIST_TOKEN_PREFIX + token;
        return redisTemplate.hasKey(key);
    }

    public void deleteRefreshToken(Long userPk) {
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + userPk);
    }


    // 특정 사용자 강제 로그아웃 (모든 토큰 거부)
    public void forceLogoutUser(String userId) {
        String key = FORCE_LOGOUT_USER_PREFIX + userId;
        redisTemplate.opsForValue().set(key, "force", refreshTokenExpiration, TimeUnit.MILLISECONDS);
    }

    private boolean isUserForceLoggedOut(String userId) {
        String key = FORCE_LOGOUT_USER_PREFIX + userId;
        return redisTemplate.hasKey(key);
    }

    // 로그인 성공 시 강제 로그아웃 플래그 해제
    public void clearForceLogout(String userId) {
        String key = FORCE_LOGOUT_USER_PREFIX + userId;
        redisTemplate.delete(key);
    }

    // Access Token에서 토큰 타입 확인
    public String getTokenType(String token) {
        Claims claims = parseToken(token);
        return claims.get("tokenType", String.class);
    }
}
