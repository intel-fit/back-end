package rto.intelfit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rto.intelfit.domain.*;
import rto.intelfit.domain.User.Gender;
import rto.intelfit.dto.RecommendedExerciseDto;
import rto.intelfit.exception.BusinessException;
import rto.intelfit.exception.ErrorCode;
import rto.intelfit.repository.*;
import rto.intelfit.security.CustomUserPrincipal;
import java.time.LocalDateTime;
import rto.intelfit.repository.UserRecommendedExerciseRepository;
import rto.intelfit.domain.UserRecommendedExercise;
import rto.intelfit.dto.WeeklyRecommendedExerciseSaveDto;
import rto.intelfit.dto.TempExerciseSummaryDto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.HashMap;
/**
 * AI 기반 운동 추천 서비스
 * <p>
 * 주요 기능:
 * 1. 사용자 맞춤 운동 추천 생성 (AI 서버 연동)
 * 2. 식단 데이터 기반 운동 추천
 * 3. 추천 플랜 저장 및 관리
 * 4. 추천 플랜을 실제 운동 기록으로 적용
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExerciseRecommendationService {

    private final RecommendedExercisePlanRepository recommendedExercisePlanRepository;
    private final UserRepository userRepository;
    private final InBodyRepository inBodyRepository;
    private final MealRepository mealRepository;
    private final DailyNutritionGoalRepository dailyNutritionGoalRepository;
    private final ExerciseRepository exerciseRepository;
    private final AIServerClient aiServerClient;  // ✅ AIServerClient 사용
    private final UserRecommendedExerciseRepository userRecommendedExerciseRepository;
    private final TempExerciseSummaryRepository tempExerciseSummaryRepository;

    /**
     * ✅ AI 기반 맞춤 운동 추천 생성 (AI 서버 호출)
     */
    @Transactional
    public RecommendedExerciseDto.DailyRecommendationResponse generateDailyRecommendation(
            CustomUserPrincipal userPrincipal,
            RecommendedExerciseDto.DailyRecommendationRequest request
    ) {

        User user = getUserById(userPrincipal.getUserId());
        // ✅ FREE / PREMIUM + 토큰 / 일일 초기화 로직
        handleWorkoutRecommendToken(user);
        // 1) 최신 인바디 조회 (없으면 예외)
        // ⬅⬅⬅ 인바디 없으면 null 반환하도록 변경
        InBody latestInBody = inBodyRepository.findTopByUserOrderByMeasurementDateDesc(user)
                .orElse(null);

// 인바디 없으면 빈 프로필로 처리
        Map<String, Object> inbodyProfile = (latestInBody != null)
                ? buildInbodyProfile(latestInBody, user)
                : buildEmptyInbodyProfile();


        int age = calculateAge(user);
        String sex = mapSex(user.getGender());               // "male" / "female"
        String goal = mapGoal(user.getHealthGoal());         // "hypertrophy" / "fat_loss" 등
        String experience = mapExperience(request.getExperienceLevel()); // "beginner" / "intermediate" / "advanced"
        String environment = normalizeEnvironment(request.getEnvironment()); // "home" 또는 "gym"

        int planDays = 1; // 일일 추천이므로 1 고정
        int targetTimeMin = request.getTargetTimeMin() != null ? request.getTargetTimeMin() : 60;
        int weightKg = user.getWeight() != null ? user.getWeight() : 70;

        // 2) 인바디 정규화 → inbody 파라미터 조립


        // 3) AI 서버로 보낼 payload 구성
        Map<String, Object> payload = new HashMap<>();
        payload.put("user_id", user.getUserId());
        payload.put("age", age);
        payload.put("sex", sex);
        payload.put("goal", goal);
        payload.put("experience", experience);
        payload.put("environment", environment);
        payload.put("available_equipment", request.getAvailableEquipment());
        payload.put("health_conditions", request.getHealthConditions());
        payload.put("plan_days", planDays);
        payload.put("target_time_min", targetTimeMin);
        payload.put("weight_kg", weightKg);
        payload.put("inbody", inbodyProfile);
        // ⭐️ 추가된 라인
        payload.put("like_muscles", request.getLikeMuscles());

        log.info("🤖 AI 일일 운동 추천 요청 payload: {}", payload);

        // 4) AI 서버 호출
        Map<String, Object> aiResponse = aiServerClient.requestDailyExercisePlan(payload);

        log.info("🤖 AI 일일 운동 추천 응답: {}", aiResponse);

        String focus = (String) aiResponse.getOrDefault("focus", null);
        Map<String, Object> metrics = (Map<String, Object>) aiResponse.getOrDefault("metrics", Map.of());
        List<Map<String, Object>> exercises = (List<Map<String, Object>>) aiResponse.getOrDefault("exercises", List.of());
        double expectedDuration = 0;
        double expectedKcal = 0;

        if (metrics != null) {
            Object durObj = metrics.get("total_duration_min");
            Object kcalObj = metrics.get("total_kcal");

            expectedDuration = durObj instanceof Number ? ((Number) durObj).doubleValue() : 0;
            expectedKcal = kcalObj instanceof Number ? ((Number) kcalObj).doubleValue() : 0;
        }
        // ⬅⬅⬅ 여기 추가 : 오늘 날짜로 저장
        saveAiRecommendedExercises(user, LocalDate.now(), exercises);
        return RecommendedExerciseDto.DailyRecommendationResponse.builder()
                .success(true)
                .message("일일 운동 추천이 생성되었습니다")
                .focus(focus)
                .metrics(metrics)
                .exercises(exercises)
                .expectedDurationMin(expectedDuration)
                .expectedKcal(expectedKcal)
                .build();
    }

    @Transactional(readOnly = true)
    public TempExerciseSummaryDto getTempSummary(String userId, LocalDate date) {

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        TempExerciseSummary summary = tempExerciseSummaryRepository
                .findByUserAndDate(user, date)
                .orElse(null);

        if (summary == null) {
            return null;
        }

        return TempExerciseSummaryDto.builder()
                .date(summary.getDate().toString())
                .focus(summary.getFocus())
                .durationMin(summary.getDurationMin())
                .kcal(summary.getKcal())
                .exerciseCount(summary.getExerciseCount())
                .title(summary.getTitle())
                .build();
    }



    @Transactional
    public void saveWeeklyRecommendations(String userId, WeeklyRecommendedExerciseSaveDto dto) {

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        for (WeeklyRecommendedExerciseSaveDto.DayExercise day : dto.getDays()) {

            LocalDate date = LocalDate.parse(day.getDate());

            // 기존 데이터 삭제 (정책)
            userRecommendedExerciseRepository.deleteByUserAndExerciseDate(user, date);

            for (WeeklyRecommendedExerciseSaveDto.ExerciseItem ex : day.getExercises()) {

                UserRecommendedExercise entity = UserRecommendedExercise.builder()
                        .user(user)
                        .exerciseDate(date)
                        .exerciseId(ex.getExerciseId())
                        .name(ex.getName())
                        .target(ex.getTarget())
                        .build();

                userRecommendedExerciseRepository.save(entity);
            }

            log.info("📌 [{}] 날짜에 {}개 운동 저장 완료 (user={})",
                    date, day.getExercises().size(), userId);
        }
    }

    private Map<String, Object> buildEmptyInbodyProfile() {
        Map<String, Object> empty = new HashMap<>();

        empty.put("arms", Map.of(
                "muscle_score", 0,
                "fat_score", 0
        ));
        empty.put("chest", Map.of(
                "muscle_score", 0,
                "fat_score", 0
        ));
        empty.put("back", Map.of(
                "muscle_score", 0,
                "fat_score", 0
        ));
        empty.put("shoulders", Map.of(
                "muscle_score", 0,
                "fat_score", 0
        ));
        empty.put("legs", Map.of(
                "muscle_score", 0,
                "fat_score", 0
        ));
        empty.put("glutes", Map.of(
                "muscle_score", 0,
                "fat_score", 0
        ));
        empty.put("core", Map.of(
                "muscle_score", 0,
                "fat_score", 0
        ));

        return empty;
    }


    @Transactional
    public void saveAiRecommendedExercises(User user, LocalDate date, List<Map<String, Object>> exercises) {
        Map<String, Map<String, Object>> uniqueExercises = new LinkedHashMap<>();
        for (Map<String, Object> ex : exercises) {
            String exerciseId = (String) ex.get("exerciseId");
            String dedupKey = exerciseId != null
                    ? exerciseId
                    : (String.valueOf(ex.get("name")) + "|" + String.valueOf(ex.get("target")));
            uniqueExercises.putIfAbsent(dedupKey, ex);
        }

        List<UserRecommendedExercise> entities = new ArrayList<>();
        for (Map<String, Object> ex : uniqueExercises.values()) {
            String exerciseId = (String) ex.get("exerciseId");
            if (exerciseId == null || exerciseId.isBlank()) {
                log.warn("⚠️ exerciseId 가 없어 저장을 건너뜁니다 - userId={}, date={}, data={}",
                        user.getUserId(), date, ex);
                continue;
            }

            boolean alreadyExists = userRecommendedExerciseRepository
                    .existsByUserAndExerciseDateAndExerciseId(user, date, exerciseId);
            if (alreadyExists) {
                log.info("🚫 중복 운동 추천 감지 - userId={}, date={}, exerciseId={}", user.getUserId(), date, exerciseId);
                continue;
            }

            entities.add(UserRecommendedExercise.builder()
                    .user(user)
                    .exerciseDate(date)
                    .exerciseId(exerciseId)
                    .name((String) ex.get("name"))
                    .target((String) ex.get("target"))
                    .build());
        }

        if (!entities.isEmpty()) {
            userRecommendedExerciseRepository.saveAll(entities);
        }

        log.info("💾 AI 운동 추천 저장 요청 - userId={}, date={}, 입력={}, 신규 저장={}",
                user.getUserId(), date, exercises.size(), entities.size());
    }


    /**
     * FREE / PREMIUM 정책 + 토큰 초기화 + 토큰 차감
     */
    private void handleWorkoutRecommendToken(User user) {
        // ✅ PREMIUM 은 토큰 상관없이 바로 통과
        if (user.getMembershipType() == User.MembershipType.PREMIUM) {
            log.info("PREMIUM 사용자 - 운동 추천 토큰 소모 없이 진행, userId={}", user.getUserId());
            return;
        }

        // ✅ FREE 사용자는 날짜 기준 초기화 먼저
        resetWorkoutTokensIfNeeded(user);

        Integer tokens = user.getWorkoutRecommendTokens();
        if (tokens == null) {
            tokens = 0;
        }

        // 토큰이 0 이하면 예외
        if (tokens <= 0) {
            log.info("FREE 사용자 토큰 소진 - userId={}", user.getUserId());
            throw new BusinessException(
                    ErrorCode.NO_WORKOUT_TOKENS,
                    "오늘 사용 가능한 운동 추천 토큰이 모두 소진되었습니다."
            );
        }

        // 토큰 1개 차감
        user.setWorkoutRecommendTokens(tokens - 1);
        log.info("운동 추천 토큰 사용 - userId={}, before={}, after={}",
                user.getUserId(), tokens, tokens - 1);
        // @Transactional 이므로 별도 save() 없어도 flush 시점에 DB 반영
    }

    /**
     * 하루가 지나면 운동 추천 토큰 1로 초기화
     */
    private void resetWorkoutTokensIfNeeded(User user) {
        LocalDate today = LocalDate.now();
        LocalDateTime lastReset = user.getWorkoutRecommendLastReset();

        // 아직 한 번도 리셋한 적 없으면 → 오늘 기준으로 세팅 + 토큰 1
        if (lastReset == null) {
            user.setWorkoutRecommendTokens(1);
            user.setWorkoutRecommendLastReset(LocalDateTime.now());
            log.info("운동 추천 토큰 최초 초기화 - userId={}, tokens=1", user.getUserId());
            return;
        }

        LocalDate lastResetDate = lastReset.toLocalDate();

        // 날짜가 바뀐 경우(어제/그전) → 다시 1로 초기화
        if (!lastResetDate.isEqual(today)) {
            user.setWorkoutRecommendTokens(1);
            user.setWorkoutRecommendLastReset(LocalDateTime.now());
            log.info("운동 추천 토큰 일일 초기화 - userId={}, tokens=1, lastReset={}",
                    user.getUserId(), lastReset);
        }
    }

    /**
     * 인바디 엔티티를 AI 서버가 요구하는 inbody 파라미터 구조로 정규화.
     *
     *  - muscle_score: (실제 근육량 - 표준 근육량) / 표준 근육량
     *  - fat_score   : (실제 지방량 - 표준 지방량) / 표준 지방량
     */
    private Map<String, Object> buildInbodyProfile(InBody inBody, User user) {

        // 표준값은 대략적인 값으로 잡아두고, 나중에 인바디 기준에 맞춰 조정해도 됨.
        double armMuscleStd = 3.0;
        double legMuscleStd = 8.0;
        double trunkMuscleStd = 20.0;

        double armFatStd = 1.5;
        double legFatStd = 3.0;
        double trunkFatStd = 8.0;

        // 부위별 실제값 (kg 단위 평균)
        double armMuscle = avg(inBody.getLeftArmMuscle(), inBody.getRightArmMuscle());
        double legMuscle = avg(inBody.getLeftLegMuscle(), inBody.getRightLegMuscle());
        double trunkMuscle = safe(inBody.getTrunkMuscle());

        double armFat = avg(inBody.getLeftArmFat(), inBody.getRightArmFat());
        double legFat = avg(inBody.getLeftLegFat(), inBody.getRightLegFat());
        double trunkFat = safe(inBody.getTrunkFat());

        Map<String, Object> map = new HashMap<>();

        // arms
        map.put("arms", Map.of(
                "muscle_score", normalize(armMuscle, armMuscleStd),
                "fat_score", normalize(armFat, armFatStd)
        ));

        // chest
        map.put("chest", Map.of(
                "muscle_score", normalize(trunkMuscle, trunkMuscleStd),
                "fat_score", normalize(trunkFat, trunkFatStd)
        ));

        // back
        map.put("back", Map.of(
                "muscle_score", normalize(trunkMuscle, trunkMuscleStd),
                "fat_score", normalize(trunkFat, trunkFatStd)
        ));

        // shoulders
        map.put("shoulders", Map.of(
                "muscle_score", normalize(trunkMuscle, trunkMuscleStd),
                "fat_score", normalize(trunkFat, trunkFatStd)
        ));

        // legs
        map.put("legs", Map.of(
                "muscle_score", normalize(legMuscle, legMuscleStd),
                "fat_score", normalize(legFat, legFatStd)
        ));

        // glutes (엉덩이) → 다리와 동일 기준 사용
        map.put("glutes", Map.of(
                "muscle_score", normalize(legMuscle, legMuscleStd),
                "fat_score", normalize(legFat, legFatStd)
        ));

        // core → trunk 기준
        map.put("core", Map.of(
                "muscle_score", normalize(trunkMuscle, trunkMuscleStd),
                "fat_score", normalize(trunkFat, trunkFatStd)
        ));

        return map;
    }

    private double avg(BigDecimal a, BigDecimal b) {
        return (safe(a) + safe(b)) / 2.0;
    }

    private double safe(BigDecimal v) {
        return v == null ? 0.0 : v.doubleValue();
    }

    /**
     * (actual - standard) / standard  →  -1.0 ~ +1.0 근처 값
     */
    private double normalize(double value, double std) {
        if (std == 0) return 0.0;
        return (value - std) / std;
    }

    private String mapSex(User.Gender gender) {
        if (gender == null) return "male";
        return gender == User.Gender.M ? "male" : "female";
    }

    /**
     * User.HealthGoal → AI goal 문자열 매핑
     *  - DIET → "fat_loss"
     *  - MUSCLE_GAIN, BULK, LEAN_MASS → "hypertrophy"
     *  - 나머지 → "maintenance"
     */
    private String mapGoal(User.HealthGoal healthGoal) {

        if (healthGoal == null) {
            return "functional"; // 널일 경우 기본값
        }

        return switch (healthGoal) {
            case DIET -> "fat_loss";
            case BULK, MUSCLE_GAIN, LEAN_MASS -> "hypertrophy"; // 근비대 / 벌크업 계열
            case MAINTENANCE -> "functional"; // 유지 → 기능성 & 밸런스계 목표로 매핑
        };
    }


    /**
     * User.ExperienceLevel → "beginner" / "intermediate" / "advanced"
     */
    private String mapExperience(User.ExperienceLevel level) {
        if (level == null) return "beginner";

        return switch (level) {
            case BEGINNER -> "beginner";
            case INTERMEDIATE -> "intermediate";
            case ADVANCED -> "advanced";
        };
    }

    /**
     * 프론트에서 들어오는 environment를 AI 서버가 이해할 형태로 정리
     */
    private String normalizeEnvironment(String env) {
        if (env == null) return "gym";
        String lower = env.toLowerCase().trim();
        if (lower.contains("home") || lower.contains("집")) {
            return "home";
        }
        return "gym"; // 기본값
    }




    @Transactional
    public RecommendedExerciseDto.GenerateRecommendationResponse generateRecommendation(
            CustomUserPrincipal userPrincipal,
            LocalDate baseDate) {

        User user = getUserById(userPrincipal.getUserId());

        // 사용자 건강 정보 조회 (없으면 null)
        InBody latestInBody = inBodyRepository.findTopByUserOrderByMeasurementDateDesc(user)
                .orElse(null);

        // ✅ AI 서버 호출
        log.info("🤖 AI 서버에 운동 추천 요청 - 사용자: {}", user.getUserId());
        Map<String, Object> aiResponse = aiServerClient.generateExercisePlan(user, latestInBody);

        log.info("🤖 AI 서버 응답 수신: {}", aiResponse);

        // AI 응답을 RecommendedExercisePlan으로 변환
        RecommendedExercisePlan plan = convertAIResponseToPlan(user, aiResponse, baseDate);

        RecommendedExercisePlan savedPlan = recommendedExercisePlanRepository.save(plan);

        log.info("✅ AI 운동 추천 생성 완료 - 사용자: {}, 플랜 ID: {}",
                user.getUserId(), savedPlan.getId());

        return RecommendedExerciseDto.GenerateRecommendationResponse.builder()
                .success(true)
                .message("맞춤 운동 플랜이 생성되었습니다")
                .plan(RecommendedExerciseDto.ExercisePlanDetailResponse.from(savedPlan))
                .build();
    }

    public Map<LocalDate, List<RecommendedExerciseDto.ExerciseSimpleResponse>>
    getGroupedRecommendedExercises(CustomUserPrincipal principal) {

        User user = userRepository.findByUserId(principal.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        List<UserRecommendedExercise> list =
                userRecommendedExerciseRepository.findByUserOrderByExerciseDateAscCreatedAtAsc(user);

        // 날짜별 그룹핑
        return list.stream()
                .collect(Collectors.groupingBy(
                        UserRecommendedExercise::getExerciseDate,
                        Collectors.mapping(
                                RecommendedExerciseDto.ExerciseSimpleResponse::from,
                                Collectors.toList()
                        )
                ));
    }


    /**
     * ✅ AI 응답을 RecommendedExercisePlan 엔티티로 변환
     */
    private RecommendedExercisePlan convertAIResponseToPlan(
            User user,
            Map<String, Object> aiResponse,
            LocalDate baseDate) {

        String summary = (String) aiResponse.getOrDefault("summary", "AI 추천 운동 플랜");
        String goal = (String) aiResponse.getOrDefault("goal", "유지");
        
        // metrics에서 총 시간 계산
        Map<String, Object> metrics = (Map<String, Object>) aiResponse.get("metrics");
        Integer targetWeeklyMinutes = 240; // 기본값
        if (metrics != null && metrics.get("total_duration_min") != null) {
            targetWeeklyMinutes = ((Number) metrics.get("total_duration_min")).intValue();
        }

        RecommendedExercisePlan plan = RecommendedExercisePlan.builder()
                .user(user)
                .planName("AI 추천: " + goal + " 플랜")
                .description(summary)
                .targetWeeklyMinutes(targetWeeklyMinutes)
                .recommendationReason("AI 분석 기반 맞춤 추천")
                .isSaved(false)
                // ✅ 필수 필드 추가
                .fitnessGoal(user.getHealthGoal() != null
                        ? user.getHealthGoal()
                        : User.HealthGoal.MAINTENANCE)
                .targetLevel(user.getExperienceLevel() != null
                        ? user.getExperienceLevel()
                        : User.ExperienceLevel.BEGINNER)
                .weeklyFrequency(parseWorkoutDays(user.getWorkoutDaysPerWeek()))

                .estimatedDurationMinutes(60)
                .build();

        // AI 서버는 'plan' 배열을 반환 (routines가 아님)
        List<Map<String, Object>> planDays = (List<Map<String, Object>>) aiResponse.get("plan");
        if (planDays != null && !planDays.isEmpty()) {
            for (Map<String, Object> dayData : planDays) {
                // Rest 날은 건너뛰기
                String focus = (String) dayData.get("focus");
                if ("Rest".equalsIgnoreCase(focus)) {
                    continue;
                }
                
                RecommendedExerciseRoutine routine = convertAIDayToRoutine(dayData);
                if (routine != null) {
                    plan.addRoutine(routine);
                }
            }
        }
        
        // 루틴이 하나도 없으면 기본 루틴 생성
        if (plan.getRoutines().isEmpty()) {
            log.warn("⚠️ AI 응답에서 유효한 루틴을 찾을 수 없습니다. 기본 루틴 생성");
            createDefaultRoutine(plan, user);
        }

        return plan;
    }

    private RecommendedExerciseRoutine convertAIDayToRoutine(Map<String, Object> dayData) {
        return null;
    }

    private int parseWorkoutDays(Object value) {
        if (value == null) return 3; // default fallback

        String s = value.toString().replaceAll("[^0-9]", " ").trim();
        if (s.isEmpty()) return 3;

        // "3 4" → 첫 번째 숫자만 사용
        String[] arr = s.split("\\s+");
        try {
            return Integer.parseInt(arr[0]);
        } catch (Exception e) {
            return 3;
        }
    }

    /**
     * ✅ AI 루틴 데이터를 RecommendedExerciseRoutine 엔티티로 변환
     */
    private RecommendedExerciseRoutine convertAIRoutineToEntity(Map<String, Object> routineData) {
        String routineName = (String) routineData.getOrDefault("routine_name", "운동 루틴");
        String dayOfWeek = (String) routineData.getOrDefault("day_of_week", "매일");
        String categoryStr = (String) routineData.getOrDefault("category", "RESISTANCE");
        Integer duration = ((Number) routineData.getOrDefault("estimated_duration_minutes", 60)).intValue();

        Exercise.ExerciseCategory category;
        try {
            category = Exercise.ExerciseCategory.valueOf(categoryStr.toUpperCase());
        } catch (Exception e) {
            log.warn("⚠️ 알 수 없는 운동 카테고리: {}. RESISTANCE로 설정", categoryStr);
            category = Exercise.ExerciseCategory.RESISTANCE;
        }

        RecommendedExerciseRoutine routine = RecommendedExerciseRoutine.builder()
                .routineName(routineName)
                .dayOfWeek(dayOfWeek)
                .exerciseCategory(category)
                .estimatedDurationMinutes(duration)
                .build();

        // 운동 항목 변환
        List<Map<String, Object>> exercises = (List<Map<String, Object>>) routineData.get("exercises");
        if (exercises != null && !exercises.isEmpty()) {
            int order = 1;
            for (Map<String, Object> exerciseData : exercises) {
                RecommendedExerciseItem item = convertAIExerciseToItem(exerciseData, order++);
                routine.addItem(item);
            }
        }

        return routine;
    }

    /**
     * ✅ AI 운동 데이터를 RecommendedExerciseItem 엔티티로 변환
     */
    private RecommendedExerciseItem convertAIExerciseToItem(Map<String, Object> exerciseData, int order) {
        String type = (String) exerciseData.getOrDefault("type", "resistance");
        String description = (String) exerciseData.getOrDefault("description", "");

        RecommendedExerciseItem.RecommendedExerciseItemBuilder builder = RecommendedExerciseItem.builder()
                .description(description)
                .exerciseOrder(order);

        if ("cardio".equalsIgnoreCase(type)) {
            // 유산소 운동
            String cardioTypeStr = (String) exerciseData.get("cardio_type");
            if (cardioTypeStr != null) {
                try {
                    builder.cardioType(Exercise.CardioType.valueOf(cardioTypeStr.toUpperCase()));
                } catch (Exception e) {
                    log.warn("⚠️ 알 수 없는 유산소 타입: {}. TREADMILL로 설정", cardioTypeStr);
                    builder.cardioType(Exercise.CardioType.TREADMILL);
                }
            }

            if (exerciseData.get("target_distance") != null) {
                builder.targetDistance(new BigDecimal(exerciseData.get("target_distance").toString()));
            }
            if (exerciseData.get("target_duration_minutes") != null) {
                builder.targetDurationMinutes(((Number) exerciseData.get("target_duration_minutes")).intValue());
            }
            if (exerciseData.get("target_calories_burn") != null) {
                builder.targetCaloriesBurn(new BigDecimal(exerciseData.get("target_calories_burn").toString()));
            }

        } else {
            // 무산소 운동 (resistance)
            String exerciseTypeStr = (String) exerciseData.get("exercise_type");
            if (exerciseTypeStr != null) {
                try {
                    builder.resistanceExerciseType(
                            Exercise.ResistanceExerciseType.valueOf(exerciseTypeStr.toUpperCase()));
                } catch (Exception e) {
                    log.warn("⚠️ 알 수 없는 무산소 타입: {}. BARBELL_SQUAT로 설정", exerciseTypeStr);
                    builder.resistanceExerciseType(Exercise.ResistanceExerciseType.BARBELL_SQUAT);
                }
            }

            String muscleGroupStr = (String) exerciseData.get("muscle_group");
            if (muscleGroupStr != null) {
                try {
                    builder.muscleGroup(Exercise.MuscleGroup.valueOf(muscleGroupStr.toUpperCase()));
                } catch (Exception e) {
                    log.warn("⚠️ 알 수 없는 근육 부위: {}. LEGS로 설정", muscleGroupStr);
                    builder.muscleGroup(Exercise.MuscleGroup.LEGS);
                }
            }

            if (exerciseData.get("recommended_sets") != null) {
                builder.recommendedSets(((Number) exerciseData.get("recommended_sets")).intValue());
            }
            builder.recommendedReps((String) exerciseData.get("recommended_reps"));
            builder.recommendedWeight((String) exerciseData.get("recommended_weight"));
            if (exerciseData.get("recommended_rest_seconds") != null) {
                builder.recommendedRestSeconds(((Number) exerciseData.get("recommended_rest_seconds")).intValue());
            }
        }

        return builder.build();
    }

    /**
     * ✅ AI 응답이 없거나 불완전할 때 기본 루틴 생성
     */
    private void createDefaultRoutine(RecommendedExercisePlan plan, User user) {
        if (user.getHealthGoal() == User.HealthGoal.DIET) {
            createDietRoutines(plan);
        } else if (user.getHealthGoal() == User.HealthGoal.MUSCLE_GAIN) {
            createMuscleGainRoutines(plan);
        } else {
            createMaintenanceRoutines(plan);
        }
    }

    /**
     * 식단 연동 운동 추천 생성
     */
    @Transactional
    public RecommendedExerciseDto.MealBasedRecommendationResponse generateMealBasedRecommendation(
            CustomUserPrincipal userPrincipal,
            LocalDate mealDate) {

        User user = getUserById(userPrincipal.getUserId());

        // 해당 날짜의 식단 데이터 조회
        List<Meal> meals = mealRepository.findByUserAndMealDate(user, mealDate);

        if (meals.isEmpty()) {
            throw new BusinessException(ErrorCode.MEAL_NOT_FOUND,
                    "해당 날짜의 식단 기록이 없습니다");
        }

        // 총 섭취 칼로리 계산
        BigDecimal totalIntakeCalories = meals.stream()
                .map(Meal::getTotalCalories)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 목표 칼로리 조회
        DailyNutritionGoal nutritionGoal = dailyNutritionGoalRepository
                .findByUser(user)
                .orElse(null);

        BigDecimal targetCalories = nutritionGoal != null
                ? nutritionGoal.getTargetCalories()
                : calculateDefaultTargetCalories(user);

        // 초과 칼로리 계산
        BigDecimal excessCalories = totalIntakeCalories.subtract(targetCalories);

        // 권장 칼로리 소모량 계산
        BigDecimal recommendedCaloriesBurn = excessCalories.compareTo(BigDecimal.ZERO) > 0
                ? excessCalories.multiply(BigDecimal.valueOf(1.2)) // 20% 여유분
                : BigDecimal.ZERO;

        // 식단 분석 정보 생성
        RecommendedExerciseDto.MealAnalysisInfo mealAnalysis = createMealAnalysisInfo(
                mealDate, totalIntakeCalories, targetCalories, excessCalories, recommendedCaloriesBurn);

        // 식단 기반 운동 추천 플랜 생성
        InBody latestInBody = inBodyRepository.findTopByUserOrderByMeasurementDateDesc(user)
                .orElse(null);

        RecommendedExercisePlan plan = createMealBasedRecommendedPlan(
                user, latestInBody, mealDate, recommendedCaloriesBurn);

        RecommendedExercisePlan savedPlan = recommendedExercisePlanRepository.save(plan);

        log.info("식단 연동 운동 추천 생성 완료 - 사용자: {}, 플랜 ID: {}, 초과 칼로리: {}kcal",
                user.getUserId(), savedPlan.getId(), excessCalories);

        return RecommendedExerciseDto.MealBasedRecommendationResponse.builder()
                .success(true)
                .message("식단 기반 운동 플랜이 생성되었습니다")
                .mealAnalysis(mealAnalysis)
                .plan(RecommendedExerciseDto.ExercisePlanDetailResponse.from(savedPlan))
                .build();
    }

    /**
     * 저장된 추천 플랜 목록 조회
     */
    public RecommendedExerciseDto.SavedPlansResponse getSavedPlans(CustomUserPrincipal userPrincipal) {
        User user = getUserById(userPrincipal.getUserId());

        List<RecommendedExercisePlan> plans = recommendedExercisePlanRepository
                .findByUserAndIsSavedTrueOrderByCreatedAtDesc(user);

        List<RecommendedExerciseDto.ExercisePlanSummary> planSummaries = plans.stream()
                .map(RecommendedExerciseDto.ExercisePlanSummary::from)
                .collect(Collectors.toList());

        return RecommendedExerciseDto.SavedPlansResponse.builder()
                .plans(planSummaries)
                .totalCount(planSummaries.size())
                .build();
    }

    public List<UserRecommendedExercise> getRecommendedExercises(CustomUserPrincipal principal) {

        User user = userRepository.findByUserId(principal.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return userRecommendedExerciseRepository.findByUserOrderByCreatedAtDesc(user);
    }

    /**
     * 특정 추천 플랜 상세 조회
     */
    public RecommendedExerciseDto.ExercisePlanDetailResponse getPlanDetail(
            CustomUserPrincipal userPrincipal,
            Long planId) {

        RecommendedExercisePlan plan = getPlanByIdAndUserId(planId, userPrincipal.getUserId());

        return RecommendedExerciseDto.ExercisePlanDetailResponse.from(plan);
    }

    /**
     * 추천 플랜 저장
     */
    @Transactional
    public RecommendedExerciseDto.SavePlanResponse savePlan(
            CustomUserPrincipal userPrincipal,
            Long planId) {

        RecommendedExercisePlan plan = getPlanByIdAndUserId(planId, userPrincipal.getUserId());

        plan.setIsSaved(true);

        log.info("추천 플랜 저장 완료 - 사용자: {}, 플랜 ID: {}",
                userPrincipal.getUserId(), planId);

        return RecommendedExerciseDto.SavePlanResponse.builder()
                .success(true)
                .message("추천 플랜이 저장되었습니다")
                .planId(planId)
                .build();
    }

    /** 2
     * 추천 플랜 삭제
     */
    @Transactional
    public void deletePlan(CustomUserPrincipal userPrincipal, Long planId) {
        RecommendedExercisePlan plan = getPlanByIdAndUserId(planId, userPrincipal.getUserId());

        recommendedExercisePlanRepository.delete(plan);

        log.info("추천 플랜 삭제 완료 - 사용자: {}, 플랜 ID: {}",
                userPrincipal.getUserId(), planId);
    }

    /**
     * 추천 플랜을 실제 운동 기록으로 적용
     */
    @Transactional
    public RecommendedExerciseDto.ApplyPlanResponse applyPlan(
            CustomUserPrincipal userPrincipal,
            Long planId,
            LocalDate exerciseDate) {

        RecommendedExercisePlan plan = getPlanByIdAndUserId(planId, userPrincipal.getUserId());
        User user = plan.getUser();

        List<Long> createdExerciseIds = new ArrayList<>();

        // 각 루틴을 실제 운동 기록으로 변환
        for (RecommendedExerciseRoutine routine : plan.getRoutines()) {
            Exercise exercise = convertRoutineToExercise(routine, user, exerciseDate);
            Exercise savedExercise = exerciseRepository.save(exercise);
            createdExerciseIds.add(savedExercise.getId());
        }

        log.info("추천 플랜 적용 완료 - 사용자: {}, 플랜 ID: {}, 생성된 운동 수: {}",
                userPrincipal.getUserId(), planId, createdExerciseIds.size());

        return RecommendedExerciseDto.ApplyPlanResponse.builder()
                .success(true)
                .message("추천 플랜이 운동 기록으로 저장되었습니다")
                .createdExerciseIds(createdExerciseIds)
                .build();
    }

    // ========== Private Helper Methods ==========

    private User getUserById(String userId) {
        return userRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private RecommendedExercisePlan getPlanByIdAndUserId(Long planId, String userId) {
        RecommendedExercisePlan plan = recommendedExercisePlanRepository.findById(planId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RECOMMENDED_EXERCISE_PLAN_NOT_FOUND,
                        "해당 추천 플랜을 찾을 수 없습니다"));

        if (!plan.getUser().getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.RECOMMENDED_EXERCISE_PLAN_ACCESS_DENIED,
                    "해당 추천 플랜에 접근할 권한이 없습니다");
        }

        return plan;
    }

    /**
     * 식단 기반 추천 운동 플랜 생성
     */
    private RecommendedExercisePlan createMealBasedRecommendedPlan(
            User user,
            InBody inBody,
            LocalDate mealDate,
            BigDecimal recommendedCaloriesBurn) {

        String planName = "식단 조절을 위한 칼로리 소모 플랜";
        String description = String.format("목표 칼로리 소모: %.0fkcal", recommendedCaloriesBurn.doubleValue());

        RecommendedExercisePlan plan = RecommendedExercisePlan.builder()
                .user(user)
                .planName(planName)
                .description(description)
                .targetWeeklyMinutes(calculateMinutesFromCalories(recommendedCaloriesBurn))
                .recommendationReason("오늘 섭취한 칼로리를 고려한 맞춤 운동 플랜입니다")
                .isSaved(false)
                .fitnessGoal(user.getHealthGoal())
                .targetLevel(user.getExperienceLevel())
                .weeklyFrequency(parseWorkoutDays(user.getWorkoutDaysPerWeek()))
                .estimatedDurationMinutes(calculateMinutesFromCalories(recommendedCaloriesBurn))
                .build();

        // 칼로리 소모 중심 유산소 루틴 생성
        createCalorieBurnRoutine(plan, recommendedCaloriesBurn);

        return plan;
    }

    /**
     * 다이어트 목표 루틴 생성
     */
    private void createDietRoutines(RecommendedExercisePlan plan) {
        RecommendedExerciseRoutine cardioRoutine = RecommendedExerciseRoutine.builder()
                .routineName("유산소 집중 데이")
                .dayOfWeek("월요일, 수요일, 금요일")
                .exerciseCategory(Exercise.ExerciseCategory.CARDIO)
                .estimatedDurationMinutes(45)
                .build();

        cardioRoutine.addItem(RecommendedExerciseItem.builder()
                .cardioType(Exercise.CardioType.TREADMILL)
                .targetDistance(new BigDecimal("5.0"))
                .targetDurationMinutes(30)
                .targetCaloriesBurn(new BigDecimal("300"))
                .description("중강도 러닝으로 지방 연소")
                .exerciseOrder(1)
                .build());

        plan.addRoutine(cardioRoutine);
    }

    /**
     * 근육 증가 목표 루틴 생성
     */
    private void createMuscleGainRoutines(RecommendedExercisePlan plan) {
        RecommendedExerciseRoutine upperBodyRoutine = RecommendedExerciseRoutine.builder()
                .routineName("상체 집중 데이")
                .dayOfWeek("월요일, 목요일")
                .exerciseCategory(Exercise.ExerciseCategory.RESISTANCE)
                .estimatedDurationMinutes(75)
                .build();

        upperBodyRoutine.addItem(RecommendedExerciseItem.builder()
                .resistanceExerciseType(Exercise.ResistanceExerciseType.BENCH_PRESS)
                .muscleGroup(Exercise.MuscleGroup.CHEST)
                .recommendedSets(4)
                .recommendedReps("6-8")
                .recommendedWeight("1RM의 80-85%")
                .recommendedRestSeconds(120)
                .description("가슴 근육 발달을 위한 고중량 훈련")
                .exerciseOrder(1)
                .build());

        plan.addRoutine(upperBodyRoutine);
    }

    /**
     * 유지 목표 루틴 생성
     */
    private void createMaintenanceRoutines(RecommendedExercisePlan plan) {
        RecommendedExerciseRoutine mixedRoutine = RecommendedExerciseRoutine.builder()
                .routineName("균형 잡힌 전신 운동")
                .dayOfWeek("월요일, 수요일, 금요일")
                .exerciseCategory(Exercise.ExerciseCategory.RESISTANCE)
                .estimatedDurationMinutes(60)
                .build();

        mixedRoutine.addItem(RecommendedExerciseItem.builder()
                .resistanceExerciseType(Exercise.ResistanceExerciseType.BARBELL_SQUAT)
                .muscleGroup(Exercise.MuscleGroup.LEGS)
                .recommendedSets(3)
                .recommendedReps("10-12")
                .recommendedWeight("1RM의 70-75%")
                .recommendedRestSeconds(90)
                .description("전신 근력 유지")
                .exerciseOrder(1)
                .build());

        plan.addRoutine(mixedRoutine);
    }

    /**
     * 칼로리 소모 중심 루틴 생성
     */
    private void createCalorieBurnRoutine(RecommendedExercisePlan plan, BigDecimal targetCalories) {
        int minutes = calculateMinutesFromCalories(targetCalories);

        RecommendedExerciseRoutine routine = RecommendedExerciseRoutine.builder()
                .routineName("칼로리 소모 집중 운동")
                .dayOfWeek("오늘")
                .exerciseCategory(Exercise.ExerciseCategory.CARDIO)
                .estimatedDurationMinutes(minutes)
                .build();

        routine.addItem(RecommendedExerciseItem.builder()
                .cardioType(Exercise.CardioType.TREADMILL)
                .targetDurationMinutes(minutes)
                .targetCaloriesBurn(targetCalories)
                .description("초과 섭취 칼로리 소모를 위한 유산소 운동")
                .exerciseOrder(1)
                .build());

        plan.addRoutine(routine);
    }

    private BigDecimal calculateDefaultTargetCalories(User user) {
        int age = calculateAge(user);
        BigDecimal bmr;

        if (user.getGender() == Gender.M) {
            bmr = BigDecimal.valueOf(66)
                    .add(BigDecimal.valueOf(13.7).multiply(BigDecimal.valueOf(user.getWeight())))
                    .add(BigDecimal.valueOf(5).multiply(BigDecimal.valueOf(user.getHeight())))
                    .subtract(BigDecimal.valueOf(6.8).multiply(BigDecimal.valueOf(age)));
        } else {
            bmr = BigDecimal.valueOf(655)
                    .add(BigDecimal.valueOf(9.6).multiply(BigDecimal.valueOf(user.getWeight())))
                    .add(BigDecimal.valueOf(1.8).multiply(BigDecimal.valueOf(user.getHeight())))
                    .subtract(BigDecimal.valueOf(4.7).multiply(BigDecimal.valueOf(age)));
        }

        return bmr.multiply(BigDecimal.valueOf(1.55)).setScale(0, RoundingMode.HALF_UP);
    }

    private int calculateAge(User user) {
        if (user.getBirthDate() == null) return 30;
        return java.time.Period.between(user.getBirthDate(), LocalDate.now()).getYears();
    }

    private RecommendedExerciseDto.MealAnalysisInfo createMealAnalysisInfo(
            LocalDate analysisDate,
            BigDecimal totalCalories,
            BigDecimal targetCalories,
            BigDecimal excessCalories,
            BigDecimal recommendedCaloriesBurn) {

        String message;
        if (excessCalories.compareTo(BigDecimal.ZERO) > 0) {
            message = String.format("목표 대비 %.0fkcal 초과 섭취하셨습니다. " +
                            "약 %.0fkcal의 칼로리 소모를 권장합니다.",
                    excessCalories.doubleValue(),
                    recommendedCaloriesBurn.doubleValue());
        } else {
            message = "목표 칼로리 범위 내에서 섭취하셨습니다.";
        }

        return RecommendedExerciseDto.MealAnalysisInfo.builder()
                .analysisDate(analysisDate)
                .totalCalories(totalCalories)
                .targetCalories(targetCalories)
                .excessCalories(excessCalories)
                .recommendedCaloriesBurn(recommendedCaloriesBurn)
                .analysisMessage(message)
                .build();
    }

    private int calculateMinutesFromCalories(BigDecimal calories) {
        return calories
                .divide(BigDecimal.valueOf(500), 2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(60))
                .intValue();
    }

    private Exercise convertRoutineToExercise(
            RecommendedExerciseRoutine routine,
            User user,
            LocalDate exerciseDate) {

        Exercise exercise = Exercise.builder()
                .user(user)
                .exerciseDate(exerciseDate)
                .exerciseCategory(routine.getExerciseCategory())
                .totalDurationMinutes(routine.getEstimatedDurationMinutes())
                .memo("추천 플랜에서 생성됨: " + routine.getRoutineName())
                .build();

        for (RecommendedExerciseItem item : routine.getItems()) {
            ExerciseSet set = convertItemToExerciseSet(item);
            exercise.addExerciseSet(set);
        }

        return exercise;
    }

    private ExerciseSet convertItemToExerciseSet(RecommendedExerciseItem item) {
        ExerciseSet.ExerciseSetBuilder builder = ExerciseSet.builder();

        if (item.isCardio()) {
            builder.cardioType(item.getCardioType())
                    .distance(item.getTargetDistance())
                    .durationMinutes(item.getTargetDurationMinutes())
                    .caloriesBurned(item.getTargetCaloriesBurn());
        }

        if (item.isResistance()) {
            builder.resistanceExerciseType(item.getResistanceExerciseType())
                    .muscleGroup(item.getMuscleGroup())
                    .setNumber(1)
                    .restSeconds(item.getRecommendedRestSeconds());

            if (item.getRecommendedWeight() != null) {
                try {
                    String[] parts = item.getRecommendedWeight().split("-");
                    if (parts.length > 0) {
                        builder.weight(new BigDecimal(parts[0].replaceAll("[^0-9.]", "")));
                    }
                } catch (Exception e) {
                    log.warn("무게 파싱 실패: {}", item.getRecommendedWeight());
                }
            }

            if (item.getRecommendedReps() != null) {
                try {
                    String[] parts = item.getRecommendedReps().split("-");
                    if (parts.length > 0) {
                        builder.reps(Integer.parseInt(parts[0].replaceAll("[^0-9]", "")));
                    }
                } catch (Exception e) {
                    log.warn("반복수 파싱 실패: {}", item.getRecommendedReps());
                }
            }
        }

        builder.memo(item.getDescription());

        return builder.build();
    }
}
