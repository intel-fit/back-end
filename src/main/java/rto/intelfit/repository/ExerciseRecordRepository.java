package rto.intelfit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rto.intelfit.domain.ExerciseRecordDB;
import rto.intelfit.domain.User;

import java.util.List;

public interface ExerciseRecordRepository extends JpaRepository<ExerciseRecordDB, Long> {
    List<ExerciseRecordDB> findByUser(User user);
    List<ExerciseRecordDB> findByUserOrderByCreatedAtDesc(User user); // 정렬하기
}
