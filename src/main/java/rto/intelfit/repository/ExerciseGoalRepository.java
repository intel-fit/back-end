package rto.intelfit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rto.intelfit.domain.ExerciseGoal;
import rto.intelfit.domain.User;

import java.util.Optional;

public interface ExerciseGoalRepository extends JpaRepository<ExerciseGoal, Long> {
    Optional<ExerciseGoal> findByUser(User user);

    void deleteAllByUser(User user);
}
