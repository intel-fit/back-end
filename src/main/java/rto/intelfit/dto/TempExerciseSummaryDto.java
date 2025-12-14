package rto.intelfit.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TempExerciseSummaryDto {

    private String date;
    private String focus;
    private double durationMin;
    private double kcal;
    private int exerciseCount;
    private String title;
}
