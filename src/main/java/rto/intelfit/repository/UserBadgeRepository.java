package rto.intelfit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rto.intelfit.domain.Badge;
import rto.intelfit.domain.User;
import rto.intelfit.domain.UserBadge;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserBadgeRepository extends JpaRepository<UserBadge, Long> {

    // 사용자의 모든 뱃지 조회 (획득 날짜 역순)
    List<UserBadge> findByUserOrderByEarnedAtDesc(User user);

    // 사용자의 표시된 뱃지만 조회 (표시 순서대로)
    List<UserBadge> findByUserAndIsDisplayedTrueOrderByDisplayOrderAsc(User user);

    // 사용자의 특정 뱃지 소유 여부 확인
    boolean existsByUserAndBadge(User user, Badge badge);

    // 사용자의 특정 뱃지 조회
    Optional<UserBadge> findByUserAndBadge(User user, Badge badge);

    // 사용자의 뱃지 개수 조회
    long countByUser(User user);

    // 사용자의 뱃지 타입별 개수 조회
    @Query("SELECT COUNT(ub) FROM UserBadge ub " +
           "JOIN ub.badge b " +
           "WHERE ub.user = :user " +
           "AND b.badgeType IN :badgeTypes")
    long countByUserAndBadgeTypes(
            @Param("user") User user,
            @Param("badgeTypes") List<Badge.BadgeType> badgeTypes);

    // 사용자의 모든 뱃지 삭제 (회원 탈퇴 시)
    void deleteAllByUser(User user);

    // 최근 획득한 뱃지 조회 (상위 N개)
    List<UserBadge> findTop3ByUserOrderByEarnedAtDesc(User user);
}
