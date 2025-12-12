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

        List<FitnessExerciseCategorySave> sessionRecords = saveRepository.findBySessionId(sessionId);
        if (sessionRecords.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "해당 세션 ID에 해당하는 기록이 없습니다.");
        }

        User user = sessionRecords.get(0).getUser();
        LocalDate workoutDate = sessionRecords.get(0).getWorkoutDate().toLocalDate();
        String externalId = sessionRecords.get(0).getExternalId();   // ✅ 운동 ID 추출

        saveRepository.deleteAll(sessionRecords);
        log.info("✅ 세션 ID={} 삭제 완료 ({}개 세트)", sessionId, sessionRecords.size());

        dailyProgressService.recalculateProgress(user, workoutDate);

        return FitnessExerciseCategorySaveDto.DeleteResponse.builder()
                .sessionId(sessionId)
                .externalId(externalId)
                .deletedCount(sessionRecords.size())
                .build();
    }
//
    public String addWorkoutSession(FitnessExerciseCategorySaveDto.CreateRequest request) {
        log.info("💪 운동 세션 추가 요청 - userId={}, exerciseId={}, exerciseName={}, sets={}",
                request.getUserId(), request.getExternalId(), request.getExerciseName(), request.getSets().size());

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String sessionId = "S-" + System.currentTimeMillis();


        List<FitnessExerciseCategorySave> entities = request.getSets().stream()
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
                        .build())
                .collect(Collectors.toList());

        saveRepository.saveAll(entities);
        log.info("✅ 세션ID={} 운동 '{}' 저장 완료 (미완료 상태)", sessionId, request.getExerciseName());

        LocalDate workoutDate = request.getWorkoutDate().toLocalDate();
        dailyProgressService.recalculateProgress(user, workoutDate);

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
        log.info("🔄 세션 완료 상태 토글 요청 - sessionId={}", sessionId);

        List<FitnessExerciseCategorySave> sessionRecords = saveRepository.findBySessionId(sessionId);
        if (sessionRecords.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "해당 세션 ID에 해당하는 기록이 없습니다.");
        }

        // 현재 상태 확인 (하나라도 미완료면 전체를 완료로, 모두 완료면 전체를 미완료로)
        boolean hasIncomplete = sessionRecords.stream().anyMatch(record -> !record.isCompleted());
        boolean newCompletionStatus = hasIncomplete;  // 미완료가 있으면 → 완료로, 모두 완료면 → 미완료로

        // 모든 세트의 완료 상태 변경
        sessionRecords.forEach(record -> record.setCompleted(newCompletionStatus));
        saveRepository.saveAll(sessionRecords);

        log.info("✅ 세션 ID={} 완료 상태 변경: {} ({}개 세트)", 
                sessionId, newCompletionStatus ? "완료" : "미완료", sessionRecords.size());

        // 달성률 재계산
        User user = sessionRecords.get(0).getUser();
        LocalDate workoutDate = sessionRecords.get(0).getWorkoutDate().toLocalDate();
        dailyProgressService.recalculateProgress(user, workoutDate);

        return FitnessExerciseCategorySaveDto.ToggleResponse.builder()
                .sessionId(sessionId)
                .completed(newCompletionStatus)
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
            LocalDate date,
            long seconds
    ) {
        // 🔥 user 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 원래 로직
        FitnessExerciseCategorySaveDto.SaveResponse response =
                saveUnsavedWorkouts(userId, saveTitle, date);

        if (response.getSessionIds() == null || response.getSessionIds().isEmpty()) {
            return response;
        }

        List<FitnessExerciseCategorySave> records =
                saveRepository.findByUserIdAndSessionIdIn(userId, response.getSessionIds());

        ExerciseFeedbackDto.Request req =
                buildExerciseFeedbackRequest(userId, saveTitle, records, intensityList, feedbackList);

        aiServerService.sendExerciseFeedback(req);

        // 🔥 DailyProgress 조회 혹은 생성 (user 사용)
        DailyProgress progress = dailyProgressRepository.findByUserIdAndDate(userId, date)
                .orElseGet(() -> dailyProgressRepository.save(
                        DailyProgress.builder()
                                .user(user)
                                .date(date)
                                .totalExerciseSeconds(0L)
                                .totalCalorie(0.0)
                                .exerciseRate(0.0)
                                .build()
                ));

        // 🔥 운동시간 누적
        progress.setTotalExerciseSeconds(progress.getTotalExerciseSeconds() + seconds);

        // 3) 목표 조회 (없어도 예외 던지지 않음)
        ExerciseGoal goal = exerciseGoalRepository.findByUser(user).orElse(null);

        double rate = 0.0;

        if (goal != null) {
            long requiredSeconds = parseDurationToSeconds(goal.getDurationPerSession());
            rate = ((double) progress.getTotalExerciseSeconds() / requiredSeconds) * 100.0;
            rate = Math.min(rate, 100.0);
        }

        // 4) 달성률 저장
        progress.setExerciseRate(rate);
        dailyProgressRepository.save(progress);


        log.info("🔥 DailyProgress 업데이트 완료: userId={}, seconds={}, rate={}",
                userId, progress.getTotalExerciseSeconds(), rate);

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

}
