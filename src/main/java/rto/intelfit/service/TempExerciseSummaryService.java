package rto.intelfit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import rto.intelfit.repository.UserRecommendedExerciseRepository;

import rto.intelfit.domain.TempExerciseSummary;
import rto.intelfit.domain.User;
import rto.intelfit.dto.TempExerciseSummaryDto;
import rto.intelfit.exception.BusinessException;
import rto.intelfit.exception.ErrorCode;
import rto.intelfit.repository.TempExerciseSummaryRepository;
import rto.intelfit.repository.UserRepository;
import java.util.List;
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class TempExerciseSummaryService {
    private final TempExerciseSummaryRepository repository;
    private final UserRepository userRepository;
    private final UserRecommendedExerciseRepository userRecommendedExerciseRepository;

    public void saveOrUpdateTempSummary(String userId, TempExerciseSummaryDto dto) {

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        LocalDate date = LocalDate.parse(dto.getDate());

        TempExerciseSummary existing = repository
                .findByUserAndDate(user, date)
                .orElse(null);

        if (existing != null) {
            log.info("🔄 TEMP summary exists. Updating… user={}, date={}", userId, date);

            existing.setFocus(dto.getFocus());
            existing.setDurationMin(dto.getDurationMin());
            existing.setKcal(dto.getKcal());
            existing.setExerciseCount(dto.getExerciseCount());
            existing.setTitle(dto.getTitle());
            return;
        }

        TempExerciseSummary created = TempExerciseSummary.builder()
                .user(user)
                .date(date)
                .focus(dto.getFocus())
                .durationMin(dto.getDurationMin())
                .kcal(dto.getKcal())
                .exerciseCount(dto.getExerciseCount())
                .title(dto.getTitle())
                .build();

        repository.save(created);
        log.info("➕ TEMP summary created. user={}, date={}", userId, date);
    }




    @Transactional(readOnly = true)
    public TempExerciseSummaryDto getTempSummary(String userId, LocalDate date) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        TempExerciseSummary entity = repository.findByUserAndDate(user, date)
                .orElse(null);

        if (entity == null) {
            return null; // 또는 default 값 반환
        }

        return TempExerciseSummaryDto.builder()
                .date(entity.getDate().toString())
                .focus(entity.getFocus())
                .durationMin(entity.getDurationMin())
                .kcal(entity.getKcal())
                .exerciseCount(entity.getExerciseCount())
                .title(entity.getTitle())
                .build();
    }


    @Transactional
    public int deleteTempSummariesInRange(
            String userId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        int deleted =
                userRecommendedExerciseRepository
                        .deleteByUserAndExerciseDateBetween(user, startDate, endDate);

        log.info("🗑 TEMP(API) → UserRecommendedExercise 삭제 user={}, start={}, end={}, count={}",
                userId, startDate, endDate, deleted);

        return deleted;
    }


}
