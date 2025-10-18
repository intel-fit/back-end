package rto.intelfit.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rto.intelfit.dto.ExerciseDto;
import rto.intelfit.service.ExerciseService;

@Slf4j
@RestController
@RequestMapping("/api/exercises")
@RequiredArgsConstructor
@Tag(name = "Exercise API", description = "운동 기록 관련 API")
public class ExerciseController {

    private final ExerciseService exerciseService;

    @Operation(summary = "운동 추가", description = "사용자가 운동을 추가합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "운동 추가 성공"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없습니다")
    })
    @PostMapping
    public ResponseEntity<ExerciseDto.Response> addExercise(@Valid @RequestBody ExerciseDto.Request request) {
        log.info("운동 추가 요청 - userId: {}, name: {}", request.getUserId(), request.getExerciseName());
        ExerciseDto.Response response = exerciseService.addExercise(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "운동 목록 조회", description = "특정 사용자의 운동 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "운동 목록 조회 성공"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없습니다")
    })
    @GetMapping("/{userId}")
    public ResponseEntity<ExerciseDto.ListResponse> getUserExercises(@PathVariable Long userId) {
        log.info("운동 조회 요청 - userId: {}", userId);
        ExerciseDto.ListResponse response = exerciseService.getUserExercises(userId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "운동 삭제", description = "특정 운동을 삭제합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "운동 삭제 성공"),
            @ApiResponse(responseCode = "404", description = "운동을 찾을 수 없습니다")
    })
    @DeleteMapping("/{exerciseId}")
    public ResponseEntity<ExerciseDto.DeleteResponse> deleteExercise(@PathVariable Long exerciseId) {
        log.info("운동 삭제 요청 - exerciseId: {}", exerciseId);
        ExerciseDto.DeleteResponse response = exerciseService.deleteExercise(exerciseId);
        return ResponseEntity.ok(response);
    }
}
