package rto.intelfit.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import rto.intelfit.domain.UserFoodPreference;
import rto.intelfit.dto.UserFoodPreferenceDto;
import rto.intelfit.security.CustomUserPrincipal;
import rto.intelfit.service.UserFoodPreferenceService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/food-preferences")
@RequiredArgsConstructor
@Tag(name = "Food Preference API", description = "사용자 선호 음식 관련 API")
@SecurityRequirement(name = "bearerAuth")
public class UserFoodPreferenceController {

    private final UserFoodPreferenceService preferenceService;

    @Operation(summary = "선호 음식 추가",
            description = "사용자의 선호 음식을 추가합니다 (좋아요/싫어요/즐겨찾기)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "선호 음식 추가 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @ApiResponse(responseCode = "401", description = "인증이 필요합니다"),
            @ApiResponse(responseCode = "409", description = "이미 등록된 음식입니다")
    })
    @PostMapping
    public ResponseEntity<UserFoodPreferenceDto.FoodPreferenceResponse> addFoodPreference(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @Valid @RequestBody UserFoodPreferenceDto.FoodPreferenceAddRequest request) {
        log.info("선호 음식 추가 요청 - 사용자 ID: {}, 음식: {}",
                userPrincipal.getUserId(), request.getFoodName());

        UserFoodPreferenceDto.FoodPreferenceResponse response =
                preferenceService.addFoodPreference(userPrincipal, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "선호 음식 수정",
            description = "등록된 선호 음식의 정보를 수정합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "선호 음식 수정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @ApiResponse(responseCode = "401", description = "인증이 필요합니다"),
            @ApiResponse(responseCode = "403", description = "본인의 선호 음식만 수정할 수 있습니다"),
            @ApiResponse(responseCode = "404", description = "선호 음식을 찾을 수 없습니다")
    })
    @PutMapping("/{preferenceId}")
    public ResponseEntity<UserFoodPreferenceDto.FoodPreferenceResponse> updateFoodPreference(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @Parameter(description = "선호 음식 ID", example = "1")
            @PathVariable Long preferenceId,
            @Valid @RequestBody UserFoodPreferenceDto.FoodPreferenceUpdateRequest request) {
        log.info("선호 음식 수정 요청 - 사용자 ID: {}, 선호 ID: {}",
                userPrincipal.getUserId(), preferenceId);

        UserFoodPreferenceDto.FoodPreferenceResponse response =
                preferenceService.updateFoodPreference(userPrincipal, preferenceId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "선호 음식 삭제",
            description = "등록된 선호 음식을 삭제합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "선호 음식 삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증이 필요합니다"),
            @ApiResponse(responseCode = "403", description = "본인의 선호 음식만 삭제할 수 있습니다"),
            @ApiResponse(responseCode = "404", description = "선호 음식을 찾을 수 없습니다")
    })
    @DeleteMapping("/{preferenceId}")
    public ResponseEntity<UserFoodPreferenceDto.FoodPreferenceDeleteResponse> deleteFoodPreference(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @Parameter(description = "선호 음식 ID", example = "1")
            @PathVariable Long preferenceId) {
        log.info("선호 음식 삭제 요청 - 사용자 ID: {}, 선호 ID: {}",
                userPrincipal.getUserId(), preferenceId);

        UserFoodPreferenceDto.FoodPreferenceDeleteResponse response =
                preferenceService.deleteFoodPreference(userPrincipal, preferenceId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "모든 선호 음식 조회",
            description = "사용자의 모든 선호 음식을 조회합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "선호 음식 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증이 필요합니다")
    })
    @GetMapping
    public ResponseEntity<UserFoodPreferenceDto.FoodPreferenceListResponse> getAllFoodPreferences(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        log.info("선호 음식 목록 조회 요청 - 사용자 ID: {}", userPrincipal.getUserId());

        UserFoodPreferenceDto.FoodPreferenceListResponse response =
                preferenceService.getAllFoodPreferences(userPrincipal);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "선호도 타입별 음식 조회",
            description = "특정 선호도 타입의 음식을 조회합니다 (LIKE, DISLIKE, FAVORITE, NEUTRAL)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "선호 음식 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증이 필요합니다")
    })
    @GetMapping("/by-type")
    public ResponseEntity<UserFoodPreferenceDto.FoodPreferenceListResponse> getFoodPreferencesByType(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @Parameter(description = "선호도 타입", example = "LIKE")
            @RequestParam UserFoodPreference.PreferenceType type) {
        log.info("선호도 타입별 음식 조회 요청 - 사용자 ID: {}, 타입: {}",
                userPrincipal.getUserId(), type);

        UserFoodPreferenceDto.FoodPreferenceListResponse response =
                preferenceService.getFoodPreferencesByType(userPrincipal, type);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "좋아하는 음식 조회",
            description = "사용자가 좋아하는 음식 목록을 조회합니다 (LIKE + FAVORITE)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "좋아하는 음식 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증이 필요합니다")
    })
    @GetMapping("/liked")
    public ResponseEntity<UserFoodPreferenceDto.FoodPreferenceListResponse> getLikedFoods(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        log.info("좋아하는 음식 조회 요청 - 사용자 ID: {}", userPrincipal.getUserId());

        UserFoodPreferenceDto.FoodPreferenceListResponse response =
                preferenceService.getLikedFoods(userPrincipal);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "싫어하는 음식 조회",
            description = "사용자가 싫어하는 음식 목록을 조회합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "싫어하는 음식 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증이 필요합니다")
    })
    @GetMapping("/disliked")
    public ResponseEntity<UserFoodPreferenceDto.FoodPreferenceListResponse> getDislikedFoods(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        log.info("싫어하는 음식 조회 요청 - 사용자 ID: {}", userPrincipal.getUserId());

        UserFoodPreferenceDto.FoodPreferenceListResponse response =
                preferenceService.getDislikedFoods(userPrincipal);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "자주 먹는 음식 조회",
            description = "사용자가 자주 먹는 음식을 섭취 횟수 기준으로 조회합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "자주 먹는 음식 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증이 필요합니다")
    })
    @GetMapping("/frequent")
    public ResponseEntity<List<UserFoodPreferenceDto.FrequentFoodResponse>> getFrequentlyConsumedFoods(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        log.info("자주 먹는 음식 조회 요청 - 사용자 ID: {}", userPrincipal.getUserId());

        List<UserFoodPreferenceDto.FrequentFoodResponse> response =
                preferenceService.getFrequentlyConsumedFoods(userPrincipal);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "AI 추천용 선호 데이터 조회",
            description = "AI 식단 추천을 위한 사용자 선호 데이터를 조회합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "선호 데이터 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증이 필요합니다")
    })
    @GetMapping("/for-ai")
    public ResponseEntity<UserFoodPreferenceDto.PreferenceDataForAI> getPreferenceDataForAI(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        log.info("AI 추천용 선호 데이터 조회 요청 - 사용자 ID: {}", userPrincipal.getUserId());

        UserFoodPreferenceDto.PreferenceDataForAI response =
                preferenceService.getPreferenceDataForAI(userPrincipal);
        return ResponseEntity.ok(response);
    }
}