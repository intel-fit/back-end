package rto.intelfit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rto.intelfit.domain.AIChatMessage;

import java.util.List;

public interface AIChatMessageRepository extends JpaRepository<AIChatMessage, Long> {

    List<AIChatMessage> findTop50ByUser_IdOrderByCreatedAtDesc(Long userId);
}
