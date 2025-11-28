package rto.intelfit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rto.intelfit.domain.Meal;
import rto.intelfit.domain.User;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MealRepository extends JpaRepository<Meal, Long> {

    // 특정 사용자의 특정 날짜 모든 식사 조회
    List<Meal> findByUserAndMealDateOrderByMealTypeAsc(User user, LocalDate mealDate);

    // 특정 사용자의 특정 날짜 모든 식사 조회 (간단한 메소드)
    List<Meal> findByUserAndMealDate(User user, LocalDate mealDate);

    // 특정 사용자의 특정 기간 모든 식사 조회
    List<Meal> findByUserAndMealDateBetweenOrderByMealDateAscMealTypeAsc(
            User user, LocalDate startDate, LocalDate endDate);

    // 특정 사용자의 특정 기간 모든 식사 조회 (간단한 메소드)
    List<Meal> findByUserAndMealDateBetween(User user, LocalDate startDate, LocalDate endDate);

    // 특정 사용자의 특정 날짜, 특정 식사 타입 조회
    Optional<Meal> findByUserAndMealDateAndMealType(
            User user, LocalDate mealDate, Meal.MealType mealType);

    // 특정 사용자의 최근 N일간 식사 기록 조회
    @Query("SELECT m FROM Meal m WHERE m.user = :user " +
            "AND m.mealDate >= :startDate " +
            "ORDER BY m.mealDate DESC, m.mealType ASC")
    List<Meal> findRecentMeals(@Param("user") User user,
                               @Param("startDate") LocalDate startDate);

    // 특정 기간의 일일 총 칼로리 계산
    @Query("SELECT SUM(m.totalCalories) FROM Meal m " +
            "WHERE m.user = :user " +
            "AND m.mealDate BETWEEN :startDate AND :endDate")
    Double sumCaloriesByPeriod(@Param("user") User user,
                               @Param("startDate") LocalDate startDate,
                               @Param("endDate") LocalDate endDate);

    // 특정 기간의 일일 총 탄수화물 계산
    @Query("SELECT SUM(m.totalCarbs) FROM Meal m " +
            "WHERE m.user = :user " +
            "AND m.mealDate BETWEEN :startDate AND :endDate")
    Double sumCarbsByPeriod(@Param("user") User user,
                            @Param("startDate") LocalDate startDate,
                            @Param("endDate") LocalDate endDate);

    // 특정 기간의 일일 총 단백질 계산
    @Query("SELECT SUM(m.totalProtein) FROM Meal m " +
            "WHERE m.user = :user " +
            "AND m.mealDate BETWEEN :startDate AND :endDate")
    Double sumProteinByPeriod(@Param("user") User user,
                              @Param("startDate") LocalDate startDate,
                              @Param("endDate") LocalDate endDate);

    // 특정 기간의 일일 총 지방 계산
    @Query("SELECT SUM(m.totalFat) FROM Meal m " +
            "WHERE m.user = :user " +
            "AND m.mealDate BETWEEN :startDate AND :endDate")
    Double sumFatByPeriod(@Param("user") User user,
                          @Param("startDate") LocalDate startDate,
                          @Param("endDate") LocalDate endDate);

    // 사용자별 식사 기록 존재 여부
    boolean existsByUserAndId(User user, Long id);

    // 사용자의 전체 식사 기록 수
    long countByUser(User user);

    // 사용자의 모든 식사 기록 삭제 (회원 탈퇴 시)
    void deleteAllByUser(User user);
}