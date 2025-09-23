package rto.intelfit.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import rto.intelfit.exception.ErrorResponse;

import java.io.IOException;

@Slf4j
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        String requestPath = request.getServletPath();

        // Actuator 엔드포인트는 401 대신 200 반환 (헬스체크용)
        if (requestPath.startsWith("/actuator/")) {
            log.debug("Actuator 엔드포인트 접근: {}", requestPath);
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        log.error("인증되지 않은 요청: {} - {}", requestPath, authException.getMessage());

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding("UTF-8");

        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .code("JWT_001")
                .message("인증이 필요합니다")
                .build();

        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(response.getOutputStream(), errorResponse);
    }
}