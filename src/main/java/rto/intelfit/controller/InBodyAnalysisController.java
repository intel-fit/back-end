package rto.intelfit.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rto.intelfit.dto.InBodyAnalysisDto;
import rto.intelfit.security.CustomUserPrincipal;
import rto.intelfit.domain.User;
import rto.intelfit.service.InBodyAnalysisService;
import rto.intelfit.repository.UserRepository;
import rto.intelfit.exception.BusinessException;
import rto.intelfit.exception.ErrorCode;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/inbody/analysis")
@Tag(name = "InBody Analysis API", description = "인바디 AI 분석 결과 API")
public class InBodyAnalysisController {

    private final InBodyAnalysisService inBodyAnalysisService;
    private final UserRepository userRepository;

    @GetMapping("/latest")
    @Operation(summary = "최신 인바디 AI 분석 결과 조회")
    public ResponseEntity<InBodyAnalysisDto.LatestAnalysisResponse> getLatestAnalysis(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {

        User user = userRepository.findByUserId(userPrincipal.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        log.info("최신 인바디 분석 결과 조회 - userId: {}", user.getUserId());

        InBodyAnalysisDto.LatestAnalysisResponse response = inBodyAnalysisService.getLatestAnalysis(user);
        return ResponseEntity.ok(response);
    }
}
