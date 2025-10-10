package rto.intelfit;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class IntelfitApplication {

	public static void main(String[] args) {
		try {
			Dotenv dotenv = Dotenv.load();
			dotenv.entries().forEach(entry ->
					System.setProperty(entry.getKey(), entry.getValue())
			);
			System.out.println(".env 파일 로드 성공.");
		} catch (Exception e) {
			System.err.println("경고: .env 파일 로드 실패. 이메일 설정 누락 오류를 확인하세요.");
		}

		SpringApplication.run(IntelfitApplication.class, args);
	}
}