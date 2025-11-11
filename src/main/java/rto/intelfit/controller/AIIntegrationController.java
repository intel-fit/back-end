package rto.intelfit.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import rto.intelfit.security.CustomUserPrincipal;
import rto.intelfit.service.AIServerClient;

import java.io.IOException;
import java.util.Map;

/**
 * AI 서버 통합 API
 * - FastAPI AI 서버의 기능을 Spring Boot를 통해 제공
 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Tag(name = "AI Integration", description = "AI 서버 통합 API")
public class AIIntegrationController {

    private final AIServerClient aiServerClient;

    // ========================================
    // 1️⃣ 식단 관련
    // ========================================

    @PostMapping("/meal/generate")
    @Operation(summary = "AI 기반 식단 추천")
    public ResponseEntity<Map<String, Object>> generateMealPlan(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @RequestBody Map<String, Object> request) {
        
        log.info("AI 식단 추천 요청 - userId: {}", userPrincipal.getUserId());
        
        // user_id 자동 주입
        request.put("user_id", String.valueOf(userPrincipal.getUserId()));
        
        Map<String, Object> result = aiServerClient.generateMealPlan(request);
        
        return ResponseEntity.ok(result);
    }

    @PostMapping(value = "/meal/upload-food", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "음식 이미지 AI 분석")
    public ResponseEntity<Map<String, Object>> uploadFoodImage(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @RequestParam("file") MultipartFile file) throws IOException {
        
        log.info("음식 이미지 업로드 - userId: {}, filename: {}", 
                userPrincipal.getUserId(), file.getOriginalFilename());
        
        Map<String, Object> result = aiServerClient.uploadFoodImage(file);
        
        return ResponseEntity.ok(result);
    }

    // ========================================
    // 2️⃣ 운동 관련
    // ========================================

    @PostMapping("/exercise/generate")
    @Operation(summary = "AI 기반 운동 루틴 추천")
    public ResponseEntity<Map<String, Object>> generateExercisePlan(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @RequestBody Map<String, Object> request) {
        
        log.info("AI 운동 루틴 추천 요청 - userId: {}", userPrincipal.getUserId());
        
        // user_id 자동 주입
        request.put("user_id", String.valueOf(userPrincipal.getUserId()));
        
        Map<String, Object> result = aiServerClient.generateExercisePlan(request);
        
        return ResponseEntity.ok(result);
    }

    @PostMapping("/exercise/log")
    @Operation(summary = "AI 운동 루틴 저장 (피드백용)")
    public ResponseEntity<Map<String, Object>> logExercisePlan(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @RequestBody Map<String, Object> request) {
        
        log.info("AI 운동 루틴 저장 - userId: {}", userPrincipal.getUserId());
        
        request.put("user_id", String.valueOf(userPrincipal.getUserId()));
        
        Map<String, Object> result = aiServerClient.logExercisePlan(request);
        
        return ResponseEntity.ok(result);
    }

    @PutMapping("/exercise/feedback/{recId}")
    @Operation(summary = "운동 피드백 업데이트")
    public ResponseEntity<Map<String, Object>> updateExerciseFeedback(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @PathVariable String recId,
            @RequestBody Map<String, Object> request) {
        
        log.info("운동 피드백 업데이트 - userId: {}, recId: {}", 
                userPrincipal.getUserId(), recId);
        
        Map<String, Object> result = aiServerClient.updateExerciseFeedback(recId, request);
        
        return ResponseEntity.ok(result);
    }

    @GetMapping("/exercise/score")
    @Operation(summary = "일일 운동 점수 조회")
    public ResponseEntity<Map<String, Object>> getDailyExerciseScore(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @RequestParam(required = false) String date) {
        
        log.info("일일 운동 점수 조회 - userId: {}, date: {}", 
                userPrincipal.getUserId(), date);
        
        Map<String, Object> result = aiServerClient.getDailyExerciseScore(
                String.valueOf(userPrincipal.getUserId()), date);
        
        return ResponseEntity.ok(result);
    }

    // ========================================
    // 3️⃣ 건강 점수
    // ========================================

    @GetMapping("/health-score/daily")
    @Operation(summary = "일일 건강 점수 조회")
    public ResponseEntity<Map<String, Object>> getDailyHealthScore(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @RequestParam(required = false) String date) {
        
        log.info("일일 건강 점수 조회 - userId: {}, date: {}", 
                userPrincipal.getUserId(), date);
        
        Map<String, Object> result = aiServerClient.getDailyHealthScore(
                String.valueOf(userPrincipal.getUserId()), date);
        
        return ResponseEntity.ok(result);
    }

    @GetMapping("/health-score/weekly")
    @Operation(summary = "주간 건강 점수 조회")
    public ResponseEntity<Map<String, Object>> getWeeklyHealthScore(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        
        log.info("주간 건강 점수 조회 - userId: {}", userPrincipal.getUserId());
        
        Map<String, Object> result = aiServerClient.getWeeklyHealthScore(
                String.valueOf(userPrincipal.getUserId()));
        
        return ResponseEntity.ok(result);
    }

    @GetMapping("/health-score/monthly")
    @Operation(summary = "월간 건강 점수 조회")
    public ResponseEntity<Map<String, Object>> getMonthlyHealthScore(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @RequestParam String yearMonth) {
        
        log.info("월간 건강 점수 조회 - userId: {}, yearMonth: {}", 
                userPrincipal.getUserId(), yearMonth);
        
        Map<String, Object> result = aiServerClient.getMonthlyHealthScore(
                String.valueOf(userPrincipal.getUserId()), yearMonth);
        
        return ResponseEntity.ok(result);
    }

    @GetMapping("/health-score/trend")
    @Operation(summary = "건강 점수 트렌드 조회")
    public ResponseEntity<Map<String, Object>> getScoreTrend(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @RequestParam(defaultValue = "30") int days) {
        
        log.info("건강 점수 트렌드 조회 - userId: {}, days: {}", 
                userPrincipal.getUserId(), days);
        
        Map<String, Object> result = aiServerClient.getScoreTrend(
                String.valueOf(userPrincipal.getUserId()), days);
        
        return ResponseEntity.ok(result);
    }

    // ========================================
    // 4️⃣ 분석/통계
    // ========================================

    @GetMapping("/analytics/daily")
    @Operation(summary = "일일 요약 조회")
    public ResponseEntity<Map<String, Object>> getDailySummary(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        
        log.info("일일 요약 조회 - userId: {}", userPrincipal.getUserId());
        
        Map<String, Object> result = aiServerClient.getDailySummary(
                String.valueOf(userPrincipal.getUserId()));
        
        return ResponseEntity.ok(result);
    }

    @GetMapping(value = "/analytics/weekly/image", produces = MediaType.IMAGE_PNG_VALUE)
    @Operation(summary = "주간 트렌드 그래프 이미지")
    public ResponseEntity<byte[]> getWeeklyTrendImage(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        
        log.info("주간 트렌드 이미지 조회 - userId: {}", userPrincipal.getUserId());
        
        byte[] image = aiServerClient.getWeeklyTrendImage(String.valueOf(userPrincipal.getUserId()));
        
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(image);
    }

    @GetMapping("/analytics/monthly")
    @Operation(summary = "월간 통계 요약")
    public ResponseEntity<Map<String, Object>> getMonthlyAverage(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        
        log.info("월간 통계 요약 조회 - userId: {}", userPrincipal.getUserId());
        
        Map<String, Object> result = aiServerClient.getMonthlyAverage(
                String.valueOf(userPrincipal.getUserId()));
        
        return ResponseEntity.ok(result);
    }

    // ========================================
    // 5️⃣ AI 코치
    // ========================================

    @GetMapping("/coach/weekly-report")
    @Operation(summary = "주간 코치 리포트")
    public ResponseEntity<Map<String, Object>> getWeeklyCoachReport(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        
        log.info("주간 코치 리포트 조회 - userId: {}", userPrincipal.getUserId());
        
        Map<String, Object> result = aiServerClient.getWeeklyCoachReport(
                String.valueOf(userPrincipal.getUserId()));
        
        return ResponseEntity.ok(result);
    }

    @PostMapping("/coach/chat")
    @Operation(summary = "AI 코치 챗봇")
    public ResponseEntity<Map<String, Object>> chatWithCoach(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @RequestBody Map<String, String> request) {
        
        String message = request.get("message");
        
        log.info("AI 코치 챗봇 - userId: {}, message: {}", 
                userPrincipal.getUserId(), message);
        
        Map<String, Object> result = aiServerClient.chatWithCoach(
                String.valueOf(userPrincipal.getUserId()), message);
        
        return ResponseEntity.ok(result);
    }
}
