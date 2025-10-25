package rto.intelfit.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import rto.intelfit.domain.ExerciseRecordDB;
import rto.intelfit.domain.User;
import rto.intelfit.dto.ExerciseRecordDto;
import rto.intelfit.exception.BusinessException;
import rto.intelfit.exception.ErrorCode;
import rto.intelfit.repository.ExerciseRecordRepository;
import rto.intelfit.repository.UserRepository;
import org.springframework.http.HttpMethod;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExerciseRecordService {

    private final ExerciseRecordRepository exerciseRecordRepository;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String WGER_API_URL = "https://wger.de/api/v2/exerciseinfo/?language=2";
    private static final String WGER_DETAIL_URL = "https://wger.de/api/v2/exerciseinfo/{id}/?language=2";


    // ✅ 기존 add/get/delete 그대로 유지
    @Transactional
    public ExerciseRecordDto.Response addExercise(ExerciseRecordDto.Request request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        ExerciseRecordDB exerciseRecordDB = ExerciseRecordDB.builder()
                .user(user)
                .exerciseName(request.getExerciseName())
                .category(request.getCategory())
                .weight(request.getWeight())
                .reps(request.getReps())
                .sets(request.getSets())
                .build();

        ExerciseRecordDB saved = exerciseRecordRepository.save(exerciseRecordDB);
        log.info("운동 추가 완료 - userId: {}, name: {}", user.getId(), saved.getExerciseName());

        return ExerciseRecordDto.Response.from(saved);
    }

    public ExerciseRecordDto.ListResponse getUserExercises(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        List<ExerciseRecordDto.Response> exercises = exerciseRecordRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(ExerciseRecordDto.Response::from)
                .collect(Collectors.toList());

        return ExerciseRecordDto.ListResponse.builder()
                .userId(userId)
                .exercises(exercises)
                .build();
    }

    @Transactional
    public ExerciseRecordDto.DeleteResponse deleteExercise(Long exerciseId) {
        ExerciseRecordDB exerciseRecordDB = exerciseRecordRepository.findById(exerciseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EXERCISE_NOT_FOUND));

        exerciseRecordRepository.delete(exerciseRecordDB);
        log.info("운동 삭제 완료 - exerciseId: {}", exerciseId);

        return ExerciseRecordDto.DeleteResponse.builder()
                .success(true)
                .message("운동이 삭제되었습니다.")
                .build();
    }

    //  WGER 운동 목록 조회
    // id , 이름 , 카테고리 , 이미지 url 가 한세트인 인스턴스의 리스트 반환
    @Transactional
    public List<ExerciseRecordDto.WgerResponse> fetchExercises(String category, String query) {
        try {
            String apiUrl = "https://wger.de/api/v2/exerciseinfo/?language=2&limit=30";

            if (category != null && !category.isBlank())
                apiUrl += "&category__name__icontains=" + category;
            if (query != null && !query.isBlank())
                apiUrl += "&name__icontains=" + query;

            ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.GET, null, String.class);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.getBody());
            JsonNode results = root.get("results");

            List<ExerciseRecordDto.WgerResponse> exerciseList = new ArrayList<>();

            for (JsonNode exerciseNode : results) {
                long id = exerciseNode.path("id").asLong();
                String name = exerciseNode.path("name").asText("Unknown");
                int categoryId = exerciseNode.path("category").path("id").asInt(-1);
                String categoryName = exerciseNode.path("category").path("name").asText("Unknown");

                // 이름이 Unknown, null, 공백이면 건너뛰기
                //if (name.equalsIgnoreCase("Unknown"))
                   //continue;

                // 이미지 가져오기
                String imageUrl = fetchExerciseImage(id);

                // 이미지가 null, 공백이면 건너뛰기
                //if (imageUrl == null || imageUrl.isBlank())
                  // continue;

                ExerciseRecordDto.WgerResponse dto = ExerciseRecordDto.WgerResponse.builder()
                        .id(id)
                        .name(name)
                        .categoryId(categoryId)
                        .category(categoryName)
                        .imageUrl(imageUrl)
                        .build();

                exerciseList.add(dto);
            }

            log.info("✅ WGER 운동목록 {}개 불러옴 (필터링 완료)", exerciseList.size());
            return exerciseList;

        } catch (Exception e) {
            log.error("❌ WGER 운동목록 조회 실패: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
        }
    }







    // 리스트 클릭시, 해당 인스턴스의 id 값을 여기에 넣어 호출한다
    public ExerciseRecordDto.WgerDetailResponse fetchExerciseDetail(Long exerciseId) {
        ResponseEntity<JsonNode> response = restTemplate.getForEntity(WGER_DETAIL_URL, JsonNode.class, exerciseId);
        JsonNode node = response.getBody();

        if (node == null) {
            throw new BusinessException(ErrorCode.EXERCISE_NOT_FOUND);
        }

        String name = node.hasNonNull("name") ? node.get("name").asText() : "Unknown Exercise";
        String description = node.hasNonNull("description") ? node.get("description").asText() : "No description available";
        String category = node.has("category") && node.get("category").has("name")
                ? node.get("category").get("name").asText()
                : "None category";

        // ✅ 이미지 가져오기 (기존 구조 유지 + 안전처리)
        String image = null;
        JsonNode imagesNode = node.get("images");
        if (imagesNode != null && imagesNode.isArray() && imagesNode.size() > 0) {
            image = imagesNode.get(0).get("image").asText();
        } else {
            // WGER에서 images가 null인 경우, 별도 image API 조회 시도
            image = fetchExerciseImage(exerciseId);
        }

        return ExerciseRecordDto.WgerDetailResponse.builder()
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
    public ExerciseRecordDto.CalorieResponse recordExerciseAndCalculateCalories(ExerciseRecordDto.CalorieRequest request) {
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
        ExerciseRecordDB exerciseRecordDB = ExerciseRecordDB.builder()
                .user(user)
                .exerciseName(name)
                .category(category)
                .weight(request.getWeight())
                .reps(request.getReps())
                .sets(request.getSets())
                .calories(calories)
                .build();

        exerciseRecordRepository.save(exerciseRecordDB);
        log.info("운동 기록 저장 완료 - {}, {} kcal", name, calories);

        return ExerciseRecordDto.CalorieResponse.builder()
                .exerciseName(name)
                .totalCalories(calories)
                .saved(true)
                .build();
    }

    // ------------------------------------------------------------------------------------------------------- //
    // 함수들
    // met mapping
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
    //wger 이미지 추출
    private String fetchExerciseImage(Long exerciseId) {
        String imageUrl = null;
        String imageApiUrl = "https://wger.de/api/v2/exerciseimage/?exercise=" + exerciseId;

        try {
            ResponseEntity<JsonNode> imgResponse = restTemplate.getForEntity(imageApiUrl, JsonNode.class);
            JsonNode imgResults = imgResponse.getBody().get("results");

            if (imgResults.isArray() && imgResults.size() > 0) {
                // 대표 이미지 (is_main = true) 우선
                for (JsonNode img : imgResults) {
                    if (img.has("is_main") && img.get("is_main").asBoolean()) {
                        imageUrl = img.get("image").asText();
                        break;
                    }
                }
                // 대표 이미지가 없으면 첫 번째 이미지 사용
                if (imageUrl == null) {
                    imageUrl = imgResults.get(0).get("image").asText();
                }
            }
        } catch (Exception e) {
            log.warn("이미지 요청 실패 - exerciseId: {}", exerciseId);
        }

        return imageUrl;
    }
}


