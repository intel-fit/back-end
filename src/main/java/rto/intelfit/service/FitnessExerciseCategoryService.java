package rto.intelfit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rto.intelfit.domain.FitnessExerciseCategoryDB;
import rto.intelfit.dto.ExerciseCategoryDto;
import rto.intelfit.exception.BusinessException;
import rto.intelfit.exception.ErrorCode;
import rto.intelfit.repository.FitnessExerciseCategoryRepository;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FitnessExerciseCategoryService {

    private final FitnessExerciseCategoryRepository repository;

    /** 1️⃣ 운동 목록 조회 (카테고리 + 검색어 + 페이징) */
    public Page<ExerciseCategoryDto.Response> getExerciseList(String bodyPart, String keyword, Pageable pageable) {
        log.info("📋 운동 목록 조회 - bodyPart={}, keyword={}", bodyPart, keyword);
        return repository.searchExercises(bodyPart, keyword, pageable)
                .map(ExerciseCategoryDto.Response::fromEntity);
    }

    /** 2️⃣ 단일 운동 상세 조회 */
    public ExerciseCategoryDto.Response getExerciseByExternalId(String externalId) {
        log.info("🔍 운동 상세 조회 - externalId={}", externalId);
        FitnessExerciseCategoryDB exercise = repository.findByExternalId(externalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "해당 운동을 찾을 수 없습니다."));
        return ExerciseCategoryDto.Response.fromEntity(exercise);
    }
}
