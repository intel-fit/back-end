package rto.intelfit.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import rto.intelfit.dto.InBodyDto;
import rto.intelfit.security.CustomUserPrincipal;
import rto.intelfit.service.InBodyService;

@Slf4j
@RestController
@RequestMapping("/api/inbody")
@RequiredArgsConstructor
@Tag(name = "InBody API", description = "인바디 관련 API")
@SecurityRequirement(name = "bearerAuth")
public class InBodyController {

    private final InBodyService inBodyService;

    /**
     * 1. 인바디 정보 등록
     */
    @Operation(summary = "인바디 정보 등록", description = "새로운 인바디 측정 기록을 등록합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "인바디 등록 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @ApiResponse(responseCode = "401", description = "인증이 필요합니다")
    })
    @PostMapping
    public ResponseEntity<InBodyDto.InBodyCreateResponse> createInBody(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @Valid @RequestBody InBodyDto.InBodyCreateRequest request) {
        log.info("인바디 등록 요청 - 사용자 ID: {}, 측정 날짜: {}",
                userPrincipal.getUserId(), request.getMeasurementDate());

        InBodyDto.InBodyCreateResponse response = inBodyService.createInBody(userPrincipal, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 1-1. 인바디 결과지 이미지 업로드
     */
    @Operation(summary = "인바디 결과지 업로드",
            description = "인바디 결과지 이미지를 업로드하면 Gemini가 수치를 추출해 초안 데이터를 반환합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "인바디 OCR 초안 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 이미지 파일"),
            @ApiResponse(responseCode = "401", description = "인증이 필요합니다")
    })
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<InBodyDto.InBodyOcrUploadResponse> uploadInBodyImage(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @RequestPart("file") MultipartFile file) {
        log.info("인바디 결과지 업로드 API 호출 - 사용자 ID: {}", userPrincipal.getUserId());
        InBodyDto.InBodyOcrUploadResponse response = inBodyService.uploadInBodyFromImage(userPrincipal, file);
        return ResponseEntity.ok(response);
    }

    /**
     * 2. 최신 인바디 기록 조회 (화면용)
     */
    @Operation(summary = "최신 인바디 기록 조회",
            description = "로그인한 사용자의 가장 최근 인바디 기록을 화면에 맞춰 조회합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "최신 인바디 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증이 필요합니다"),
            @ApiResponse(responseCode = "404", description = "인바디 기록이 없습니다")
    })
    @GetMapping("/latest")
    public ResponseEntity<InBodyDto.InBodyDetailResponse> getLatestInBody(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        log.info("최신 인바디 조회 요청 - 사용자 ID: {}", userPrincipal.getUserId());

        InBodyDto.InBodyDetailResponse response = inBodyService.getLatestInBody(userPrincipal);
        return ResponseEntity.ok(response);
    }

    /**
     * 3. 인바디 정보 수정
     */
    @Operation(summary = "인바디 정보 수정",
            description = "기존 인바디 측정 기록을 부분 수정합니다 (선택적 필드)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "인바디 수정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @ApiResponse(responseCode = "401", description = "인증이 필요합니다"),
            @ApiResponse(responseCode = "403", description = "본인의 기록만 수정할 수 있습니다"),
            @ApiResponse(responseCode = "404", description = "인바디 기록을 찾을 수 없습니다")
    })
    @PatchMapping("/{inBodyId}")
    public ResponseEntity<InBodyDto.InBodyUpdateResponse> updateInBody(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @PathVariable Long inBodyId,
            @Valid @RequestBody InBodyDto.InBodyUpdateRequest request) {
        log.info("인바디 수정 요청 - 사용자 ID: {}, 인바디 ID: {}",
                userPrincipal.getUserId(), inBodyId);

        InBodyDto.InBodyUpdateResponse response = inBodyService.updateInBody(userPrincipal, inBodyId, request);
        return ResponseEntity.ok(response);
    }
}
