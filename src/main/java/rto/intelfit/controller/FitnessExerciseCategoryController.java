package rto.intelfit.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rto.intelfit.dto.ExerciseCategoryDto;
import rto.intelfit.service.FitnessExerciseCategoryService;

@Slf4j
@RestController
@RequestMapping("/api/exercise-db")
@RequiredArgsConstructor
@Tag(name = "Exercise DB API", description = "운동 종목(카테고리) 조회 API")
public class FitnessExerciseCategoryController {

    private final FitnessExerciseCategoryService service;

    @Operation(summary = "운동 목록 조회", description = "운동 부위(bodyPart)와 검색어(keyword)로 운동을 필터링합니다. 페이징 지원.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping
    public ResponseEntity<Page<ExerciseCategoryDto.Response>> getExerciseList(
            @RequestParam(required = false) String bodyPart,
            @RequestParam(required = false) String keyword,
            Pageable pageable) {

        Page<ExerciseCategoryDto.Response> result = service.getExerciseList(bodyPart, keyword, pageable);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "운동 상세 조회", description = "externalId 기준으로 단일 운동 상세 정보를 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "운동을 찾을 수 없음")
    })
    @GetMapping("/{externalId}")
    public ResponseEntity<ExerciseCategoryDto.Response> getExerciseDetail(@PathVariable String externalId) {
        return ResponseEntity.ok(service.getExerciseByExternalId(externalId));
    }
}
