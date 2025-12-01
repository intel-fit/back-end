package rto.intelfit.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "kakao")
public class KakaoOAuthConfig {
    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private String adminKey;

    public static final String KAKAO_AUTH_URL = "https://kauth.kakao.com";
    public static final String KAKAO_API_URL = "https://kapi.kakao.com";
}