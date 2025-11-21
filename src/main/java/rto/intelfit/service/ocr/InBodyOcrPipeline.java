package rto.intelfit.service.ocr;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import rto.intelfit.dto.InBodyOcrResult;
import rto.intelfit.service.GeminiVisionClient;


@Slf4j
@Component
@RequiredArgsConstructor
public class InBodyOcrPipeline {

    private final GeminiVisionClient geminiVisionClient;

    public PipelineResult execute(byte[] originalImageBytes) {
        InBodyOcrResult finalResult = geminiVisionClient.analyze(originalImageBytes);

        return PipelineResult.builder()
                .finalResult(finalResult)
                .build();
    }

    @Getter
    @Builder
    public static class PipelineResult {
        private final InBodyOcrResult finalResult;
    }
}
