package rto.intelfit.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import rto.intelfit.dto.NutritionGoalDto;
import rto.intelfit.security.CustomUserPrincipal;
import rto.intelfit.service.NutritionGoalService;

@Slf4j
@RestController
@RequestMapping("/api/nutrition-goals")
@RequiredArgsConstructor
@Tag(name = "Nutrition Goal API", description = "일일 영양 목표 관련 API")
@SecurityRequirement(name = "bearerAuth")
public class NutritionGoalController {

    private final NutritionGoalService nutritionGoalService;

    @Operation(summary = "영양 목표 설정",
            description = "사용자의 일일 영양 목표를 설정합니다 (자동 계산 또는 수동 입력)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "영양 목표 설정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @ApiResponse(responseCode = "401", description = "인증이 필요합니다")
    })
    @PostMapping
    public ResponseEntity<NutritionGoalDto.NutritionGoalSetResponse> setNutritionGoal(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @Valid @RequestBody NutritionGoalDto.NutritionGoalSetRequest request) {
        log.info("영양 목표 설정 요청 - 사용자 ID: {}, 목표 칼로리: {}",
                userPrincipal.getUserId(), request.getTargetCalories());

        NutritionGoalDto.NutritionGoalSetResponse response =
                nutritionGoalService.setNutritionGoal(userPrincipal, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "영양 목표 조회",
            description = "사용자의 일일 영양 목표를 조회합니다 (없으면 자동 생성)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "영양 목표 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증이 필요합니다")
    })
    @GetMapping
    public ResponseEntity<NutritionGoalDto.NutritionGoalResponse> getNutritionGoal(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        log.info("영양 목표 조회 요청 - 사용자 ID: {}", userPrincipal.getUserId());

        NutritionGoalDto.NutritionGoalResponse response =
                nutritionGoalService.getNutritionGoal(userPrincipal);
        return ResponseEntity.ok(response);
    }
}