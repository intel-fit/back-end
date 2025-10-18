package rto.intelfit.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import rto.intelfit.domain.Exercise;
import rto.intelfit.domain.User;
import rto.intelfit.dto.ExerciseDto;
import rto.intelfit.exception.BusinessException;
import rto.intelfit.exception.ErrorCode;
import rto.intelfit.repository.ExerciseRepository;
import rto.intelfit.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExerciseService {

    private final ExerciseRepository exerciseRepository;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String WGER_API_URL = "https://wger.de/api/v2/exerciseinfo/?language=2";
    private static final String WGER_DETAIL_URL = "https://wger.de/api/v2/exerciseinfo/{id}/?language=2";


    // ✅ 기존 add/get/delete 그대로 유지
    @Transactional
    public ExerciseDto.Response addExercise(ExerciseDto.Request request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Exercise exercise = Exercise.builder()
                .user(user)
                .exerciseName(request.getExerciseName())
                .category(request.getCategory())
                .weight(request.getWeight())
                .reps(request.getReps())
                .sets(request.getSets())
                .build();

        Exercise saved = exerciseRepository.save(exercise);
        log.info("운동 추가 완료 - userId: {}, name: {}", user.getId(), saved.getExerciseName());

        return ExerciseDto.Response.from(saved);
    }

    public ExerciseDto.ListResponse getUserExercises(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        List<ExerciseDto.Response> exercises = exerciseRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(ExerciseDto.Response::from)
                .collect(Collectors.toList());

        return ExerciseDto.ListResponse.builder()
                .userId(userId)
                .exercises(exercises)
                .build();
    }

    @Transactional
    public ExerciseDto.DeleteResponse deleteExercise(Long exerciseId) {
        Exercise exercise = exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EXERCISE_NOT_FOUND));

        exerciseRepository.delete(exercise);
        log.info("운동 삭제 완료 - exerciseId: {}", exerciseId);

        return ExerciseDto.DeleteResponse.builder()
                .success(true)
                .message("운동이 삭제되었습니다.")
                .build();
    }

    // ✅ [신규] WGER 운동 목록 조회
    // id , 이름 , 카테고리 , 이미지 url 가 한세트인 인스턴스의 리스트 반환
    public List<ExerciseDto.WgerResponse> fetchExercises(String category, String query) {
        ResponseEntity<JsonNode> response = restTemplate.getForEntity(WGER_API_URL, JsonNode.class);
        JsonNode results = response.getBody().get("results");

        List<ExerciseDto.WgerResponse> exercises = new ArrayList<>();

        for (JsonNode node : results) {
            Long id = node.get("id").asLong(); // ✅ 운동 ID 추가
            String name = node.get("name").asText();
            String cat = node.get("category").get("name").asText();
            String image = null;

            if (node.get("images").isArray() && node.get("images").size() > 0) {
                image = node.get("images").get(0).get("image").asText();
            }

            boolean categoryMatch = (category == null || cat.equalsIgnoreCase(category));
            boolean queryMatch = (query == null || name.toLowerCase().contains(query.toLowerCase()));

            if (categoryMatch && queryMatch) {
                exercises.add(ExerciseDto.WgerResponse.builder()
                        .id(id) // ✅ 프론트에서 클릭 시 사용할 ID
                        .name(name)
                        .category(cat)
                        .imageUrl(image)
                        .build());
            }
        }

        return exercises;
    }

    // 리스트 클릭시, 해당 인스턴스의 id 값을 여기에 넣어 호출한다
    public ExerciseDto.WgerDetailResponse fetchExerciseDetail(Long exerciseId) {
        ResponseEntity<JsonNode> response = restTemplate.getForEntity(WGER_DETAIL_URL, JsonNode.class, exerciseId);
        JsonNode node = response.getBody();

        String name = node.get("name").asText();
        String description = node.get("description").asText();
        String category = node.hasNonNull("category") ? node.get("category").get("name").asText() : "None category";
        String image = null;

        if (node.get("images").isArray() && node.get("images").size() > 0) {
            image = node.get("images").get(0).get("image").asText();
        }

        return ExerciseDto.WgerDetailResponse.builder()
                .id(exerciseId)
                .name(name)
                .description(description)
                .category(category)
                .imageUrl(image)
                .build();
    }

    // ✅ [신규] 칼로리 계산 + DB 저장
    // 리스트 -> 운동선택 -> 운동의 세트수와 무게 지정하는 페이지에서 완료하기 버튼 눌렀을 때 호출
    // ✅ [변경된 칼로리 계산 메서드]
    @Transactional
    public ExerciseDto.CalorieResponse recordExerciseAndCalculateCalories(ExerciseDto.CalorieRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 1️⃣ WGER에서 해당 운동의 상세 정보 가져오기
        ResponseEntity<JsonNode> response = restTemplate.getForEntity(WGER_DETAIL_URL, JsonNode.class, request.getExerciseId());
        JsonNode node = response.getBody();

        String name = node.get("name").asText();
        String category = node.get("category").get("name").asText();

        // 2️⃣ 카테고리에 따른 MET 값 자동 추정
        double met = getMetByCategory(category);

        // 3️⃣ 운동 시간 근사치 계산
        double durationHours = (request.getReps() * 2.0 * request.getSets()) / 3600.0;

        // 4️⃣ 실제 칼로리 계산
        double calories = met * user.getWeight() * durationHours;

        // 5️⃣ DB 저장
        Exercise exercise = Exercise.builder()
                .user(user)
                .exerciseName(name)
                .category(category)
                .weight(request.getWeight())
                .reps(request.getReps())
                .sets(request.getSets())
                .calories(calories)
                .build();

        exerciseRepository.save(exercise);
        log.info("운동 기록 저장 완료 - {}, {} kcal", name, calories);

        return ExerciseDto.CalorieResponse.builder()
                .exerciseName(name)
                .totalCalories(calories)
                .saved(true)
                .build();
    }

    // ✅ 카테고리별 MET 값 맵핑 함수
    private double getMetByCategory(String category) {
        String cat = category.toLowerCase();
        if (cat.contains("chest")) return 6.0;
        if (cat.contains("back")) return 7.0;
        if (cat.contains("leg")) return 8.0;
        if (cat.contains("arm")) return 4.5;
        if (cat.contains("shoulder")) return 5.5;
        if (cat.contains("abs") || cat.contains("core")) return 5.0;
        return 6.0;
    }
}


