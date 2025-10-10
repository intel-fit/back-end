package rto.intelfit.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${app.name:IntelFit}")
    private String appName;

    /**
     * 이메일 설정 유효성 검증
     */
    private void validateEmailConfig() {
        if (fromEmail == null || fromEmail.trim().isEmpty()) {
            log.error("이메일 발송 설정이 올바르지 않습니다. SPRING_MAIL_USERNAME 환경변수를 확인해주세요.");
            throw new IllegalStateException("이메일 발송 설정이 완료되지 않았습니다");
        }
    }

    /**
     * 이메일 도메인 검증 (Gmail, Naver만 허용)
     */
    private boolean isValidEmailDomain(String email) {
        if (email == null || !email.contains("@")) {
            return false;
        }
        String domain = email.substring(email.indexOf("@") + 1).toLowerCase();
        return domain.equals("gmail.com") || domain.equals("naver.com");
    }

    /**
     * 이메일 인증코드 발송
     */
    @Async
    public void sendVerificationCode(String toEmail, String verificationCode) {
        // 이메일 설정 검증
        validateEmailConfig();

        // 도메인 검증
        if (!isValidEmailDomain(toEmail)) {
            log.error("지원하지 않는 이메일 도메인 - 수신자: {}", toEmail);
            throw new IllegalArgumentException("Gmail 또는 Naver 이메일만 사용 가능합니다");
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(String.format("[%s] 이메일 인증코드", appName));

            String htmlContent = buildVerificationEmailContent(verificationCode);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("이메일 인증코드 발송 성공 - 수신자: {}", toEmail);

        } catch (MessagingException e) {
            log.error("이메일 인증코드 발송 실패 - 수신자: {}, 에러: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("이메일 발송에 실패했습니다", e);
        }
    }

    /**
     * 임시 비밀번호 발송
     */
    @Async
    public void sendTempPassword(String toEmail, String tempPassword) {
        // 이메일 설정 검증
        validateEmailConfig();

        // 도메인 검증
        if (!isValidEmailDomain(toEmail)) {
            log.error("지원하지 않는 이메일 도메인 - 수신자: {}", toEmail);
            throw new IllegalArgumentException("Gmail 또는 Naver 이메일만 사용 가능합니다");
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(String.format("[%s] 임시 비밀번호 발송", appName));

            String htmlContent = buildTempPasswordEmailContent(tempPassword);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("임시 비밀번호 발송 성공 - 수신자: {}", toEmail);

        } catch (MessagingException e) {
            log.error("임시 비밀번호 발송 실패 - 수신자: {}, 에러: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("이메일 발송에 실패했습니다", e);
        }
    }

    /**
     * 아이디 찾기 이메일 발송
     */
    @Async
    public void sendUserId(String toEmail, String userId) {
        // 이메일 설정 검증
        validateEmailConfig();

        // 도메인 검증
        if (!isValidEmailDomain(toEmail)) {
            log.error("지원하지 않는 이메일 도메인 - 수신자: {}", toEmail);
            throw new IllegalArgumentException("Gmail 또는 Naver 이메일만 사용 가능합니다");
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(String.format("[%s] 아이디 찾기 결과", appName));

            String htmlContent = buildUserIdEmailContent(userId);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("아이디 찾기 이메일 발송 성공 - 수신자: {}", toEmail);

        } catch (MessagingException e) {
            log.error("아이디 찾기 이메일 발송 실패 - 수신자: {}, 에러: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("이메일 발송에 실패했습니다", e);
        }
    }

    /**
     * 인증코드 이메일 HTML 템플릿
     */
    private String buildVerificationEmailContent(String verificationCode) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body {
                        font-family: 'Malgun Gothic', Arial, sans-serif;
                        line-height: 1.6;
                        color: #333;
                    }
                    .container {
                        max-width: 600px;
                        margin: 0 auto;
                        padding: 20px;
                        background-color: #f9f9f9;
                    }
                    .content {
                        background-color: white;
                        padding: 30px;
                        border-radius: 10px;
                        box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                    }
                    .header {
                        text-align: center;
                        margin-bottom: 30px;
                    }
                    .code-box {
                        background-color: #f0f8ff;
                        border: 2px solid #4CAF50;
                        border-radius: 5px;
                        padding: 20px;
                        text-align: center;
                        margin: 30px 0;
                    }
                    .code {
                        font-size: 32px;
                        font-weight: bold;
                        color: #4CAF50;
                        letter-spacing: 5px;
                    }
                    .info {
                        color: #666;
                        font-size: 14px;
                        margin-top: 20px;
                    }
                    .footer {
                        text-align: center;
                        margin-top: 30px;
                        color: #999;
                        font-size: 12px;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="content">
                        <div class="header">
                            <h1>%s 이메일 인증</h1>
                        </div>
                        <p>안녕하세요,</p>
                        <p>회원가입을 위한 이메일 인증코드입니다.</p>
                        <p>아래의 인증코드를 입력하여 회원가입을 완료해주세요.</p>
                        
                        <div class="code-box">
                            <div class="code">%s</div>
                        </div>
                        
                        <div class="info">
                            <p>※ 본 인증코드는 5분간 유효합니다.</p>
                            <p>※ 본인이 요청하지 않은 경우, 이 메일을 무시하셔도 됩니다.</p>
                        </div>
                        
                        <div class="footer">
                            <p>본 메일은 발신 전용입니다.</p>
                            <p>&copy; 2024 %s. All rights reserved.</p>
                        </div>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(appName, verificationCode, appName);
    }

    /**
     * 임시 비밀번호 이메일 HTML 템플릿
     */
    private String buildTempPasswordEmailContent(String tempPassword) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body {
                        font-family: 'Malgun Gothic', Arial, sans-serif;
                        line-height: 1.6;
                        color: #333;
                    }
                    .container {
                        max-width: 600px;
                        margin: 0 auto;
                        padding: 20px;
                        background-color: #f9f9f9;
                    }
                    .content {
                        background-color: white;
                        padding: 30px;
                        border-radius: 10px;
                        box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                    }
                    .header {
                        text-align: center;
                        margin-bottom: 30px;
                    }
                    .password-box {
                        background-color: #fff3cd;
                        border: 2px solid #ff9800;
                        border-radius: 5px;
                        padding: 20px;
                        text-align: center;
                        margin: 30px 0;
                    }
                    .password {
                        font-size: 32px;
                        font-weight: bold;
                        color: #ff9800;
                        letter-spacing: 5px;
                    }
                    .warning {
                        background-color: #ffebee;
                        border-left: 4px solid #f44336;
                        padding: 15px;
                        margin: 20px 0;
                    }
                    .info {
                        color: #666;
                        font-size: 14px;
                        margin-top: 20px;
                    }
                    .footer {
                        text-align: center;
                        margin-top: 30px;
                        color: #999;
                        font-size: 12px;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="content">
                        <div class="header">
                            <h1>%s 임시 비밀번호 발급</h1>
                        </div>
                        <p>안녕하세요,</p>
                        <p>요청하신 임시 비밀번호가 발급되었습니다.</p>
                        
                        <div class="password-box">
                            <div class="password">%s</div>
                        </div>
                        
                        <div class="warning">
                            <strong>⚠️ 보안 안내</strong>
                            <p>로그인 후 반드시 비밀번호를 변경해주세요.</p>
                        </div>
                        
                        <div class="info">
                            <p>※ 본 임시 비밀번호는 30분간 유효합니다.</p>
                            <p>※ 로그인 후 즉시 새로운 비밀번호로 변경하시기 바랍니다.</p>
                            <p>※ 본인이 요청하지 않은 경우, 즉시 고객센터로 문의해주세요.</p>
                        </div>
                        
                        <div class="footer">
                            <p>본 메일은 발신 전용입니다.</p>
                            <p>&copy; 2024 %s. All rights reserved.</p>
                        </div>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(appName, tempPassword, appName);
    }

    /**
     * 아이디 찾기 이메일 HTML 템플릿
     */
    private String buildUserIdEmailContent(String userId) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body {
                        font-family: 'Malgun Gothic', Arial, sans-serif;
                        line-height: 1.6;
                        color: #333;
                    }
                    .container {
                        max-width: 600px;
                        margin: 0 auto;
                        padding: 20px;
                        background-color: #f9f9f9;
                    }
                    .content {
                        background-color: white;
                        padding: 30px;
                        border-radius: 10px;
                        box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                    }
                    .header {
                        text-align: center;
                        margin-bottom: 30px;
                    }
                    .userid-box {
                        background-color: #e8f5e9;
                        border: 2px solid #4CAF50;
                        border-radius: 5px;
                        padding: 20px;
                        text-align: center;
                        margin: 30px 0;
                    }
                    .userid {
                        font-size: 24px;
                        font-weight: bold;
                        color: #4CAF50;
                    }
                    .info {
                        color: #666;
                        font-size: 14px;
                        margin-top: 20px;
                    }
                    .footer {
                        text-align: center;
                        margin-top: 30px;
                        color: #999;
                        font-size: 12px;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="content">
                        <div class="header">
                            <h1>%s 아이디 찾기</h1>
                        </div>
                        <p>안녕하세요,</p>
                        <p>요청하신 아이디 찾기 결과입니다.</p>
                        
                        <div class="userid-box">
                            <p>회원님의 아이디는</p>
                            <div class="userid">%s</div>
                            <p>입니다.</p>
                        </div>
                        
                        <div class="info">
                            <p>※ 본인이 요청하지 않은 경우, 즉시 고객센터로 문의해주세요.</p>
                        </div>
                        
                        <div class="footer">
                            <p>본 메일은 발신 전용입니다.</p>
                            <p>&copy; 2024 %s. All rights reserved.</p>
                        </div>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(appName, userId, appName);
    }
}