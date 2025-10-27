package rto.intelfit.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rto.intelfit.domain.FitnessExerciseCategoryDB;

import java.util.Optional;

public interface FitnessExerciseCategoryRepository extends JpaRepository<FitnessExerciseCategoryDB, Long> {

    /** 외부 ID 기준 단일 조회 */
    Optional<FitnessExerciseCategoryDB> findByExternalId(String externalId);

    /** 운동 부위별 + 키워드 검색 (둘 다 optional) */
    @Query("""
        SELECT f FROM FitnessExerciseCategoryDB f
        WHERE (:bodyPart IS NULL OR LOWER(f.bodyPart) = LOWER(:bodyPart))
        AND (:keyword IS NULL OR LOWER(f.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
        ORDER BY f.name ASC
    """)
    Page<FitnessExerciseCategoryDB> searchExercises(
            @Param("bodyPart") String bodyPart,
            @Param("keyword") String keyword,
            Pageable pageable);

    /** 전체 운동 목록 페이징 조회 */
    Page<FitnessExerciseCategoryDB> findAll(Pageable pageable);
}
