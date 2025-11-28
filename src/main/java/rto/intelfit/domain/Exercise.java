package rto.intelfit.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "exercises")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Exercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "exercise_date", nullable = false)
    private LocalDate exerciseDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "exercise_category", nullable = false, length = 20)
    private ExerciseCategory exerciseCategory;

    @Column(name = "total_duration_minutes")
    private Integer totalDurationMinutes;

    @Column(name = "memo", length = 500)
    private String memo;

    @OneToMany(mappedBy = "exercise", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ExerciseSet> exerciseSets = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 운동 세트 추가
    public void addExerciseSet(ExerciseSet exerciseSet) {
        exerciseSets.add(exerciseSet);
        exerciseSet.setExercise(this);
    }

    // 운동 카테고리 Enum
    @Getter
    @RequiredArgsConstructor
    public enum ExerciseCategory {
        CARDIO("유산소"),
        RESISTANCE("무산소");

        private final String description;
    }

    // 유산소 운동 타입
    @Getter
    @RequiredArgsConstructor
    public enum CardioType {
        TREADMILL("런닝머신", ExerciseCategory.CARDIO),
        MY_MOUNTAIN("마이마운틴", ExerciseCategory.CARDIO),
        STAIR_MASTER("천국의 계단", ExerciseCategory.CARDIO),
        CYCLE("사이클", ExerciseCategory.CARDIO);

        private final String description;
        private final ExerciseCategory category;
    }

    // 무산소 운동 부위
    @Getter
    @RequiredArgsConstructor
    public enum MuscleGroup {
        LEGS("하체", ExerciseCategory.RESISTANCE),
        CHEST("가슴", ExerciseCategory.RESISTANCE),
        BACK("등", ExerciseCategory.RESISTANCE),
        SHOULDERS("어깨", ExerciseCategory.RESISTANCE),
        ARMS("팔", ExerciseCategory.RESISTANCE);

        private final String description;
        private final ExerciseCategory category;
    }

    // 무산소 운동 종목
    @Getter
    @RequiredArgsConstructor
    public enum ResistanceExerciseType {
        // 하체
        BARBELL_SQUAT("바벨 스쿼트", MuscleGroup.LEGS),
        LEG_PRESS("레그프레스", MuscleGroup.LEGS),
        HACK_SQUAT("핵스쿼트", MuscleGroup.LEGS),
        CONVENTIONAL_DEADLIFT("컨벤셔널 데드리프트", MuscleGroup.LEGS),
        BULGARIAN_SPLIT_SQUAT("불가리안 스플릿 스쿼트", MuscleGroup.LEGS),
        LUNGE("런지", MuscleGroup.LEGS),
        LEG_CURL("레그 컬", MuscleGroup.LEGS),
        LEG_EXTENSION("레그 익스텐션", MuscleGroup.LEGS),

        // 가슴
        PUSH_UP("푸쉬업", MuscleGroup.CHEST),
        DIPS("딥스", MuscleGroup.CHEST),
        BENCH_PRESS("벤치프레스", MuscleGroup.CHEST),
        INCLINE_DUMBBELL_PRESS("인클라인 덤벨 프레스", MuscleGroup.CHEST),
        SMITH_MACHINE_INCLINE_BENCH_PRESS("스미스머신 인클라인 벤치 프레스", MuscleGroup.CHEST),
        CHEST_PRESS("체스트 프레스", MuscleGroup.CHEST),
        PEC_DECK_FLY("펙 덱 플라이", MuscleGroup.CHEST),
        DUMBBELL_FLY("덤벨 플라이", MuscleGroup.CHEST),
        CABLE_FLY("케이블 플라이", MuscleGroup.CHEST),

        // 등
        PULL_UP("풀업", MuscleGroup.BACK),
        ROMANIAN_DEADLIFT("루마니안 데드리프트", MuscleGroup.BACK),
        WIDE_GRIP_LAT_PULLDOWN("와이드 그립 랫풀다운", MuscleGroup.BACK),
        CLOSE_GRIP_LAT_PULLDOWN("클로즈 그립 랫풀다운", MuscleGroup.BACK),
        ARM_PULLDOWN("암풀다운", MuscleGroup.BACK),
        BARBELL_ROW("바벨로우", MuscleGroup.BACK),
        ONE_ARM_DUMBBELL_ROW("원암 덤벨로우", MuscleGroup.BACK),
        CHEST_SUPPORTED_DUMBBELL_ROW("체스트 서포트 투암 덤벨로우", MuscleGroup.BACK),
        SEATED_CABLE_ROW("시티드 케이블 로우", MuscleGroup.BACK),
        SEATED_ROW_MACHINE("시티드 로우 머신", MuscleGroup.BACK),
        CABLE_ROW("케이블 로우", MuscleGroup.BACK),

        // 어깨
        OVERHEAD_PRESS("오버헤드 프레스", MuscleGroup.SHOULDERS),
        SEATED_DUMBBELL_SHOULDER_PRESS("시티드 덤벨 숄더 프레스", MuscleGroup.SHOULDERS),
        SEATED_MACHINE_SHOULDER_PRESS("시티드 머신 숄더 프레스", MuscleGroup.SHOULDERS),
        SIDE_LATERAL_RAISE("사이드 레터럴 레이즈", MuscleGroup.SHOULDERS),
        CABLE_SIDE_LATERAL_RAISE("케이블 사이드 레터럴 레이즈", MuscleGroup.SHOULDERS),
        DUMBBELL_FRONT_RAISE("덤벨 프론트 레이즈", MuscleGroup.SHOULDERS),
        BENT_OVER_LATERAL_RAISE("벤트오버 레터럴 레이즈", MuscleGroup.SHOULDERS),
        REVERSE_PEC_DECK_FLY("리버스 펙 덱 플라이", MuscleGroup.SHOULDERS),

        // 팔
        CHIN_UP("친업", MuscleGroup.ARMS),
        BARBELL_CURL("바벨컬", MuscleGroup.ARMS),
        DUMBBELL_CURL("덤벨컬", MuscleGroup.ARMS),
        HAMMER_CURL("해머컬", MuscleGroup.ARMS),
        CABLE_CURL("케이블컬", MuscleGroup.ARMS),
        PREACHER_CURL("프리쳐컬", MuscleGroup.ARMS),
        CABLE_PUSH_DOWN("케이블 푸쉬 다운", MuscleGroup.ARMS),
        CLOSE_GRIP_BENCH_PRESS("클로즈 그립 벤치프레스", MuscleGroup.ARMS),
        DUMBBELL_OVERHEAD_EXTENSION("덤벨 오버헤드 익스텐션", MuscleGroup.ARMS),
        CABLE_OVERHEAD_EXTENSION("케이블 오버헤드 익스텐션", MuscleGroup.ARMS),
        LYING_TRICEPS_EXTENSION("라잉 트라이셉스 익스텐션", MuscleGroup.ARMS);

        private final String description;
        private final MuscleGroup muscleGroup;
    }
}