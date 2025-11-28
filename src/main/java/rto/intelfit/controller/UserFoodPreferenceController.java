package rto.intelfit.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import rto.intelfit.domain.User;
import rto.intelfit.dto.UserFoodPreferenceDto;
import rto.intelfit.security.CustomUserPrincipal;
import rto.intelfit.service.UserFoodPreferenceService;
import rto.intelfit.repository.UserRepository;
import rto.intelfit.exception.BusinessException;
import rto.intelfit.exception.ErrorCode;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user/preferences")
@Tag(name = "User Food Preferences", description = "선호/비선호 음식 관리 API")
public class UserFoodPreferenceController {

    private final UserRepository userRepository;
    private final UserFoodPreferenceService service;

    private User loadUser(CustomUserPrincipal principal) {
        return userRepository.findByUserId(principal.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    // -----------------------------
    // 1) 선호/비선호 전체 조회 API
    // -----------------------------
    @Operation(summary = "현재 유저의 선호/비선호 음식 조회")
    @GetMapping
    public ResponseEntity<UserFoodPreferenceDto.Response> getPreferences(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        User user = loadUser(principal);
        return ResponseEntity.ok(service.getPreferences(user));
    }

    // -----------------------------
    // 2) 비선호 음식 추가 API
    // -----------------------------
    @Operation(summary = "비선호 음식 추가")
    @PostMapping("/disliked")
    public ResponseEntity<String> addDislikedFood(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @RequestBody UserFoodPreferenceDto.AddDislikedFoodRequest request
    ) {
        User user = loadUser(principal);
        String[] foods = request.getFoodName().split(",");
        for (String f : foods) {
            service.addDislikedFood(user, f.trim());
        }
        return ResponseEntity.ok("비선호 음식이 추가되었습니다.");
    }
}
