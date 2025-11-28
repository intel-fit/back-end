package rto.intelfit.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class InBodyOcrResult {

    private String measurementDate;
    private BigDecimal weight;
    private BigDecimal muscleMass;
    private BigDecimal bodyFatMass;
    private BigDecimal skeletalMuscleMass;
    private BigDecimal bodyFatPercentage;
    private BigDecimal leftArmMuscle;
    private BigDecimal rightArmMuscle;
    private BigDecimal trunkMuscle;
    private BigDecimal leftLegMuscle;
    private BigDecimal rightLegMuscle;
    private BigDecimal leftArmFat;
    private BigDecimal rightArmFat;
    private BigDecimal trunkFat;
    private BigDecimal leftLegFat;
    private BigDecimal rightLegFat;
    private BigDecimal totalBodyWater;
    private BigDecimal protein;
    private BigDecimal mineral;
    private BigDecimal bmi;
    private BigDecimal bodyFatPercentageStandard;
    private BigDecimal obesityDegree;
    private BigDecimal visceralFatLevel;
    private Integer basalMetabolicRate;

    public InBodyDto.InBodyCreateRequest toCreateRequest() {
        return InBodyDto.InBodyCreateRequest.builder()
                .measurementDate(parseMeasurementDate())
                .weight(weight)
                .muscleMass(muscleMass)
                .bodyFatMass(bodyFatMass)
                .skeletalMuscleMass(skeletalMuscleMass)
                .bodyFatPercentage(bodyFatPercentage)
                .leftArmMuscle(leftArmMuscle)
                .rightArmMuscle(rightArmMuscle)
                .trunkMuscle(trunkMuscle)
                .leftLegMuscle(leftLegMuscle)
                .rightLegMuscle(rightLegMuscle)
                .leftArmFat(leftArmFat)
                .rightArmFat(rightArmFat)
                .trunkFat(trunkFat)
                .leftLegFat(leftLegFat)
                .rightLegFat(rightLegFat)
                .totalBodyWater(totalBodyWater)
                .protein(protein)
                .mineral(mineral)
                .bmi(bmi)
                .bodyFatPercentageStandard(bodyFatPercentageStandard)
                .obesityDegree(obesityDegree)
                .visceralFatLevel(visceralFatLevel)
                .basalMetabolicRate(basalMetabolicRate)
                .build();
    }

    private LocalDate parseMeasurementDate() {
        if (!StringUtils.hasText(measurementDate)) {
            return null;
        }
        String normalized = measurementDate.trim()
                .replace(".", "-")
                .replace("/", "-");
        if (normalized.matches("\\d{8}")) {
            normalized = normalized.substring(0, 4) + "-" +
                    normalized.substring(4, 6) + "-" +
                    normalized.substring(6);
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        try {
            return LocalDate.parse(normalized, formatter);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
