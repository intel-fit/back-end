package rto.intelfit.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import rto.intelfit.security.JwtAuthenticationFilter;
import rto.intelfit.security.JwtAuthenticationEntryPoint;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception.authenticationEntryPoint(jwtAuthenticationEntryPoint))
                .authorizeHttpRequests(authz -> authz
                        // ==================== 인증 없이 접근 가능한 API ====================
                        // 사용자 인증 관련 API
                        .requestMatchers(
                                "/api/users/signup",
                                "/api/users/login",
                                "/api/users/check-userId",
                                "/api/users/send-verification-code",
                                "/api/users/find-userId",
                                "/api/users/reset-password",
                                "/api/users/change-password",
                                "/api/users/refresh-token"
                        ).permitAll()

                        // Swagger UI 및 API 문서
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()

                        // Actuator Health check
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()

                        // 기타 공개 페이지
                        .requestMatchers("/", "/error").permitAll()

                        // ==================== 인증이 필요한 API ====================
                        // 사용자 API
                        .requestMatchers("/api/users/logout").authenticated()

                        // 프로필 API
                        .requestMatchers("/api/profile/**").authenticated()

                        // 인바디 API
                        .requestMatchers("/api/inbody/**").authenticated()

                        // 식단 API
                        .requestMatchers("/api/meals/**").authenticated()

                        // 영양 목표 API
                        .requestMatchers("/api/nutrition-goals/**").authenticated()

                        // 추천 식단 API
                        .requestMatchers("/api/recommended-meals/**").authenticated()

                        // 선호 음식 API (신규)
                        .requestMatchers("/api/food-preferences/**").authenticated()

                        // 나머지는 인증 필요
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}