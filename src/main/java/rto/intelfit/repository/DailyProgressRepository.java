package rto.intelfit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rto.intelfit.domain.DailyProgress;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyProgressRepository extends JpaRepository<DailyProgress, Long> {

    Optional<DailyProgress> findByUserIdAndDate(Long userId, LocalDate date);

    List<DailyProgress> findByUserIdAndDateBetween(Long userId, LocalDate start, LocalDate end);

    // --------------------------------------------------
    // 🔥 핵심 추가: INSERT 중복 방지용 PESSIMISTIC LOCK
    // --------------------------------------------------
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM DailyProgress p WHERE p.user.id = :userId AND p.date = :date")
    Optional<DailyProgress> findByUserIdAndDateForUpdate(
            @Param("userId") Long userId,
            @Param("date") LocalDate date
    );
}
