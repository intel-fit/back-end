package rto.intelfit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rto.intelfit.domain.User;
import rto.intelfit.domain.UserRecommendedExercise;
import java.time.LocalDate;

import java.util.List;

@Repository
public interface UserRecommendedExerciseRepository extends JpaRepository<UserRecommendedExercise, Long> {
    List<UserRecommendedExercise> findByUserOrderByCreatedAtDesc(User user);
    void deleteByUserAndExerciseDate(User user, LocalDate exerciseDate);
    List<UserRecommendedExercise> findByUserOrderByExerciseDateAscCreatedAtAsc(User user);

    List<UserRecommendedExercise> findByUserAndExerciseDateOrderByCreatedAtAsc(User user, LocalDate date);
}
