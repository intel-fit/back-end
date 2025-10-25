package rto.intelfit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rto.intelfit.domain.Exercise;
import rto.intelfit.domain.User;

import java.util.List;

public interface ExerciseRepository extends JpaRepository<Exercise, Long> {
    List<Exercise> findByUser(User user);
    List<Exercise> findByUserOrderByCreatedAtDesc(User user); // 정렬하기
}