package rto.intelfit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import rto.intelfit.exception.BusinessException;
import rto.intelfit.exception.ErrorCode;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3StorageService {

    private static final DateTimeFormatter FILE_NAME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss", Locale.KOREA);

    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket:}")
    private String bucketName;

    @Value("${cloud.aws.s3.folder:inbody-records}")
    private String folderName;

    @Value("${cloud.aws.s3.base-url:}")
    private String customBaseUrl;

    public String uploadInBodyImage(String userId, MultipartFile file) {
        return uploadInBodyImageWithKey(userId, file).imageUrl();
    }

    public UploadResult uploadInBodyImageWithKey(String userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "업로드할 인바디 이미지가 필요합니다");
        }
        if (!StringUtils.hasText(bucketName)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "S3 버킷 이름이 설정되지 않았습니다");
        }

        String objectKey = buildObjectKey(userId, file.getOriginalFilename());

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));
            log.info("인바디 이미지 업로드 완료 - bucket: {}, key: {}", bucketName, objectKey);
            return new UploadResult(resolveFileUrl(objectKey), objectKey);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "업로드할 파일을 읽을 수 없습니다", e);
        } catch (S3Exception e) {
            log.error("S3 업로드 실패", e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "S3 업로드 중 오류가 발생했습니다", e);
        }
    }

    public byte[] downloadImage(String objectKey) {
        if (!StringUtils.hasText(objectKey)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "다운로드할 객체 키가 필요합니다");
        }

        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();

        try (ResponseInputStream<GetObjectResponse> response = s3Client.getObject(request)) {
            return response.readAllBytes();
        } catch (IOException | S3Exception e) {
            log.error("S3 다운로드 실패 - key: {}", objectKey, e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "S3에서 이미지를 다운로드할 수 없습니다", e);
        }
    }

    private String buildObjectKey(String userId, String originalFilename) {
        String cleanFileName = (originalFilename != null) ? originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_") : "inbody.png";
        String timestamp = FILE_NAME_FORMATTER.format(LocalDateTime.now());
        return String.format("%s/%s/%s_%s_%s",
                folderName.replaceAll("^/+", "").replaceAll("/+$", ""),
                userId,
                timestamp,
                UUID.randomUUID(),
                cleanFileName);
    }

    private String resolveFileUrl(String key) {
        if (StringUtils.hasText(customBaseUrl)) {
            String base = customBaseUrl.endsWith("/") ? customBaseUrl.substring(0, customBaseUrl.length() - 1) : customBaseUrl;
            return base + "/" + key;
        }

        try {
            return s3Client.utilities().getUrl(GetUrlRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build()).toExternalForm();
        } catch (S3Exception e) {
            log.warn("S3 URL 생성 실패 - {}", e.getMessage());
            return String.format("s3://%s/%s", bucketName, key);
        }
    }

    public record UploadResult(String imageUrl, String objectKey) {}
}
