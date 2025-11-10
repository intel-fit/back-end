    package rto.intelfit.repository;

    import org.springframework.data.jpa.repository.JpaRepository;
    import org.springframework.data.jpa.repository.Query;
    import org.springframework.data.repository.query.Param;
    import org.springframework.stereotype.Repository;
    import rto.intelfit.domain.RecommendedMealPlan;
    import rto.intelfit.domain.User;

    import java.util.List;

    @Repository
    public interface RecommendedMealPlanRepository extends JpaRepository<RecommendedMealPlan, Long> {

        // ✅ 저장된 식단만(최신순)
        List<RecommendedMealPlan> findByUserAndIsSavedTrueOrderByCreatedAtDesc(User user);

        // 모든 추천 식단(최신순)
        List<RecommendedMealPlan> findByUserOrderByCreatedAtDesc(User user);

        // 저장된 식단 개수
        long countByUserAndIsSavedTrue(User user);

        // 회원 탈퇴시 전체 삭제
        void deleteAllByUser(User user);

        // ✅ 번들 단위 상세(1~7일)
        List<RecommendedMealPlan> findByUserAndBundleIdOrderByBundleDayAsc(User user, String bundleId);

        // 내 번들 ID 목록(최신 생성 순)
        @Query("""
               select p.bundleId
               from RecommendedMealPlan p
               where p.user = :user
               group by p.bundleId
               order by max(p.createdAt) desc
               """)
        List<String> findMyBundleIdsOrderByLatest(@Param("user") User user);

        // 번들 삭제
        void deleteByUserAndBundleId(User user, String bundleId);
    }
