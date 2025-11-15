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

    private final ImagePreprocessor imagePreprocessor;
    private final ImageQualityEvaluator qualityEvaluator;
    private final GeminiVisionClient geminiVisionClient;

    public PipelineResult execute(byte[] originalImageBytes) {
        ImageQualityEvaluator.QualityReport originalQuality = qualityEvaluator.evaluate(originalImageBytes);

        ImagePreprocessor.PreprocessingResult preprocessingResult =
                imagePreprocessor.preprocess(originalImageBytes, false);

        byte[] processedImage = preprocessingResult.getProcessedImage();
        ImageQualityEvaluator.QualityReport processedQuality = qualityEvaluator.evaluate(processedImage);

        if (qualityEvaluator.isLowQuality(originalQuality) || qualityEvaluator.isLowQuality(processedQuality)) {
            preprocessingResult = imagePreprocessor.preprocess(originalImageBytes, true);
            processedImage = preprocessingResult.getProcessedImage();
        }

        InBodyOcrResult finalResult = geminiVisionClient.analyze(
                processedImage,
                originalImageBytes,
                GeminiVisionClient.FocusTarget.PREPROCESSED_PRIMARY);

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
