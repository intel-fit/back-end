package rto.intelfit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rto.intelfit.domain.Badge;

import java.util.List;
import java.util.Optional;

@Repository
public interface BadgeRepository extends JpaRepository<Badge, Long> {

    // 뱃지 타입으로 조회
    Optional<Badge> findByBadgeType(Badge.BadgeType badgeType);

    // 모든 뱃지 조회 (표시 순서대로)
    List<Badge> findAllByOrderByDisplayOrderAsc();

    // 뱃지 타입 존재 여부
    boolean existsByBadgeType(Badge.BadgeType badgeType);
}
