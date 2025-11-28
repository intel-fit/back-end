package rto.intelfit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rto.intelfit.domain.*;
import rto.intelfit.dto.BadgeDto;
import rto.intelfit.exception.BusinessException;
import rto.intelfit.exception.ErrorCode;
import rto.intelfit.repository.*;
import rto.intelfit.security.CustomUserPrincipal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 뱃지 서비스
 * 
 * 주요 기능:
 * 1. 뱃지 획득 조건 체크 및 자동 부여
 * 2. 사용자 뱃지 목록 조회
 * 3. 뱃지 진행 상황 조회
 * 4. 마이페이지 표시 뱃지 관리
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BadgeService {

    private final UserRepository userRepository;
    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final ExerciseRepository exerciseRepository;
    private final MealRepository mealRepository;
    private final InBodyRepository inBodyRepository;

    /**
     * 사용자의 모든 뱃지 조회 (획득/미획득 포함)
     */
    public BadgeDto.UserBadgesResponse getUserBadges(CustomUserPrincipal userPrincipal) {
        User user = getUserById(userPrincipal.getUserId());

        // 모든 뱃지 조회
        List<Badge> allBadges = badgeRepository.findAllByOrderByDisplayOrderAsc();

        // 사용자가 획득한 뱃지 조회
        List<UserBadge> userBadges = userBadgeRepository.findByUserOrderByEarnedAtDesc(user);
        Map<Long, UserBadge> userBadgeMap = userBadges.stream()
                .collect(Collectors.toMap(ub -> ub.getBadge().getId(), ub -> ub));

        // 뱃지 분류
        List<BadgeDto.BadgeInfo> earnedBadges = new ArrayList<>();
        List<BadgeDto.BadgeInfo> notEarnedBadges = new ArrayList<>();

        for (Badge badge : allBadges) {
            UserBadge userBadge = userBadgeMap.get(badge.getId());
            BadgeDto.BadgeInfo badgeInfo = BadgeDto.BadgeInfo.from(badge, userBadge);

            if (userBadge != null) {
                earnedBadges.add(badgeInfo);
            } else {
                notEarnedBadges.add(badgeInfo);
            }
        }

        // 카테고리별 개수 계산
        int exerciseCount = countBadgesByCategory(earnedBadges, "EXERCISE");
        int mealCount = countBadgesByCategory(earnedBadges, "MEAL");
        int weightCount = countBadgesByCategory(earnedBadges, "WEIGHT");

        double achievementRate = allBadges.isEmpty() ? 0.0 :
                (double) earnedBadges.size() / allBadges.size() * 100;

        return BadgeDto.UserBadgesResponse.builder()
                .earnedBadges(earnedBadges)
                .notEarnedBadges(notEarnedBadges)
                .totalBadges(allBadges.size())
                .earnedCount(earnedBadges.size())
                .achievementRate(Math.round(achievementRate * 10.0) / 10.0)
                .exerciseBadgeCount(exerciseCount)
                .mealBadgeCount(mealCount)
                .weightBadgeCount(weightCount)
                .build();
    }

    /**
     * 마이페이지 표시용 뱃지 조회 (최대 3개)
     */
    public BadgeDto.DisplayBadgesResponse getDisplayBadges(CustomUserPrincipal userPrincipal) {
        User user = getUserById(userPrincipal.getUserId());

        // 표시할 뱃지 조회 (표시 순서대로, 최대 3개)
        List<UserBadge> displayBadges = userBadgeRepository
                .findByUserAndIsDisplayedTrueOrderByDisplayOrderAsc(user);

        // 최대 3개만 선택
        List<BadgeDto.BadgeInfo> badgeInfos = displayBadges.stream()
                .limit(3)
                .map(ub -> BadgeDto.BadgeInfo.from(ub.getBadge(), ub))
                .collect(Collectors.toList());

        // 총 획득 뱃지 수
        long totalCount = userBadgeRepository.countByUser(user);

        // 메시지 생성
        String message = createDisplayBadgeMessage(badgeInfos, totalCount);

        return BadgeDto.DisplayBadgesResponse.builder()
                .badges(badgeInfos)
                .totalEarnedCount((int) totalCount)
                .message(message)
                .build();
    }

    /**
     * 뱃지 통계 조회
     */
    public BadgeDto.BadgeStatistics getBadgeStatistics(CustomUserPrincipal userPrincipal) {
        User user = getUserById(userPrincipal.getUserId());

        List<Badge> allBadges = badgeRepository.findAllByOrderByDisplayOrderAsc();
        List<UserBadge> earnedBadges = userBadgeRepository.findByUserOrderByEarnedAtDesc(user);

        // 카테고리별 통계
        Map<String, Integer> categoryCount = new HashMap<>();
        categoryCount.put("exercise", 0);
        categoryCount.put("meal", 0);
        categoryCount.put("weight", 0);
        categoryCount.put("overall", 0);

        for (UserBadge ub : earnedBadges) {
            String badgeType = ub.getBadge().getBadgeType().name();
            if (badgeType.startsWith("EXERCISE")) {
                categoryCount.put("exercise", categoryCount.get("exercise") + 1);
            } else if (badgeType.startsWith("MEAL") || badgeType.startsWith("CALORIE") || badgeType.startsWith("HEALTHY")) {
                categoryCount.put("meal", categoryCount.get("meal") + 1);
            } else if (badgeType.startsWith("WEIGHT") || badgeType.startsWith("MUSCLE") || badgeType.startsWith("BODY")) {
                categoryCount.put("weight", categoryCount.get("weight") + 1);
            } else {
                categoryCount.put("overall", categoryCount.get("overall") + 1);
            }
        }

        // 최근 획득 뱃지 (최대 3개)
        List<BadgeDto.BadgeInfo> recentBadges = userBadgeRepository
                .findTop3ByUserOrderByEarnedAtDesc(user).stream()
                .map(ub -> BadgeDto.BadgeInfo.from(ub.getBadge(), ub))
                .collect(Collectors.toList());

        double achievementRate = allBadges.isEmpty() ? 0.0 :
                (double) earnedBadges.size() / allBadges.size() * 100;

        return BadgeDto.BadgeStatistics.builder()
                .totalBadges(allBadges.size())
                .earnedBadges(earnedBadges.size())
                .notEarnedBadges(allBadges.size() - earnedBadges.size())
                .achievementRate(Math.round(achievementRate * 10.0) / 10.0)
                .categoryStats(BadgeDto.CategoryStatistics.builder()
                        .exercise(categoryCount.get("exercise"))
                        .meal(categoryCount.get("meal"))
                        .weight(categoryCount.get("weight"))
                        .overall(categoryCount.get("overall"))
                        .build())
                .recentBadges(recentBadges)
                .build();
    }

    /**
     * 뱃지 획득 조건 체크 및 자동 부여
     */
    @Transactional
    public List<BadgeDto.NewBadgeNotification> checkAndAwardBadges(User user) {
        List<BadgeDto.NewBadgeNotification> newBadges = new ArrayList<>();

        // 운동 관련 뱃지 체크
        newBadges.addAll(checkExerciseBadges(user));

        // 식단 관련 뱃지 체크
        newBadges.addAll(checkMealBadges(user));

        // 체중 관련 뱃지 체크
        newBadges.addAll(checkWeightBadges(user));

        return newBadges;
    }

    /**
     * 특정 뱃지 수동 부여 (관리자용)
     */
    @Transactional
    public BadgeDto.NewBadgeNotification awardBadge(
            CustomUserPrincipal userPrincipal,
            Badge.BadgeType badgeType) {

        User user = getUserById(userPrincipal.getUserId());
        Badge badge = badgeRepository.findByBadgeType(badgeType)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                        "해당 뱃지를 찾을 수 없습니다"));

        // 이미 획득한 뱃지인지 확인
        if (userBadgeRepository.existsByUserAndBadge(user, badge)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
                    "이미 획득한 뱃지입니다");
        }

        // 뱃지 부여
        UserBadge userBadge = UserBadge.builder()
                .user(user)
                .badge(badge)
                .isDisplayed(true)
                .displayOrder(getNextDisplayOrder(user))
                .build();

        userBadgeRepository.save(userBadge);

        log.info("뱃지 부여 완료 - 사용자: {}, 뱃지: {}",
                user.getUserId(), badge.getBadgeType());

        return BadgeDto.NewBadgeNotification.builder()
                .success(true)
                .message("축하합니다! 새로운 뱃지를 획득했습니다! 🎉")
                .badge(BadgeDto.BadgeInfo.from(badge, userBadge))
                .earnedAt(userBadge.getEarnedAt())
                .build();
    }

    // ========== Private Helper Methods ==========

    private User getUserById(String userId) {
        return userRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private List<BadgeDto.NewBadgeNotification> checkExerciseBadges(User user) {
        List<BadgeDto.NewBadgeNotification> newBadges = new ArrayList<>();

        // 총 운동 횟수
        long totalExercises = exerciseRepository.countByUser(user);

        // 운동 횟수 뱃지 체크
        if (totalExercises >= 100) {
            newBadges.add(tryAwardBadge(user, Badge.BadgeType.EXERCISE_100));
        } else if (totalExercises >= 50) {
            newBadges.add(tryAwardBadge(user, Badge.BadgeType.EXERCISE_50));
        } else if (totalExercises >= 30) {
            newBadges.add(tryAwardBadge(user, Badge.BadgeType.EXERCISE_30));
        } else if (totalExercises >= 10) {
            newBadges.add(tryAwardBadge(user, Badge.BadgeType.EXERCISE_10));
        }

        // 연속 운동 일수 체크 (향후 구현 가능)

        return newBadges.stream().filter(Objects::nonNull).collect(Collectors.toList());
    }

    private List<BadgeDto.NewBadgeNotification> checkMealBadges(User user) {
        List<BadgeDto.NewBadgeNotification> newBadges = new ArrayList<>();

        // 총 식단 기록 횟수
        long totalMeals = mealRepository.countByUser(user);

        // 식단 기록 횟수 뱃지 체크
        if (totalMeals >= 100) {
            newBadges.add(tryAwardBadge(user, Badge.BadgeType.MEAL_100));
        } else if (totalMeals >= 50) {
            newBadges.add(tryAwardBadge(user, Badge.BadgeType.MEAL_50));
        } else if (totalMeals >= 30) {
            newBadges.add(tryAwardBadge(user, Badge.BadgeType.MEAL_30));
        } else if (totalMeals >= 10) {
            newBadges.add(tryAwardBadge(user, Badge.BadgeType.MEAL_10));
        }

        return newBadges.stream().filter(Objects::nonNull).collect(Collectors.toList());
    }

    private List<BadgeDto.NewBadgeNotification> checkWeightBadges(User user) {
        List<BadgeDto.NewBadgeNotification> newBadges = new ArrayList<>();

        // 최근 인바디 기록 조회
        List<InBody> inBodyRecords = inBodyRepository.findByUserOrderByMeasurementDateDesc(user);

        if (inBodyRecords.size() < 2) {
            return newBadges; // 비교할 데이터 부족
        }

        InBody latest = inBodyRecords.get(0);
        InBody first = inBodyRecords.get(inBodyRecords.size() - 1);

        // 체중 변화 계산
        BigDecimal weightChange = latest.getWeight().subtract(first.getWeight());

        // 목표 체중 달성 체크
        if (user.getWeightGoal() != null) {
            int goalWeight = user.getWeightGoal();
            int currentWeight = latest.getWeight().intValue();

            if (user.getHealthGoal() == User.HealthGoal.DIET && currentWeight <= goalWeight) {
                newBadges.add(tryAwardBadge(user, Badge.BadgeType.WEIGHT_GOAL_ACHIEVED));
            } else if ((user.getHealthGoal() == User.HealthGoal.BULK ||
                    user.getHealthGoal() == User.HealthGoal.LEAN_MASS) &&
                    currentWeight >= goalWeight) {
                newBadges.add(tryAwardBadge(user, Badge.BadgeType.WEIGHT_GOAL_ACHIEVED));
            }
        }

        // 체중 감량 뱃지
        if (weightChange.compareTo(BigDecimal.valueOf(-10)) <= 0) {
            newBadges.add(tryAwardBadge(user, Badge.BadgeType.WEIGHT_LOSS_10KG));
        } else if (weightChange.compareTo(BigDecimal.valueOf(-5)) <= 0) {
            newBadges.add(tryAwardBadge(user, Badge.BadgeType.WEIGHT_LOSS_5KG));
        }

        // 체중 증량 뱃지
        if (weightChange.compareTo(BigDecimal.valueOf(5)) >= 0) {
            newBadges.add(tryAwardBadge(user, Badge.BadgeType.MUSCLE_GAIN_5KG));
        }

        // 체지방률 감소 뱃지
        if (inBodyRecords.size() >= 2) {
            BigDecimal bodyFatChange = latest.getBodyFatPercentage()
                    .subtract(first.getBodyFatPercentage());

            if (bodyFatChange.compareTo(BigDecimal.valueOf(-5)) <= 0) {
                newBadges.add(tryAwardBadge(user, Badge.BadgeType.BODY_FAT_REDUCED));
            }
        }

        return newBadges.stream().filter(Objects::nonNull).collect(Collectors.toList());
    }

    private BadgeDto.NewBadgeNotification tryAwardBadge(User user, Badge.BadgeType badgeType) {
        Badge badge = badgeRepository.findByBadgeType(badgeType).orElse(null);
        if (badge == null) {
            return null;
        }

        // 이미 획득한 뱃지인지 확인
        if (userBadgeRepository.existsByUserAndBadge(user, badge)) {
            return null;
        }

        // 뱃지 부여
        UserBadge userBadge = UserBadge.builder()
                .user(user)
                .badge(badge)
                .isDisplayed(true)
                .displayOrder(getNextDisplayOrder(user))
                .build();

        userBadgeRepository.save(userBadge);

        log.info("새 뱃지 획득 - 사용자: {}, 뱃지: {}",
                user.getUserId(), badge.getBadgeType());

        return BadgeDto.NewBadgeNotification.builder()
                .success(true)
                .message("축하합니다! 새로운 뱃지를 획득했습니다! 🎉")
                .badge(BadgeDto.BadgeInfo.from(badge, userBadge))
                .earnedAt(userBadge.getEarnedAt())
                .build();
    }

    private int getNextDisplayOrder(User user) {
        List<UserBadge> userBadges = userBadgeRepository
                .findByUserAndIsDisplayedTrueOrderByDisplayOrderAsc(user);

        if (userBadges.isEmpty()) {
            return 1;
        }

        return userBadges.stream()
                .mapToInt(ub -> ub.getDisplayOrder() != null ? ub.getDisplayOrder() : 0)
                .max()
                .orElse(0) + 1;
    }

    private int countBadgesByCategory(List<BadgeDto.BadgeInfo> badges, String category) {
        return (int) badges.stream()
                .filter(b -> b.getBadgeType().startsWith(category))
                .count();
    }

    private String createDisplayBadgeMessage(List<BadgeDto.BadgeInfo> badges, long totalCount) {
        if (badges.isEmpty()) {
            return "운동과 식단을 기록하고 첫 뱃지를 획득하세요! 💪";
        }

        // 가장 최근 뱃지 기준 메시지
        BadgeDto.BadgeInfo latestBadge = badges.get(0);
        String badgeType = latestBadge.getBadgeType();

        if (badgeType.contains("100")) {
            return "후워! 100개 성공! 상태 정말이 반짝✨";
        } else if (badgeType.contains("50")) {
            return "50회 달성! 대단해요! 계속 이어가세요! 🔥";
        } else if (badgeType.contains("30")) {
            return "30회 달성! 벌써 여기까지 왔어요! 👏";
        } else if (badgeType.contains("WEIGHT_GOAL")) {
            return "목표 체중 달성! 정말 대단해요! 🎉";
        } else if (badgeType.contains("STREAK")) {
            return "연속 기록 달성! 꾸준함이 최고예요! 🌟";
        }

        return String.format("현재 %d개의 뱃지를 획득했어요! 계속 화이팅! 💪", totalCount);
    }
}
