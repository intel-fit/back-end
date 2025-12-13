package rto.intelfit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rto.intelfit.domain.InBody;
import rto.intelfit.domain.User;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface InBodyRepository extends JpaRepository<InBody, Long> {

    // 사용자별 모든 인바디 기록 조회 (최신순)
    List<InBody> findByUserOrderByMeasurementDateDesc(User user);

    // 사용자별 최신 인바디 기록 조회
    Optional<InBody> findFirstByUserOrderByMeasurementDateDesc(User user);

    // 사용자별 최신 인바디 기록 조회 (Top 메소드)
    Optional<InBody> findTopByUserOrderByMeasurementDateDesc(User user);

    // 사용자별 가장 오래된 인바디 기록 조회
    Optional<InBody> findFirstByUserOrderByMeasurementDateAsc(User user);

    // 특정 날짜 이전의 가장 최근 인바디 기록 조회
    Optional<InBody> findTopByUserAndMeasurementDateBeforeOrderByMeasurementDateDesc(
            User user, LocalDate measurementDate);

    // 사용자와 측정 날짜로 조회 (중복 체크용)
    Optional<InBody> findByUserAndMeasurementDate(User user, LocalDate measurementDate);

    // 사용자의 특정 기간 인바디 기록 조회 (최신순)
    List<InBody> findByUserAndMeasurementDateBetweenOrderByMeasurementDateDesc(
            User user, LocalDate startDate, LocalDate endDate);

    // 사용자의 인바디 기록 개수
    long countByUser(User user);

    // 사용자의 특정 인바디 기록 존재 여부 확인
    boolean existsByUserAndId(User user, Long id);

    // 사용자의 모든 인바디 기록 삭제 (회원 탈퇴 시)
    void deleteAllByUser(User user);
}
