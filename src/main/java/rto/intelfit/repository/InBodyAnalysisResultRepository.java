package rto.intelfit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rto.intelfit.domain.InBodyAnalysisResult;
import rto.intelfit.domain.User;

import java.util.Optional;

public interface InBodyAnalysisResultRepository extends JpaRepository<InBodyAnalysisResult, Long> {

    Optional<InBodyAnalysisResult> findTopByUserOrderByCreatedAtDesc(User user);

    void deleteAllByUser(User user);
}
