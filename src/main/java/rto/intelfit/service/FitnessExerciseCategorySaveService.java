package rto.intelfit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rto.intelfit.domain.FitnessExerciseCategorySave;
import rto.intelfit.domain.User;
import rto.intelfit.dto.FitnessExerciseCategorySaveDto;
import rto.intelfit.exception.BusinessException;
import rto.intelfit.exception.ErrorCode;
import rto.intelfit.repository.FitnessExerciseCategorySaveRepository;
import rto.intelfit.repository.UserRepository;
import rto.intelfit.repository.DailyProgressRepository;
import rto.intelfit.domain.DailyProgress;
import rto.intelfit.dto.ExerciseFeedbackDto;
import java.util.ArrayList;
import java.util.HashMap;
import rto.intelfit.domain.ExerciseGoal;
import rto.intelfit.repository.ExerciseGoalRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Comparator;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class FitnessExerciseCategorySaveService {

    private final FitnessExerciseCategorySaveRepository saveRepository;
    private final UserRepository userRepository;
    private final DailyProgressService dailyProgressService;
    private final DailyProgressRepository dailyProgressRepository;
    private final AIServerService aiServerService;
    private final ExerciseGoalRepository exerciseGoalRepository;


    @Transactional(readOnly = true)
    public List<FitnessExerciseCategorySaveDto.SessionResponse> getUserGroupedSessions(Long userId) {
        log.info("🔍 유저 ID={} 의 운동 세션 기록 조회 시작", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        List<FitnessExerciseCategorySave> records =
                saveRepository.findByUserOrderByWorkoutDateDesc(user);

        // sessionId 기준 그룹핑
        Map<String, List<FitnessExerciseCategorySave>> grouped =
                records.stream().collect(Collectors.groupingBy(FitnessExerciseCategorySave::getSessionId));

        List<FitnessExerciseCategorySaveDto.SessionResponse> result = grouped.entrySet().stream()
                .map(entry -> FitnessExerciseCategorySaveDto.SessionResponse.from(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        log.info("✅ 유저 ID={} 의 운동 세션 {}개 반환", userId, result.size());
        return result;
    }


    public FitnessExerciseCategorySaveDto.DeleteResponse deleteBySessionId(String sessionId) {

        log.info("🗑 세션 ID={} 삭제 요청", sessionId);

        List<FitnessExerciseCategorySave> sessionRecords =
                saveRepository.findBySessionId(sessionId);

        if (sessionRecords.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "해당 세션 ID에 해당하는 기록이 없습니다.");
        }

        FitnessExerciseCategorySave first = sessionRecords.get(0);
        User user = first.getUser();
        String externalId = first.getExternalId();

        // 🔥 기준 날짜는 무조건 "오늘"
        LocalDate progressDate = LocalDate.now();

        ExerciseGoal goal =
                exerciseGoalRepository.findByUser(user).orElse(null);

        if (goal != null && first.isSaved()) {

            long sessionSeconds = first.getExerciseSeconds();

            DailyProgress progress =
                    dailyProgressRepository
                            .findByUserIdAndDate(user.getId(), progressDate)
                            .orElse(null);

            if (progress != null) {

                long updatedSeconds =
                        progress.getTotalExerciseSeconds() - sessionSeconds;

                progress.setTotalExerciseSeconds(
                        Math.max(0L, updatedSeconds)
                );

                log.info("✔ DailyProgress 차감 userId={}, date={}, remainingSeconds={}",
                        user.getId(), progressDate, progress.getTotalExerciseSeconds());
            }
        }

        // 🔥 세션 삭제
        saveRepository.deleteAll(sessionRecords);

        return FitnessExerciseCategorySaveDto.DeleteResponse.builder()
                .sessionId(sessionId)
                .externalId(externalId)
                .deletedCount(sessionRecords.size())
                .build();
    }







//
public String addWorkoutSession(FitnessExerciseCategorySaveDto.CreateRequest request) {

    log.info("💪 운동 세션 추가 요청 - userId={}, exerciseName={}, seconds={}",
            request.getUserId(), request.getExerciseName(), request.getSeconds());

    User user = userRepository.findById(request.getUserId())
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

    String sessionId = "S-" + System.currentTimeMillis();

    long sessionSeconds = request.getSeconds();
    double met = resolveMet(request.getCategory());

    List<FitnessExerciseCategorySave> entities =
            request.getSets().stream()
                    .map(set -> FitnessExerciseCategorySave.builder()
                            .user(user)
                            .sessionId(sessionId)
                            .externalId(request.getExternalId())
                            .exerciseName(request.getExerciseName())
                            .category(request.getCategory())
                            .setNumber(set.getSetNumber())
                            .weight(set.getWeight())
                            .reps(set.getReps())
                            .workoutDate(request.getWorkoutDate())
                            .exerciseSeconds(sessionSeconds)
                            .met(met)
                            .caloriesBurned(sessionSeconds * met)
                            .isSaved(false)
                            .completed(false)
                            .build())
                    .toList();

    saveRepository.saveAll(entities);

    return sessionId;
}




    public FitnessExerciseCategorySaveDto.SaveResponse saveUnsavedWorkouts(
            Long userId,
            String saveTitle,
            LocalDate date
    ) {

        List<FitnessExerciseCategorySave> rows =
                saveRepository.findByUserIdAndIsSavedFalse(userId);

        if (rows.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "저장할 운동 기록이 없습니다.");
        }

        rows.forEach(r -> {
            r.setSaved(true);
            r.setSaveTitle(saveTitle);
            r.setDate(date);  // 🔥 여기서 날짜를 넣는다
        });

        List<String> sessionIds = rows.stream()
                .map(FitnessExerciseCategorySave::getSessionId)
                .distinct()
                .toList();

        return FitnessExerciseCategorySaveDto.SaveResponse.builder()
                .sessionIds(sessionIds)
                .saveTitle(saveTitle)
                .updatedCount(rows.size())
                .build();
    }

    @Transactional(readOnly = true)
    public List<FitnessExerciseCategorySaveDto.SavedGroupResponse> getSavedWorkoutGroupsByDate(
            Long userId,
            LocalDate date
    ) {

        List<FitnessExerciseCategorySave> saved =
                saveRepository.findByUserIdAndIsSavedTrueAndDateOrderBySaveTitleAsc(userId, date);

        if (saved.isEmpty()) {
            return List.of();
        }

        // title 기준 그룹핑
        Map<String, List<FitnessExerciseCategorySave>> byTitle =
                saved.stream().collect(Collectors.groupingBy(FitnessExerciseCategorySave::getSaveTitle));

        return byTitle.entrySet().stream()
                .map(titleEntry -> {

                    String title = titleEntry.getKey();
                    List<FitnessExerciseCategorySave> titleRows = titleEntry.getValue();

                    // sessionId 기준 그룹핑
                    Map<String, List<FitnessExerciseCategorySave>> bySession =
                            titleRows.stream().collect(Collectors.groupingBy(FitnessExerciseCategorySave::getSessionId));

                    List<FitnessExerciseCategorySaveDto.SavedGroupResponse.SessionGroup> sessions =
                            bySession.entrySet().stream()
                                    .map(s -> FitnessExerciseCategorySaveDto.SavedGroupResponse.SessionGroup.builder()
                                            .sessionId(s.getKey())
                                            .records(
                                                    s.getValue().stream()
                                                            .map(FitnessExerciseCategorySaveDto.SavedSetDetail::from)
                                                            .collect(Collectors.toList())
                                            )
                                            .build()
                                    )
                                    .collect(Collectors.toList());

                    return FitnessExerciseCategorySaveDto.SavedGroupResponse.builder()
                            .title(title)
                            .sessions(sessions)
                            .build();

                })
                .collect(Collectors.toList());
    }





    @Transactional(readOnly = true)
    public List<FitnessExerciseCategorySaveDto.SavedGroupResponse> getSavedWorkoutGroups(Long userId) {

        List<FitnessExerciseCategorySave> saved =
                saveRepository.findByUserIdAndIsSavedTrueOrderBySaveTitleAsc(userId);

        if (saved.isEmpty()) {
            return List.of();
        }

        // 1) 제목 기준 그룹핑
        Map<String, List<FitnessExerciseCategorySave>> byTitle =
                saved.stream().collect(Collectors.groupingBy(FitnessExerciseCategorySave::getSaveTitle));

        // 2) 각 title 내에서 sessionId 기준 그룹핑
        return byTitle.entrySet().stream()
                .map(titleEntry -> {
                    String title = titleEntry.getKey();
                    List<FitnessExerciseCategorySave> titleRows = titleEntry.getValue();

                    Map<String, List<FitnessExerciseCategorySave>> bySession =
                            titleRows.stream().collect(Collectors.groupingBy(FitnessExerciseCategorySave::getSessionId));

                    List<FitnessExerciseCategorySaveDto.SavedGroupResponse.SessionGroup> sessions =
                            bySession.entrySet().stream().map(sessionEntry ->
                                    FitnessExerciseCategorySaveDto.SavedGroupResponse.SessionGroup.builder()
                                            .sessionId(sessionEntry.getKey())
                                            .records(
                                                    sessionEntry.getValue()
                                                            .stream()
                                                            .map(FitnessExerciseCategorySaveDto.SavedSetDetail::from)
                                                            .collect(Collectors.toList())
                                            )
                                            .build()
                            ).collect(Collectors.toList());

                    return FitnessExerciseCategorySaveDto.SavedGroupResponse.builder()
                            .title(title)
                            .sessions(sessions)
                            .build();
                })
                .collect(Collectors.toList());
    }




public FitnessExerciseCategorySaveDto.ToggleResponse toggleSessionCompletion(String sessionId) {

    List<FitnessExerciseCategorySave> sessionRecords =
            saveRepository.findBySessionId(sessionId);

    if (sessionRecords.isEmpty()) {
        throw new BusinessException(ErrorCode.NOT_FOUND, "해당 세션 ID에 해당하는 기록이 없습니다.");
    }

    boolean hasIncomplete =
            sessionRecords.stream().anyMatch(r -> !r.isCompleted());

    boolean newStatus = hasIncomplete;

    sessionRecords.forEach(r -> r.setCompleted(newStatus));
    saveRepository.saveAll(sessionRecords);

    User user = sessionRecords.get(0).getUser();
    LocalDate date = sessionRecords.get(0).getWorkoutDate().toLocalDate();

    // ✅ 달성률 재계산 복구
    dailyProgressService.recalculateProgress(user, date);

    return FitnessExerciseCategorySaveDto.ToggleResponse.builder()
            .sessionId(sessionId)
            .completed(newStatus)
            .affectedSets(sessionRecords.size())
            .build();
}

@Transactional
    public void addDailyExerciseSeconds(Long userId, long seconds) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "유저를 찾을 수 없습니다."));

        LocalDate today = LocalDate.now();

        DailyProgress progress = dailyProgressRepository.findByUserIdAndDate(userId, today)
                .orElse(null);

        if (progress == null) {

            progress = DailyProgress.builder()
                    .user(user)
                    .date(today)
                    .totalExerciseSeconds(0L)
                    .totalCalorie(0.0)
                    .exerciseRate(0.0)
                    .build();

            try {
                progress = dailyProgressRepository.save(progress);
            } catch (Exception e) {
                // 다른 요청이 먼저 INSERT 하면 여기서 UNIQUE 제약 위반
                progress = dailyProgressRepository.findByUserIdAndDate(userId, today)
                        .orElseThrow(() -> new BusinessException(ErrorCode.SERVER_ERROR, "운동 시간 저장 중 충돌 발생"));
            }
        }

        progress.setTotalExerciseSeconds(progress.getTotalExerciseSeconds() + seconds);

        log.info("✔ 오늘 운동시간 누적 userId={}, totalSeconds={}",
                userId, progress.getTotalExerciseSeconds());
    }


    @Transactional(readOnly = true)
    public long getTodayWorkoutSeconds(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "유저를 찾을 수 없습니다."));

        LocalDate today = LocalDate.now();

        return dailyProgressRepository.findByUserIdAndDate(userId, today)
                .map(DailyProgress::getTotalExerciseSeconds)
                .orElse(0L); // 오늘 기록이 없으면 0초로 반환
    }
    /** 문자열 → 초 단위 운동시간 파싱 ("30분 이상") → 1800 초 */
    private long parseDurationToSeconds(String durationPerSession) {
        int minutes = Integer.parseInt(durationPerSession.replaceAll("\\D", ""));
        return minutes * 60L;
    }


public FitnessExerciseCategorySaveDto.SaveResponse saveUnsavedWorkoutsAndSendFeedback(
        Long userId,
        String saveTitle,
        List<Double> intensityList,
        List<String> feedbackList,
        LocalDate date

) {
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

    // 1️⃣ 저장 전 unsaved 세션 확보
    List<FitnessExerciseCategorySave> unsavedRows =
            saveRepository.findByUserIdAndIsSavedFalse(userId);

    if (unsavedRows.isEmpty()) {
        throw new BusinessException(ErrorCode.NOT_FOUND, "저장할 운동 기록이 없습니다.");
    }

    // 2️⃣ sessionId 기준 중복 제거 후 시간 합산
    long totalSessionSeconds =
            unsavedRows.stream()
                    .collect(Collectors.groupingBy(FitnessExerciseCategorySave::getSessionId))
                    .values()
                    .stream()
                    .mapToLong(list -> list.get(0).getExerciseSeconds())
                    .sum();

    // 3️⃣ 저장 처리
    FitnessExerciseCategorySaveDto.SaveResponse response =
            saveUnsavedWorkouts(userId, saveTitle, date);

    // 4️⃣ DailyProgress 조회 또는 생성
    DailyProgress progress =
            dailyProgressRepository.findByUserIdAndDate(userId, date)
                    .orElseGet(() -> dailyProgressRepository.save(
                            DailyProgress.builder()
                                    .user(user)
                                    .date(date)
                                    .totalExerciseSeconds(0L)
                                    .totalCalorie(0.0)
                                    .exerciseRate(0.0)
                                    .build()
                    ));

    // 5️⃣ 운동 시간 누적
    progress.setTotalExerciseSeconds(
            progress.getTotalExerciseSeconds() + totalSessionSeconds
    );

    // 6️⃣ 달성률 재계산
    ExerciseGoal goal = exerciseGoalRepository.findByUser(user).orElse(null);
    if (goal != null) {
        long requiredSeconds = parseDurationToSeconds(goal.getDurationPerSession());
        double rate = Math.min(
                ((double) progress.getTotalExerciseSeconds() / requiredSeconds) * 100.0,
                100.0
        );
        progress.setExerciseRate(rate);
    }

    dailyProgressRepository.save(progress);

    // 7️⃣ AI 피드백 전송
    List<FitnessExerciseCategorySave> records =
            saveRepository.findByUserIdAndSessionIdIn(userId, response.getSessionIds());

    ExerciseFeedbackDto.Request req =
            buildExerciseFeedbackRequest(userId, saveTitle, records, intensityList, feedbackList);

    aiServerService.sendExerciseFeedback(req);

    return response;
}





private ExerciseFeedbackDto.Request buildExerciseFeedbackRequest(
            Long userId,
            String saveTitle,
            List<FitnessExerciseCategorySave> records,
            List<Double> intensityList,
            List<String> feedbackList
    ) {
        // 운동 단위로 그룹핑 (sessionId + exerciseName 단위)
        Map<String, List<FitnessExerciseCategorySave>> byExercise =
                records.stream().collect(Collectors.groupingBy(r ->
                        r.getSessionId() + "::" + r.getExerciseName()
                ));

        List<String> exerciseKeys = byExercise.keySet().stream().toList();

        List<ExerciseFeedbackDto.Item> items = new ArrayList<>();

        for (int i = 0; i < exerciseKeys.size(); i++) {

            String key = exerciseKeys.get(i);
            List<FitnessExerciseCategorySave> setList = byExercise.get(key);

            // setNumber 순서대로 정렬
            setList.sort(Comparator.comparingInt(s -> s.getSetNumber() != null ? s.getSetNumber() : 0));

            FitnessExerciseCategorySave last = setList.get(setList.size() - 1);

            // warmup = 마지막 세트 제외
            List<Map<String, Object>> warmup =
                    setList.stream()
                            .filter(s -> !s.getId().equals(last.getId()))
                            .map(s -> {
                                Map<String, Object> m = new HashMap<>();
                                m.put("weight", s.getWeight());
                                m.put("reps", s.getReps());
                                return m;
                            })
                            .collect(Collectors.toList());


            // 🔥 intensityList[i], feedbackList[i] 매칭
            Double intensity = (intensityList != null && intensityList.size() > i)
                    ? intensityList.get(i) : null;

            String feedback = (feedbackList != null && feedbackList.size() > i)
                    ? feedbackList.get(i) : "neutral";

            items.add(
                    ExerciseFeedbackDto.Item.builder()
                            .exercise_id(last.getExternalId() != null ? last.getExternalId() : last.getExerciseName())
                            .name(last.getExerciseName())
                            .weight(last.getWeight())
                            .reps(last.getReps())
                            .sets(setList.size())
                            .warmup(warmup)
                            .intensity(intensity)    // 🔥 여기 붙음
                            .feedback(feedback)      // 🔥 여기 붙음
                            .build()
            );
        }

        return ExerciseFeedbackDto.Request.builder()
                .user_id(String.valueOf(userId))  // 실제로는 user.userId 문자열 사용 가능
                .session_name(saveTitle)
                .duration_min(null)
                .items(items)
                .build();
    }
    private static final Map<String, List<String>> BODY_PART_MAP = Map.of(
            "가슴", List.of("가슴", "흉근", "대흉근"),
            "등", List.of("등", "광배근", "승모근", "척추기립근"),
            "어깨", List.of("어깨", "삼각근", "전면삼각근", "측면삼각근", "후면삼각근"),
            "팔", List.of("팔", "이두", "삼두", "전완근", "이두근", "삼두근"),
            "하체", List.of("하체", "허벅지", "햄스트링", "종아리", "둔근", "대퇴사두근", "엉덩이"),
            "복부", List.of("복부", "복근", "복직근", "복사근", "코어")
    );

    private static final Map<String, Double> MET_MAP = Map.of(
            "가슴", 6.0,
            "등", 6.5,
            "어깨", 5.5,
            "팔", 5.0,
            "하체", 7.5,
            "복부", 4.5
    );
    private double resolveMet(String rawCategory) {
        if (rawCategory == null) return 5.0; // default

        for (Map.Entry<String, List<String>> entry : BODY_PART_MAP.entrySet()) {
            if (entry.getValue().stream().anyMatch(rawCategory::contains)) {
                return MET_MAP.get(entry.getKey());
            }
        }
        return 5.0; // fallback
    }


}
