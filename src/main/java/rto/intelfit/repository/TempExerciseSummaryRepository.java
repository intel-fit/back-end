package rto.intelfit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rto.intelfit.domain.TempExerciseSummary;
import rto.intelfit.domain.User;

import java.time.LocalDate;
import java.util.Optional;

public interface TempExerciseSummaryRepository
        extends JpaRepository<TempExerciseSummary, Long> {

    Optional<TempExerciseSummary> findByUserAndDate(User user, LocalDate date);

}
