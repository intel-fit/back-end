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
        private List<String> warmup;     // 문자열 리스트
        private List<String> feedback;   // 문자열 리스트
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Request {
        private String user_id;
        private String session_name; // a
        private Integer duration_min;      // null


        private Integer intensity;   // 세션 intensity
        private String feedback;    // 세션 feedback

        private List<Item> items;         // intensity/feedback 포함
    }

}
