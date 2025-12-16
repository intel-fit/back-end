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
    @Transactional(readOnly = true)
    public FitnessExerciseCategorySaveDto.DailyCaloriesResponse getDailyCalories(Long userId, LocalDate date) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 해당 날짜의 모든 운동 기록 불러오기
        List<FitnessExerciseCategorySave> records =
                saveRepository.findByUserIdAndDateOrderBySessionIdAsc(userId, date);

        if (records.isEmpty()) {
            return FitnessExerciseCategorySaveDto.DailyCaloriesResponse.builder()
                    .date(date)
                    .totalCalories(0)
                    .sessions(List.of())
                    .build();
        }

        // 🔥 sessionId 기준 그룹핑
        Map<String, List<FitnessExerciseCategorySave>> grouped =
                records.stream()
                        .collect(Collectors.groupingBy(FitnessExerciseCategorySave::getSessionId));

        // 🔥 session별 칼로리 합산
        List<FitnessExerciseCategorySaveDto.DailyCaloriesResponse.SessionCalories> sessionList =
                grouped.entrySet().stream()
                        .map(entry -> {

                            // 🔥 세션 내 첫 번째 세트의 칼로리만 사용
                            double sessionCalories = entry.getValue().stream()
                                    .findFirst()
                                    .map(FitnessExerciseCategorySave::getCaloriesBurned)
                                    .orElse(0.0);

                            return FitnessExerciseCategorySaveDto.DailyCaloriesResponse.SessionCalories.builder()
                                    .sessionId(entry.getKey())
                                    .sessionCalories(sessionCalories)
                                    .build();
                        })
                        .toList();


        double dayTotal = sessionList.stream()
                .mapToDouble(FitnessExerciseCategorySaveDto.DailyCaloriesResponse.SessionCalories::getSessionCalories)
                .sum();

        return FitnessExerciseCategorySaveDto.DailyCaloriesResponse.builder()
                .date(date)
                .totalCalories(dayTotal)
                .sessions(sessionList)
                .build();
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

        // 🔥 운동이 저장된 날짜 기준으로 차감 (date 필드 사용)
        LocalDate progressDate = first.getDate();

        if (first.isSaved() && progressDate != null) {

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

                // 🔥 달성률 직접 재계산 (recalculateProgress 호출하면 다시 조회해서 덮어씀)
                ExerciseGoal goal = exerciseGoalRepository.findByUser(user).orElse(null);
                if (goal != null) {
                    long requiredSeconds = parseDurationToSeconds(goal.getDurationPerSession());
                    if (requiredSeconds > 0) {
                        double rate = Math.min(
                                ((double) progress.getTotalExerciseSeconds() / requiredSeconds) * 100.0,
                                100.0
                        );
                        progress.setExerciseRate(rate);
                    }
                } else {
                    progress.setExerciseRate(0.0);
                }

                dailyProgressRepository.save(progress);

                log.info("✔ DailyProgress 차감 userId={}, date={}, 차감={}초, 남은시간={}초, 달성률={}%",
                        user.getId(), progressDate, sessionSeconds, 
                        progress.getTotalExerciseSeconds(), progress.getExerciseRate());
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

    // 🔥 가라 kcal 계산 (공용 함수 재사용)
    double caloriesBurned = calculateCalories(met, sessionSeconds);

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
                            // ✔ 수정된 가라 칼로리
                            .caloriesBurned(caloriesBurned)
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
    LocalDate date = sessionRecords.get(0).getDate();  // date 필드 사용

    // ✅ 종목 기반 달성률 재계산
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

    /** MET 값과 초 단위 운동시간으로 가라 칼로리 계산 */
    private double calculateCalories(double met, long seconds) {
        double minutes = seconds / 60.0;
        return met * minutes * 0.8;
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

        // 2️⃣ sessionId 기준 그룹핑
        Map<String, List<FitnessExerciseCategorySave>> bySession =
                unsavedRows.stream()
                        .collect(Collectors.groupingBy(FitnessExerciseCategorySave::getSessionId));

        // 🔥 총 운동 시간(초) & 총 가라 칼로리 계산
        long totalSessionSeconds = 0L;
        double totalCaloriesBurned = 0.0;

        for (List<FitnessExerciseCategorySave> sessionList : bySession.values()) {
            if (sessionList.isEmpty()) continue;

            FitnessExerciseCategorySave rep = sessionList.get(0);

            // primitive 타입이기 때문에 null 체크 불필요
            long seconds = rep.getExerciseSeconds();   // long
            double met = (rep.getMet() != null)        // met는 Double일 가능성 높음
                    ? rep.getMet()
                    : resolveMet(rep.getCategory());

            totalSessionSeconds += seconds;

            // caloriesBurned도 primitive double 이라 null 체크 불가
            double sessionCalories = rep.getCaloriesBurned();

            // 혹시 0.0 이면 가라 로직으로 다시 계산하고 싶다면 이렇게 한 번 더 보정 가능
            if (sessionCalories == 0.0d) {
                sessionCalories = calculateCalories(met, seconds);
            }

            totalCaloriesBurned += sessionCalories;
        }


        // 🔥 인텐시티 스케일링 (0~100 → 1~5)
        Integer scaledIntensity = 3;
        Double rawScaled = calculateSessionIntensity(intensityList);
        if (rawScaled != null) {
            scaledIntensity = (int) Math.round(rawScaled);  // 1~5 범위로 이미 clamp 되어 있음
        }

        // 3️⃣ 저장 처리 (isSaved=true, date 세팅 등)
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

        // 7️⃣ AI 운동 요약 로그 전송 (/exercise/log)
        try {
            aiServerService.sendExerciseLog(
                    user.getUserId(),              // user_id (string)
                    date,                          // date
                    totalSessionSeconds / 60.0,    // duration_min (분)
                    totalCaloriesBurned,           // calories_burned
                    scaledIntensity                // intensity (1~5 정수, null 가능)
            );
        } catch (Exception e) {
            log.warn("⚠ AI exercise/log 전송 실패 userId={}, date={}, msg={}",
                    user.getUserId(), date, e.getMessage());
        }

        // 8️⃣ AI 피드백 전송 (/api/exercise-feedback 등 기존 로직)
        List<FitnessExerciseCategorySave> records =
                saveRepository.findByUserIdAndSessionIdIn(userId, response.getSessionIds());

        ExerciseFeedbackDto.Request req =
                buildExerciseFeedbackRequest(user, saveTitle, records, intensityList, feedbackList);

        aiServerService.sendExerciseFeedback(req);

        return response;
    }




    private ExerciseFeedbackDto.Request buildExerciseFeedbackRequest(
            User user,
            String saveTitle,
            List<FitnessExerciseCategorySave> records,
            List<Double> intensityList,
            List<String> feedbackList
    ) {

        // 운동 단위로 그룹핑 (sessionId + exerciseName)
        Map<String, List<FitnessExerciseCategorySave>> byExercise =
                records.stream()
                        .collect(Collectors.groupingBy(r -> r.getSessionId() + "::" + r.getExerciseName()));

        List<String> exerciseKeys = byExercise.keySet().stream().toList();
        List<ExerciseFeedbackDto.Item> items = new ArrayList<>();

        for (int i = 0; i < exerciseKeys.size(); i++) {
            String key = exerciseKeys.get(i);
            List<FitnessExerciseCategorySave> setList = byExercise.get(key);

            setList.sort(Comparator.comparingInt(s -> s.getSetNumber() != null ? s.getSetNumber() : 0));

            FitnessExerciseCategorySave last = setList.get(setList.size() - 1);

            // ✔ warmup 문자열 리스트로 변환
            List<String> warmupList =
                    setList.stream()
                            .filter(s -> !s.getId().equals(last.getId()))
                            .map(s -> s.getWeight() + "kg x " + s.getReps())
                            .collect(Collectors.toList());

            // ✔ 운동별 feedback → List<String>
            List<String> exerciseFeedback =
                    (feedbackList != null && feedbackList.size() > i)
                            ? List.of(feedbackList.get(i))
                            : List.of();

            items.add(
                    ExerciseFeedbackDto.Item.builder()
                            .exercise_id(last.getExternalId() != null ? last.getExternalId() : last.getExerciseName())
                            .name(last.getExerciseName())
                            .weight(last.getWeight())
                            .reps(last.getReps())
                            .sets(setList.size())
                            .warmup(warmupList)
                            .feedback(exerciseFeedback)  // ✔ 리스트로 전달
                            .build()
            );
        }

        // ✔ 세션 intensity 계산 → int 변환
        Integer sessionIntensity = null;
        if (intensityList != null && !intensityList.isEmpty()) {
            double avg = intensityList.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            sessionIntensity = (int) Math.round(scaleToOneToFive(avg));  // int로 변환
        }

        // ✔ 세션 전체 feedback 단일 string
        String sessionFeedback =
                (feedbackList != null && !feedbackList.isEmpty())
                        ? feedbackList.get(0)
                        : "neutral";

        return ExerciseFeedbackDto.Request.builder()
                .user_id(user.getUserId())
                .session_name(saveTitle)
                .duration_min(null)
                .intensity(sessionIntensity)   // ✔ int
                .feedback(sessionFeedback)     // ✔ session feedback
                .items(items)
                .build();
    }

    private Double calculateSessionIntensity(List<Double> intensityList) {

        if (intensityList == null || intensityList.isEmpty()) {
            return null;
        }

        double avg = intensityList.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        return scaleToOneToFive(avg);
    }

    private double scaleToOneToFive(double rawIntensity) {
        double scaled = 1 + (rawIntensity / 100.0) * 4.0;
        return Math.max(1.0, Math.min(5.0, scaled));
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
