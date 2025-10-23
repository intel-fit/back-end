package rto.intelfit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rto.intelfit.domain.User;
import rto.intelfit.domain.UserFoodPreference;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserFoodPreferenceRepository extends JpaRepository<UserFoodPreference, Long> {

    // 사용자별 모든 선호 음식 조회
    List<UserFoodPreference> findByUserOrderByUpdatedAtDesc(User user);

    // 사용자별 선호도 타입별 조회
    List<UserFoodPreference> findByUserAndPreferenceTypeOrderByPreferenceScoreDesc(
            User user, UserFoodPreference.PreferenceType preferenceType);

    // 사용자의 좋아하는 음식 조회
    List<UserFoodPreference> findByUserAndPreferenceTypeInOrderByPreferenceScoreDesc(
            User user, List<UserFoodPreference.PreferenceType> preferenceTypes);

    // 사용자의 싫어하는 음식 조회
    List<UserFoodPreference> findByUserAndPreferenceTypeOrderByUpdatedAtDesc(
            User user, UserFoodPreference.PreferenceType preferenceType);

    // 특정 음식 선호도 조회
    Optional<UserFoodPreference> findByUserAndFoodName(User user, String foodName);

    // 사용자가 자주 먹는 음식 조회 (섭취 횟수 기준)
    @Query("SELECT ufp FROM UserFoodPreference ufp " +
            "WHERE ufp.user = :user " +
            "AND ufp.consumedCount > 0 " +
            "ORDER BY ufp.consumedCount DESC")
    List<UserFoodPreference> findFrequentlyConsumedFoods(@Param("user") User user);

    // 카테고리별 선호 음식 조회
    List<UserFoodPreference> findByUserAndCategoryOrderByPreferenceScoreDesc(
            User user, String category);

    // 선호 음식 존재 여부 확인
    boolean existsByUserAndFoodName(User user, String foodName);

    // 사용자의 선호 음식 개수
    long countByUser(User user);

    // 선호도 점수가 높은 음식 조회 (상위 N개)
    @Query("SELECT ufp FROM UserFoodPreference ufp " +
            "WHERE ufp.user = :user " +
            "AND ufp.preferenceType IN :types " +
            "ORDER BY ufp.preferenceScore DESC, ufp.consumedCount DESC")
    List<UserFoodPreference> findTopPreferredFoods(
            @Param("user") User user,
            @Param("types") List<UserFoodPreference.PreferenceType> types);

    // 사용자의 모든 음식 선호도 삭제 (회원 탈퇴 시)
    void deleteAllByUser(User user);
}