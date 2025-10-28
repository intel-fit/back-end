package rto.intelfit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rto.intelfit.domain.Exercise;
import rto.intelfit.domain.User;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExerciseRepository extends JpaRepository<Exercise, Long> {

    // 사용자별 모든 운동 기록 조회 (최신순)
    List<Exercise> findByUserOrderByExerciseDateDesc(User user);

    // 사용자의 특정 날짜 운동 기록 조회
    List<Exercise> findByUserAndExerciseDate(User user, LocalDate exerciseDate);

    List<Exercise> findByUserAndExerciseDateOrderByCreatedAtDesc(User user, LocalDate exerciseDate);

    // 사용자의 특정 기간 운동 기록 조회
    List<Exercise> findByUserAndExerciseDateBetween(User user, LocalDate startDate, LocalDate endDate);

    List<Exercise> findByUserAndExerciseDateBetweenOrderByExerciseDateDesc(
            User user, LocalDate startDate, LocalDate endDate);

    // 사용자의 최근 N일간 운동 기록 조회
    @Query("SELECT e FROM Exercise e WHERE e.user = :user AND e.exerciseDate >= :startDate ORDER BY e.exerciseDate DESC")
    List<Exercise> findRecentExercises(@Param("user") User user, @Param("startDate") LocalDate startDate);

    // 사용자의 운동 기록 개수
    long countByUser(User user);

    // 사용자의 특정 기간 운동 일수 계산
    @Query("SELECT COUNT(DISTINCT e.exerciseDate) FROM Exercise e WHERE e.user = :user AND e.exerciseDate BETWEEN :startDate AND :endDate")
    long countDistinctExerciseDays(@Param("user") User user, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    // 사용자의 모든 운동 기록 삭제 (회원 탈퇴 시)
    void deleteAllByUser(User user);

    // 사용자의 특정 운동 기록 존재 여부 확인
    boolean existsByUserAndId(User user, Long id);

    // 사용자별 운동 카테고리별 기록 조회
    List<Exercise> findByUserAndExerciseCategoryOrderByExerciseDateDesc(
            User user, Exercise.ExerciseCategory category);

    // 최근 운동 기록 조회 (최신 N개)
    List<Exercise> findTop10ByUserOrderByExerciseDateDesc(User user);
}