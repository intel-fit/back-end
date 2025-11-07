package rto.intelfit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rto.intelfit.domain.FitnessExerciseCategoryDB;
import rto.intelfit.dto.ExerciseCategoryDto;
import rto.intelfit.exception.BusinessException;
import rto.intelfit.exception.ErrorCode;
import rto.intelfit.repository.FitnessExerciseCategoryRepository;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FitnessExerciseCategoryService {

    private final FitnessExerciseCategoryRepository repository;

    private static final Map<String, List<String>> BODY_PART_MAP = new HashMap<>() {{
        put("가슴", List.of("가슴", "흉근", "대흉근"));
        put("등", List.of("등", "광배근", "승모근", "척추기립근"));
        put("어깨", List.of("어깨", "삼각근", "전면삼각근", "측면삼각근", "후면삼각근"));
        put("팔", List.of("팔", "이두", "삼두", "전완근", "이두근", "삼두근"));
        put("하체", List.of("하체", "허벅지", "햄스트링", "종아리", "둔근", "대퇴사두근", "엉덩이"));
        put("복부", List.of("복부", "복근", "복직근", "복사근", "코어"));
    }};

    /** 1️⃣ 운동 목록 조회 (카테고리 + 검색어 + 페이징) */
    public Page<ExerciseCategoryDto.Response> getExerciseList(String bodyPart, String keyword, Pageable pageable) {
        bodyPart = (bodyPart != null) ? bodyPart.trim() : null;
        log.info("📋 운동 목록 조회 - bodyPart={}, keyword={}", bodyPart, keyword);

        // ✅ DB 전체 데이터 조회
        List<ExerciseCategoryDto.Response> all = repository.findAll().stream()
                .map(ExerciseCategoryDto.Response::fromEntity)
                .toList();

        if (bodyPart == null || bodyPart.isBlank()) {
            return toPage(all, pageable);
        }

        List<String> mappedParts = BODY_PART_MAP.getOrDefault(bodyPart, List.of(bodyPart));
        log.info("✅ 매핑된 세부 부위 리스트: {}", mappedParts);

        // ✅ 매핑된 부위로 필터링
        List<ExerciseCategoryDto.Response> filtered = all.stream()
                .filter(e -> mappedParts.stream().anyMatch(mp ->
                        e.getBodyPart() != null && e.getBodyPart().contains(mp)
                ))
                .toList();

        // ✅ 필터링된 리스트를 다시 Pageable 형태로 자르기
        return toPage(filtered, pageable);
    }
    // ✅ 리스트를 Pageable 형태로 잘라주는 헬퍼 함수
    private Page<ExerciseCategoryDto.Response> toPage(List<ExerciseCategoryDto.Response> list, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), list.size());
        List<ExerciseCategoryDto.Response> pageContent = list.subList(start, end);
        return new PageImpl<>(pageContent, pageable, list.size());
    }

    /** 2️⃣ 단일 운동 상세 조회 */
    public ExerciseCategoryDto.Response getExerciseByExternalId(String externalId) {
        log.info("🔍 운동 상세 조회 - externalId={}", externalId);
        FitnessExerciseCategoryDB exercise = repository.findByExternalId(externalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "해당 운동을 찾을 수 없습니다."));
        return ExerciseCategoryDto.Response.fromEntity(exercise);
    }
}
