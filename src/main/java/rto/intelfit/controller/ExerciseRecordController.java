package rto.intelfit.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rto.intelfit.dto.ExerciseRecordDto;
import rto.intelfit.service.ExerciseRecordService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/exercises")
@RequiredArgsConstructor
@Tag(name = "Exercise API", description = "운동 기록 및 외부 운동 데이터 API")


public class ExerciseRecordController {

    private final ExerciseRecordService exerciseService;

    // ✅ 기존 기능들 (add / list / delete) 그대로 유지
    @Operation(summary = "운동 추가", description = "사용자가 운동기록을 추가합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "운동 추가 성공"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없습니다")
    })
    @PostMapping
    public ResponseEntity<ExerciseRecordDto.Response> addExercise(@Valid @RequestBody ExerciseRecordDto.Request request) {
        log.info("운동 추가 요청 - userId: {}, name: {}", request.getUserId(), request.getExerciseName());
        ExerciseRecordDto.Response response = exerciseService.addExercise(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "운동 목록 조회", description = "특정 사용자의 운동 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "운동 목록 조회 성공"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없습니다")
    })
    @GetMapping("/{userId}")
    public ResponseEntity<ExerciseRecordDto.ListResponse> getUserExercises(@PathVariable Long userId) {
        log.info("운동 조회 요청 - userId: {}", userId);
        ExerciseRecordDto.ListResponse response = exerciseService.getUserExercises(userId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "운동 삭제", description = "특정 운동 기록을 삭제합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "운동 삭제 성공"),
            @ApiResponse(responseCode = "404", description = "운동을 찾을 수 없습니다")
    })
    @DeleteMapping("/{exerciseId}")
    public ResponseEntity<ExerciseRecordDto.DeleteResponse> deleteExercise(@PathVariable Long exerciseId) {
        log.info("운동 삭제 요청 - exerciseId: {}", exerciseId);
        ExerciseRecordDto.DeleteResponse response = exerciseService.deleteExercise(exerciseId);
        return ResponseEntity.ok(response);
    }

    // ✅ 추가 API 1 : WGER 운동 목록 조회
    // 이름, 카테고리, 이미지 url 세트가 인스턴스 -> 이것의 리스트
    @Operation(summary = "외부 운동 목록 조회", description = "WGER API를 통해 운동 목록을 가져옵니다, 운동 기록 추가 버튼 누를때 나오는 리스트를 이것의 반환값으로 ")
    @GetMapping("/wger")
    public ResponseEntity<List<ExerciseRecordDto.WgerResponse>> getExercisesFromWger(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String query) { //종목, 검색식, 둘다 nullable by @리퀘스트파람
        //추가버튼 누르면 둘다 null 타입으로 호출 / 종목 누르면 재호출 / 검색하면 재호출

        log.info("WGER 운동 목록 조회 요청 - category: {}, query: {}", category, query);
        List<ExerciseRecordDto.WgerResponse> list = exerciseService.fetchExercises(category, query);
        return ResponseEntity.ok(list);
    }

    //리스트에서 클릭 했을 때
    @Operation(summary = "외부 운동 상세 조회", description = "WGER API를 통해 특정 운동의 상세 정보를 가져옵니다. \n 운동 리스트에서 인스턴스를 누르고 편집창으로 갈때 호출")
    @GetMapping("/wger/{exerciseId}")
    public ResponseEntity<ExerciseRecordDto.WgerDetailResponse> getExerciseDetailFromWger(
            @PathVariable Long exerciseId) {

        log.info("WGER 운동 상세 조회 요청 - exerciseId: {}", exerciseId);
        ExerciseRecordDto.WgerDetailResponse detail = exerciseService.fetchExerciseDetail(exerciseId);
        return ResponseEntity.ok(detail);
    }

    // ✅ 추가 API 2 : 운동 세트 기록 및 칼로리 계산
    @Operation(summary = "운동 칼로리 계산 및 기록", description = "운동 세트 정보를 받아 칼로리를 계산하고 저장합니다. \n 운동 기록 설정 페이지에서 편집을 끝내고 추가할때 호출")
    @PostMapping("/calorie")
    public ResponseEntity<ExerciseRecordDto.CalorieResponse> recordExerciseAndCalculateCalories(
            @Valid @RequestBody ExerciseRecordDto.CalorieRequest request) {
        log.info("운동 칼로리 계산 요청 - userId: {}, exercise: {}", request.getUserId(), request.getExerciseId());
        return ResponseEntity.ok(exerciseService.recordExerciseAndCalculateCalories(request));
    }
}
