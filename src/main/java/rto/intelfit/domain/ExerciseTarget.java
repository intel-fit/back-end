package rto.intelfit.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "exercise_targets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExerciseTarget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_set_id", nullable = false)
    private ExerciseSet exerciseSet;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_muscle", nullable = false)
    private TargetMuscle targetMuscle;

    @Column(name = "intensity_percentage")
    private Integer intensityPercentage; // 해당 부위에 대한 강도 (0-100%)

    // 타겟 근육 부위
    @Getter
    @RequiredArgsConstructor
    public enum TargetMuscle {
        // 하체
        QUADRICEPS("대퇴사두근", "하체"),
        HAMSTRINGS("햄스트링", "하체"),
        GLUTES("둔근", "하체"),
        CALVES("종아리", "하체"),
        ADDUCTORS("내전근", "하체"),

        // 가슴
        UPPER_CHEST("상부 가슴", "가슴"),
        MIDDLE_CHEST("중부 가슴", "가슴"),
        LOWER_CHEST("하부 가슴", "가슴"),

        // 등
        LATS("광배근", "등"),
        UPPER_BACK("상부 등", "등"),
        LOWER_BACK("하부 등", "등"),
        TRAPS("승모근", "등"),

        // 어깨
        FRONT_DELTS("전면 삼각근", "어깨"),
        SIDE_DELTS("측면 삼각근", "어깨"),
        REAR_DELTS("후면 삼각근", "어깨"),

        // 팔
        BICEPS("이두근", "팔"),
        TRICEPS("삼두근", "팔"),
        FOREARMS("전완근", "팔"),

        // 코어
        ABS("복근", "코어"),
        OBLIQUES("복사근", "코어"),
        LOWER_ABS("하복부", "코어"),

        // 유산소 (전신)
        FULL_BODY_CARDIO("전신 유산소", "전신");

        private final String description;
        private final String category;
    }
}