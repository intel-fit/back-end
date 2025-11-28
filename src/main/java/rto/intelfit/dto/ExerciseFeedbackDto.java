package rto.intelfit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

public class ExerciseFeedbackDto {

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Item {
        private String exercise_id;
        private String name;
        private Double weight;
        private Integer reps;
        private Integer sets;
        private List<Map<String, Object>> warmup;

        private Double intensity;      // 🔥 운동 단위 intensity
        private String feedback;       // 🔥 운동 단위 feedback
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Request {
        private String user_id;
        private String session_name;
        private Integer duration_min;      // null
        private List<Item> items;          // intensity/feedback 포함
    }

}
