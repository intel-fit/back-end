package rto.intelfit.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import rto.intelfit.domain.InBody;
import rto.intelfit.domain.User;
//import
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;

public class InBodyDto {


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "인바디 등록 요청")
    public static class InBodyCreateRequest {

        @Schema(description = "측정 날짜", example = "2025-08-04")
        @NotNull(message = "측정 날짜를 입력해주세요")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate measurementDate;

        @Schema(description = "체중 (kg)", example = "58.8")
        @NotNull(message = "체중을 입력해주세요")
        @DecimalMin(value = "30.0", message = "체중은 30kg 이상이어야 합니다")
        @DecimalMax(value = "200.0", message = "체중은 200kg 이하여야 합니다")
        private BigDecimal weight;

        @Schema(description = "근육량 (kg)", example = "30.4")
        private BigDecimal muscleMass;

        @Schema(description = "체지방량 (kg)", example = "17.3")
        private BigDecimal bodyFatMass;

        @Schema(description = "골격근량 (kg)", example = "22.9")
        private BigDecimal skeletalMuscleMass;

        @Schema(description = "체지방률 (%)", example = "29.4")
        @DecimalMin(value = "0.0", message = "체지방률은 0 이상이어야 합니다")
        @DecimalMax(value = "100.0", message = "체지방률은 100 이하여야 합니다")
        private BigDecimal bodyFatPercentage;

        // 부위별 근육량
        @Schema(description = "좌측 팔 근육량 (kg)", example = "2.5")
        private BigDecimal leftArmMuscle;

        @Schema(description = "우측 팔 근육량 (kg)", example = "2.6")
        private BigDecimal rightArmMuscle;

        @Schema(description = "몸통 근육량 (kg)", example = "23.5")
        private BigDecimal trunkMuscle;

        @Schema(description = "좌측 다리 근육량 (kg)", example = "8.2")
        private BigDecimal leftLegMuscle;

        @Schema(description = "우측 다리 근육량 (kg)", example = "8.3")
        private BigDecimal rightLegMuscle;

        // 부위별 지방량
        @Schema(description = "좌측 팔 지방량 (kg)", example = "1.2")
        private BigDecimal leftArmFat;

        @Schema(description = "우측 팔 지방량 (kg)", example = "1.3")
        private BigDecimal rightArmFat;

        @Schema(description = "몸통 지방량 (kg)", example = "10.5")
        private BigDecimal trunkFat;

        @Schema(description = "좌측 다리 지방량 (kg)", example = "3.5")
        private BigDecimal leftLegFat;

        @Schema(description = "우측 다리 지방량 (kg)", example = "3.6")
        private BigDecimal rightLegFat;

        // 체수분/단백질/무기질
        @Schema(description = "체수분 (L)", example = "30.4")
        private BigDecimal totalBodyWater;

        @Schema(description = "단백질 (kg)", example = "8.2")
        private BigDecimal protein;

        @Schema(description = "무기질 (kg)", example = "2.89")
        private BigDecimal mineral;

        // BMI 및 기타
        @Schema(description = "BMI", example = "22.4")
        private BigDecimal bmi;

        @Schema(description = "체지방률 기준", example = "20.5")
        private BigDecimal bodyFatPercentageStandard;

        @Schema(description = "비만도", example = "95.5")
        private BigDecimal obesityDegree;

        @Schema(description = "내장지방 레벨", example = "6")
        private BigDecimal visceralFatLevel;

        @Schema(description = "기초대사량 (kcal)", example = "1600")
        @Min(value = 0, message = "기초대사량은 0 이상이어야 합니다")
        private Integer basalMetabolicRate;
    }

    // ==================== 수정 요청 ====================
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "인바디 수정 요청 (선택적 필드)")
    public static class InBodyUpdateRequest {

        @Schema(description = "측정 날짜", example = "2025-08-04")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate measurementDate;

        @Schema(description = "체중 (kg)", example = "58.8")
        @DecimalMin(value = "30.0", message = "체중은 30kg 이상이어야 합니다")
        @DecimalMax(value = "200.0", message = "체중은 200kg 이하여야 합니다")
        private BigDecimal weight;

        @Schema(description = "근육량 (kg)", example = "30.4")
        private BigDecimal muscleMass;

        @Schema(description = "체지방량 (kg)", example = "17.3")
        private BigDecimal bodyFatMass;

        @Schema(description = "골격근량 (kg)", example = "22.9")
        private BigDecimal skeletalMuscleMass;

        @Schema(description = "체지방률 (%)", example = "29.4")
        @DecimalMin(value = "0.0", message = "체지방률은 0 이상이어야 합니다")
        @DecimalMax(value = "100.0", message = "체지방률은 100 이하여야 합니다")
        private BigDecimal bodyFatPercentage;

        @Schema(description = "좌측 팔 근육량 (kg)", example = "2.5")
        private BigDecimal leftArmMuscle;

        @Schema(description = "우측 팔 근육량 (kg)", example = "2.6")
        private BigDecimal rightArmMuscle;

        @Schema(description = "몸통 근육량 (kg)", example = "23.5")
        private BigDecimal trunkMuscle;

        @Schema(description = "좌측 다리 근육량 (kg)", example = "8.2")
        private BigDecimal leftLegMuscle;

        @Schema(description = "우측 다리 근육량 (kg)", example = "8.3")
        private BigDecimal rightLegMuscle;

        @Schema(description = "좌측 팔 지방량 (kg)", example = "1.2")
        private BigDecimal leftArmFat;

        @Schema(description = "우측 팔 지방량 (kg)", example = "1.3")
        private BigDecimal rightArmFat;

        @Schema(description = "몸통 지방량 (kg)", example = "10.5")
        private BigDecimal trunkFat;

        @Schema(description = "좌측 다리 지방량 (kg)", example = "3.5")
        private BigDecimal leftLegFat;

        @Schema(description = "우측 다리 지방량 (kg)", example = "3.6")
        private BigDecimal rightLegFat;

        @Schema(description = "체수분 (L)", example = "30.4")
        private BigDecimal totalBodyWater;

        @Schema(description = "단백질 (kg)", example = "8.2")
        private BigDecimal protein;

        @Schema(description = "무기질 (kg)", example = "2.89")
        private BigDecimal mineral;

        @Schema(description = "BMI", example = "22.4")
        private BigDecimal bmi;

        @Schema(description = "체지방률 기준", example = "20.5")
        private BigDecimal bodyFatPercentageStandard;

        @Schema(description = "비만도", example = "95.5")
        private BigDecimal obesityDegree;

        @Schema(description = "내장지방 레벨", example = "6")
        private BigDecimal visceralFatLevel;

        @Schema(description = "기초대사량 (kcal)", example = "1600")
        @Min(value = 0, message = "기초대사량은 0 이상이어야 합니다")
        private Integer basalMetabolicRate;

        @Schema(description = "달성 뱃지", example = "GOLD", allowableValues = {"GOLD", "SILVER", "BRONZE", "NONE"})
        private InBody.AchievementBadge achievementBadge;
    }

    // ==================== 상세 응답 (화면 맞춤) ====================
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "인바디 상세 정보 응답")
    public static class InBodyDetailResponse {

        @Schema(description = "인바디 기록 ID", example = "1")
        private Long id;

        @Schema(description = "인바디 점수", example = "74")
        private Integer score;

        @Schema(description = "측정 날짜", example = "2025.08.04")
        private String measurementDate;

        @Schema(description = "나이", example = "29세")
        private String age;

        @Schema(description = "키", example = "162cm")
        private String height;

        @Schema(description = "성별", example = "여성")
        private String gender;

        @Schema(description = "체성분 분석")
        private BodyComposition bodyComposition;

        @Schema(description = "골격근/지방 분석")
        private MuscleFatAnalysis muscleFatAnalysis;

        @Schema(description = "비만 분석")
        private ObesityAnalysis obesityAnalysis;

        @Schema(description = "체중 조절")
        private WeightControl weightControl;

        @Schema(description = "부위별 근육 분석")
        private SegmentalMuscleAnalysis segmentalMuscleAnalysis;

        @Schema(description = "부분별지방률", example = "0.86")
        private BigDecimal segmentalFatRatio;

        @Schema(description = "내장지방레벨", example = "6 (1~20)")
        private String visceralFatLevel;

        @Schema(description = "달성 뱃지", example = "GOLD")
        private InBody.AchievementBadge achievementBadge;

        @Schema(description = "생성 일시")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime createdAt;

        @Schema(description = "수정 일시")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime updatedAt;

        // ========== 내부 클래스들 ==========

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class BodyComposition {
            @Schema(description = "체수분", example = "30.4 ( 26.1 ~ 34.3 )")
            private String totalBodyWater;

            @Schema(description = "단백질", example = "8.2 ( 7.6 ~ 9.2 )")
            private String protein;

            @Schema(description = "무기질", example = "2.89 ( 2.60 ~ 3.18 )")
            private String mineral;

            @Schema(description = "체지방량", example = "17.3 ( 11.0 ~ 17.6 )")
            private String bodyFatMass;

            @Schema(description = "체중", example = "58.8 ( 46.8 ~ 63.4 )")
            private String weight;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class MuscleFatAnalysis {
            @Schema(description = "체중", example = "58.8")
            private BigDecimal weight;

            @Schema(description = "체중 상태", example = "표준")
            private String weightStatus;

            @Schema(description = "골격근량", example = "22.9")
            private BigDecimal skeletalMuscleMass;

            @Schema(description = "골격근량 상태", example = "표준")
            private String skeletalMuscleStatus;

            @Schema(description = "체지방량", example = "17.3")
            private BigDecimal bodyFatMass;

            @Schema(description = "체지방량 상태", example = "표준")
            private String bodyFatStatus;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class ObesityAnalysis {
            @Schema(description = "BMI", example = "22.4")
            private BigDecimal bmi;

            @Schema(description = "BMI 상태", example = "표준")
            private String bmiStatus;

            @Schema(description = "체지방률", example = "29.4")
            private BigDecimal bodyFatPercentage;

            @Schema(description = "체지방률 상태", example = "표준")
            private String bodyFatPercentageStatus;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class WeightControl {
            @Schema(description = "적정 체중", example = "55.1kg")
            private String targetWeight;

            @Schema(description = "체중 조절", example = "-3.7kg")
            private String weightAdjustment;

            @Schema(description = "지방 조절", example = "-4.6kg")
            private String fatAdjustment;

            @Schema(description = "근육 조절", example = "+ 0.9kg")
            private String muscleAdjustment;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class SegmentalMuscleAnalysis {
            @Schema(description = "좌측 팔", example = "표준")
            private String leftArm;

            @Schema(description = "우측 팔", example = "표준")
            private String rightArm;

            @Schema(description = "몸통", example = "표준")
            private String trunk;

            @Schema(description = "좌측 다리", example = "표준")
            private String leftLeg;

            @Schema(description = "우측 다리", example = "표준")
            private String rightLeg;
        }

        // ========== 변환 메서드 ==========

        public static InBodyDetailResponse from(InBody inBody, User user) {
            // 나이 계산
            int age = Period.between(user.getBirthDate(), LocalDate.now()).getYears();

            // 성별 한글 변환
            String genderText = user.getGender() == User.Gender.M ? "남성" : "여성";

            // 체성분 분석 생성
            BodyComposition bodyComp = BodyComposition.builder()
                    .totalBodyWater(formatWithRange(inBody.getTotalBodyWater(), 0.86, 1.14))
                    .protein(formatWithRange(inBody.getProtein(), 0.93, 1.12))
                    .mineral(formatWithRange(inBody.getMineral(), 0.90, 1.10))
                    .bodyFatMass(formatWithRange(inBody.getBodyFatMass(), 0.64, 1.02))
                    .weight(formatWithRange(inBody.getWeight(), 0.80, 1.08))
                    .build();

            // 골격근/지방 분석
            MuscleFatAnalysis muscleFat = MuscleFatAnalysis.builder()
                    .weight(inBody.getWeight())
                    .weightStatus("표준")
                    .skeletalMuscleMass(inBody.getSkeletalMuscleMass())
                    .skeletalMuscleStatus("표준")
                    .bodyFatMass(inBody.getBodyFatMass())
                    .bodyFatStatus("표준")
                    .build();

            // 비만 분석
            ObesityAnalysis obesity = ObesityAnalysis.builder()
                    .bmi(inBody.getBmi())
                    .bmiStatus(determineBmiStatus(inBody.getBmi()))
                    .bodyFatPercentage(inBody.getBodyFatPercentage())
                    .bodyFatPercentageStatus(determineBodyFatStatus(inBody.getBodyFatPercentage(), user.getGender()))
                    .build();

            // 체중 조절 계산
            BigDecimal targetWeight = calculateTargetWeight(user.getHeight(), user.getGender());
            WeightControl weightCtrl = WeightControl.builder()
                    .targetWeight(String.format("%.1fkg", targetWeight))
                    .weightAdjustment(calculateAdjustment(inBody.getWeight(), targetWeight))
                    .fatAdjustment(calculateAdjustment(inBody.getBodyFatMass(),
                            targetWeight.multiply(BigDecimal.valueOf(0.18))))
                    .muscleAdjustment(calculateMuscleAdjustment(inBody.getSkeletalMuscleMass(),
                            targetWeight.multiply(BigDecimal.valueOf(0.40))))
                    .build();

            // 부위별 근육 분석
            SegmentalMuscleAnalysis segmental = SegmentalMuscleAnalysis.builder()
                    .leftArm("표준")
                    .rightArm("표준")
                    .trunk("표준")
                    .leftLeg("표준")
                    .rightLeg("표준")
                    .build();

            return InBodyDetailResponse.builder()
                    .id(inBody.getId())
                    .score(calculateScore(inBody, user))
                    .measurementDate(inBody.getMeasurementDate().toString().replace("-", "."))
                    .age(age + "세")
                    .height(formatHeight(user.getHeight()))
                    .gender(genderText)
                    .bodyComposition(bodyComp)
                    .muscleFatAnalysis(muscleFat)
                    .obesityAnalysis(obesity)
                    .weightControl(weightCtrl)
                    .segmentalMuscleAnalysis(segmental)
                    .segmentalFatRatio(BigDecimal.valueOf(0.86))
                    .visceralFatLevel(inBody.getVisceralFatLevel() != null ?
                            inBody.getVisceralFatLevel().intValue() + " (1~20)" : "N/A")
                    .achievementBadge(inBody.getAchievementBadge() != null ?
                            inBody.getAchievementBadge() : InBody.AchievementBadge.NONE)
                    .createdAt(inBody.getCreatedAt())
                    .updatedAt(inBody.getUpdatedAt())
                    .build();
        }

        // ========== 유틸리티 메서드 ==========

        private static String formatWithRange(BigDecimal value, double lowerRatio, double upperRatio) {
            if (value == null) return "N/A";
            BigDecimal lower = value.multiply(BigDecimal.valueOf(lowerRatio));
            BigDecimal upper = value.multiply(BigDecimal.valueOf(upperRatio));
            return String.format("%.1f ( %.1f ~ %.1f )", value, lower, upper);
        }

        private static String formatHeight(Integer heightCm) {
            if (heightCm == null) return "N/A";
            return String.format("%.1fcm", heightCm / 1.0);
        }

        private static String determineBmiStatus(BigDecimal bmi) {
            if (bmi == null) return "표준";
            if (bmi.compareTo(BigDecimal.valueOf(18.5)) < 0) return "저체중";
            if (bmi.compareTo(BigDecimal.valueOf(23)) < 0) return "표준";
            if (bmi.compareTo(BigDecimal.valueOf(25)) < 0) return "과체중";
            return "비만";
        }

        private static String determineBodyFatStatus(BigDecimal bodyFat, User.Gender gender) {
            if (bodyFat == null) return "표준";
            if (gender == User.Gender.M) {
                if (bodyFat.compareTo(BigDecimal.valueOf(10)) < 0) return "낮음";
                if (bodyFat.compareTo(BigDecimal.valueOf(20)) < 0) return "표준";
                return "높음";
            } else {
                if (bodyFat.compareTo(BigDecimal.valueOf(18)) < 0) return "낮음";
                if (bodyFat.compareTo(BigDecimal.valueOf(28)) < 0) return "표준";
                return "높음";
            }
        }

        private static BigDecimal calculateTargetWeight(Integer height, User.Gender gender) {
            if (height == null) return BigDecimal.valueOf(60);
            double heightM = height / 100.0;
            double bmi = gender == User.Gender.M ? 22 : 21;
            return BigDecimal.valueOf(bmi * heightM * heightM);
        }

        private static String calculateAdjustment(BigDecimal current, BigDecimal target) {
            if (current == null || target == null) return "N/A";
            BigDecimal diff = current.subtract(target);
            String sign = diff.compareTo(BigDecimal.ZERO) > 0 ? "-" : "+";
            return sign + String.format("%.1fkg", diff.abs());
        }

        private static String calculateMuscleAdjustment(BigDecimal current, BigDecimal target) {
            if (current == null || target == null) return "N/A";
            BigDecimal diff = target.subtract(current);
            String sign = diff.compareTo(BigDecimal.ZERO) > 0 ? "+ " : "- ";
            return sign + String.format("%.1fkg", diff.abs());
        }

        private static Integer calculateScore(InBody inBody, User user) {
            int score = 70;

            // BMI 점수
            if (inBody.getBmi() != null) {
                BigDecimal bmi = inBody.getBmi();
                if (bmi.compareTo(BigDecimal.valueOf(18.5)) >= 0 &&
                        bmi.compareTo(BigDecimal.valueOf(23)) < 0) {
                    score += 10;
                }
            }

            // 체지방률 점수
            if (inBody.getBodyFatPercentage() != null) {
                BigDecimal bodyFat = inBody.getBodyFatPercentage();
                if (user.getGender() == User.Gender.M) {
                    if (bodyFat.compareTo(BigDecimal.valueOf(10)) >= 0 &&
                            bodyFat.compareTo(BigDecimal.valueOf(20)) < 0) {
                        score += 10;
                    }
                } else {
                    if (bodyFat.compareTo(BigDecimal.valueOf(18)) >= 0 &&
                            bodyFat.compareTo(BigDecimal.valueOf(28)) < 0) {
                        score += 10;
                    }
                }
            }

            // 골격근량 점수
            if (inBody.getSkeletalMuscleMass() != null) {
                score += 10;
            }

            return Math.min(100, score);
        }
    }

    // ==================== 등록 응답 ====================
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "인바디 등록 응답")
    public static class InBodyCreateResponse {

        @Schema(description = "성공 여부", example = "true")
        private boolean success;

        @Schema(description = "메시지", example = "인바디 정보가 등록되었습니다")
        private String message;

        @Schema(description = "인바디 상세 정보")
        private InBodyDetailResponse inBody;
    }

    // ==================== OCR 업로드 응답 ====================
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "인바디 OCR 초안 응답")
    public static class InBodyOcrUploadResponse {

        @Schema(description = "성공 여부", example = "true")
        private boolean success;

        @Schema(description = "메시지", example = "인바디 초안 데이터를 확인해 주세요")
        private String message;

        @Schema(description = "업로드된 이미지 URL")
        private String imageUrl;

        @Schema(description = "AI가 해석한 초안 수치 (최종 반영)")
        private InBodyOcrResult draftData;

    }

    // ==================== 수정 응답 ====================
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "인바디 수정 응답")
    public static class InBodyUpdateResponse {

        @Schema(description = "성공 여부", example = "true")
        private boolean success;

        @Schema(description = "메시지", example = "인바디 정보가 수정되었습니다")
        private String message;

        @Schema(description = "인바디 상세 정보")
        private InBodyDetailResponse inBody;
    }
}
