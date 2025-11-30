package rto.intelfit.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import rto.intelfit.util.JwtUtil;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    // JWT 검증을 건너뛸 경로들
    private static final List<String> EXCLUDE_PATHS = Arrays.asList(
            "/actuator",
            "/swagger-ui",
            "/v3/api-docs",
            "/api/users/signup",
            "/api/users/login",
            "/api/users/check-userId",
            "/api/users/send-verification-code",
            "/api/users/find-userId",
            "/api/users/reset-password",
            "/api/users/change-password",
            "/api/users/kakao/webview-login", //이원웅 추가
            "/api/users/kakao/callback", //이원웅 추가
            "/api/users/kakao/logout", //이원웅 추가
            "/api/users/kakao/logout/callback" //이원웅 추가
            "/api/payments/stripe/webhook"
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getServletPath();

        // 제외할 경로인지 확인
        boolean shouldExclude = EXCLUDE_PATHS.stream()
                .anyMatch(excludePath -> path.startsWith(excludePath));

        if (shouldExclude) {
            log.debug("JWT 필터 제외 경로: {}", path);
        }

        return shouldExclude;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        log.debug("JWT 필터 처리 시작: {}", requestURI);

        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt)) {
                log.debug("JWT 토큰 발견");

                if (jwtUtil.validateToken(jwt)) {
                    String userId = jwtUtil.getUserIdFromToken(jwt);
                    log.debug("JWT 토큰 유효함 - 사용자 ID: {}", userId);

                    UserDetails userDetails = userDetailsService.loadUserByUsername(userId);
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("SecurityContext에 인증 정보 설정 완료 - 사용자: {}", userId);
                } else {
                    log.debug("JWT 토큰이 유효하지 않음");
                }
            } else {
                log.debug("JWT 토큰이 없거나 Bearer 형식이 아님");
            }
        } catch (Exception ex) {
            log.error("JWT 인증 필터에서 오류 발생: {}", ex.getMessage());
            // 오류가 발생해도 계속 진행 (401은 EntryPoint에서 처리)
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (StringUtils.hasText(bearerToken)) {
            log.debug("Authorization 헤더: {}", bearerToken);

            if (bearerToken.startsWith("Bearer ")) {
                return bearerToken.substring(7);
            } else {
                log.debug("Bearer 토큰 형식이 아님");
            }
        } else {
            log.debug("Authorization 헤더가 없음");
        }

        return null;
    }
}