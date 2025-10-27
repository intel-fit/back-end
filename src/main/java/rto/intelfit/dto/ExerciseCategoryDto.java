package rto.intelfit.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import rto.intelfit.domain.FitnessExerciseCategoryDB;

public class ExerciseCategoryDto {

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "운동 종목 응답 DTO")
    public static class Response {
        @Schema(description = "운동 고유 ID")
        private String externalId;

        @Schema(description = "운동명")
        private String name;

        @Schema(description = "운동 부위")
        private String bodyPart;

        @Schema(description = "타겟 근육")
        private String targetMuscle;

        @Schema(description = "보조 근육")
        private String secondaryMuscles;

        @Schema(description = "장비")
        private String equipment;

        @Schema(description = "이미지 URL")
        private String imageUrl;

        @Schema(description = "운동 설명")
        private String instructions;

        public static Response fromEntity(FitnessExerciseCategoryDB entity) {
            return Response.builder()
                    .externalId(entity.getExternalId())
                    .name(entity.getName())
                    .bodyPart(entity.getBodyPart())
                    .targetMuscle(entity.getTargetMuscle())
                    .secondaryMuscles(entity.getSecondaryMuscles())
                    .equipment(entity.getEquipment())
                    .imageUrl(entity.getImageUrl())
                    .instructions(entity.getInstructions())
                    .build();
        }
    }
}
