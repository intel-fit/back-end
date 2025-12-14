package rto.intelfit.dto;

import lombok.*;
import java.time.LocalDate;
import rto.intelfit.domain.DailyProgress;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyProgressDto {
    private LocalDate date;
    private double exerciseRate;
    private double totalCalorie;
    // ⭐ 새로 추가
    private long totalExerciseSeconds;
    private int minutes;
    private int seconds;

    public static DailyProgressDto from(DailyProgress p) {
        long sec = p.getTotalExerciseSeconds();
        return DailyProgressDto.builder()
                .date(p.getDate())
                .exerciseRate(p.getExerciseRate())
                .totalCalorie(p.getTotalCalorie())
                .totalExerciseSeconds(sec)
                .minutes((int)(sec / 60))
                .seconds((int)(sec % 60))
                .build();
    }
}
