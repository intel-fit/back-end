package rto.intelfit.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeeklyRecommendedExerciseSaveDto {

    private String startDate;  // optional
    private List<DayExercise> days;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DayExercise {
        private String date; // yyyy-MM-dd
        private List<ExerciseItem> exercises;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ExerciseItem {
        private String exerciseId;
        private String name;
        private String target;
    }
}
