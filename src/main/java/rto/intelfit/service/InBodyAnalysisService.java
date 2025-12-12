package rto.intelfit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import rto.intelfit.domain.InBody;
import rto.intelfit.domain.InBodyAnalysisResult;
import rto.intelfit.domain.User;
import rto.intelfit.dto.InBodyAnalysisDto;
import rto.intelfit.repository.InBodyAnalysisResultRepository;
import rto.intelfit.repository.InBodyRepository;
import rto.intelfit.service.AIServerClient;

import java.time.LocalDate;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InBodyAnalysisService {

    private final InBodyAnalysisResultRepository inBodyAnalysisResultRepository;
    private final InBodyRepository inBodyRepository;
    private final AIServerClient aiServerClient;

    @Transactional
    public void saveAnalysisResult(User user, LocalDate startDate, LocalDate endDate, String analysisText) {
        if (user == null || startDate == null || endDate == null || !StringUtils.hasText(analysisText)) {
            log.warn("인바디 분석 저장 스킵 - user: {}, start: {}, end: {}, text: {}",
                    user != null ? user.getUserId() : null, startDate, endDate,
                    StringUtils.trimWhitespace(analysisText));
            return;
        }

        InBodyAnalysisResult result = InBodyAnalysisResult.builder()
                .user(user)
                .startDate(startDate)
                .endDate(endDate)
                .analysisText(analysisText)
                .build();

        inBodyAnalysisResultRepository.save(result);
        log.info("인바디 분석 저장 완료 - userId: {}, 기간: {}~{}", user.getUserId(), startDate, endDate);
    }

    @Transactional
    public InBodyAnalysisDto.LatestAnalysisResponse getLatestAnalysis(User user) {
        Optional<InBodyAnalysisResult> latestAnalysis = inBodyAnalysisResultRepository.findTopByUserOrderByCreatedAtDesc(user);

        if (shouldTriggerHistoricalAnalysis(user, latestAnalysis)) {
            latestAnalysis = requestHistoricalAnalysis(user);
        }

        return latestAnalysis
                .map(InBodyAnalysisDto.LatestAnalysisResponse::from)
                .orElseGet(() -> InBodyAnalysisDto.LatestAnalysisResponse.builder()
                        .exists(false)
                        .build());
    }

    private boolean shouldTriggerHistoricalAnalysis(User user, Optional<InBodyAnalysisResult> latestAnalysis) {
        Optional<InBody> latestInBody = inBodyRepository.findFirstByUserOrderByMeasurementDateDesc(user);
        if (latestInBody.isEmpty()) {
            return false;
        }

        if (latestAnalysis.isEmpty()) {
            return true;
        }

        LocalDate latestMeasurementDate = latestInBody.get().getMeasurementDate();
        return latestMeasurementDate.isAfter(latestAnalysis.get().getEndDate());
    }

    private Optional<InBodyAnalysisResult> requestHistoricalAnalysis(User user) {
        log.info("🔥 AI 인바디 분석 요청 시작 - userId={}", user.getUserId());
        Optional<InBody> latestInBodyOpt = inBodyRepository.findFirstByUserOrderByMeasurementDateDesc(user);
        if (latestInBodyOpt.isEmpty()) {
            return Optional.empty();
        }

        LocalDate endDate = latestInBodyOpt.get().getMeasurementDate();
        LocalDate startDate = inBodyRepository.findFirstByUserOrderByMeasurementDateAsc(user)
                .map(InBody::getMeasurementDate)
                .orElse(endDate);

        try {
            log.info("🔥 AI 인바디 분석 호출 준비 - userId={}, 기간: {}~{}", user.getUserId(), startDate, endDate);
            String analysisText = aiServerClient.requestInBodyPeriodAnalysis(
                    user.getUserId(), startDate, endDate);
            saveAnalysisResult(user, startDate, endDate, analysisText);
            return inBodyAnalysisResultRepository.findTopByUserOrderByCreatedAtDesc(user);
        } catch (Exception e) {
            log.error("기존 인바디 분석 요청 실패 - userId: {}, 기간: {}~{}, 오류: {}",
                    user.getUserId(), startDate, endDate, e.getMessage(), e);
            return Optional.empty();
        }
    }

}
