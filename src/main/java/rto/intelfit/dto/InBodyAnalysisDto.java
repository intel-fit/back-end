package rto.intelfit.dto;

import lombok.Builder;
import lombok.Getter;
import rto.intelfit.domain.InBodyAnalysisResult;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class InBodyAnalysisDto {

    @Getter
    @Builder
    public static class LatestAnalysisResponse {
        private final boolean exists;
        private final LocalDate startDate;
        private final LocalDate endDate;
        private final String analysisText;
        private final LocalDateTime analyzedAt;

        public static LatestAnalysisResponse from(InBodyAnalysisResult result) {
            if (result == null) {
                return LatestAnalysisResponse.builder()
                        .exists(false)
                        .build();
            }
            return LatestAnalysisResponse.builder()
                    .exists(true)
                    .startDate(result.getStartDate())
                    .endDate(result.getEndDate())
                    .analysisText(result.getAnalysisText())
                    .analyzedAt(result.getCreatedAt())
                    .build();
        }
    }
}
