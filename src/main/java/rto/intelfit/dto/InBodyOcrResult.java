package rto.intelfit.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.Data;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class InBodyOcrResult {

    private String measurementDate;
    private String gender;
    private Integer age;
    private BigDecimal height;
    private BigDecimal weight;
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
    private BigDecimal visceralFatLevel;

    @JsonSetter("height")
    public void setHeight(Object height) {
        if (height == null) {
            this.height = null;
            return;
        }
        String value = height.toString();
        if (!StringUtils.hasText(value)) {
            this.height = null;
            return;
        }
        // 숫자와 소수점만 추출 (예: "170cm", "170.5 cm", "170")
        Matcher matcher = Pattern.compile("(\\d+(?:\\.\\d+)?)").matcher(value);
        if (matcher.find()) {
            this.height = new BigDecimal(matcher.group(1));
        } else {
            this.height = null;
        }
    }

    public InBodyDto.InBodyCreateRequest toCreateRequest() {
        return InBodyDto.InBodyCreateRequest.builder()
                .measurementDate(parseMeasurementDate())
                .weight(weight)
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
                .visceralFatLevel(visceralFatLevel)
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
