package rto.intelfit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import rto.intelfit.domain.User;
import rto.intelfit.domain.UserFoodPreference;
import rto.intelfit.dto.UserFoodPreferenceDto;
import rto.intelfit.exception.BusinessException;
import rto.intelfit.exception.ErrorCode;
import rto.intelfit.repository.UserFoodPreferenceRepository;
import rto.intelfit.repository.UserRepository;
import rto.intelfit.security.CustomUserPrincipal;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserFoodPreferenceService {

    private final UserFoodPreferenceRepository preferenceRepository;
    private final UserRepository userRepository;

    /**
     * 선호 음식 추가
     */
    @Transactional
    public UserFoodPreferenceDto.FoodPreferenceResponse addFoodPreference(
            CustomUserPrincipal userPrincipal,
            UserFoodPreferenceDto.FoodPreferenceAddRequest request) {

        User user = findUserByPrincipal(userPrincipal);

        // 이미 존재하는지 확인
        if (preferenceRepository.existsByUserAndFoodName(user, request.getFoodName())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
                    "이미 등록된 음식입니다. 수정을 원하시면 수정 API를 사용해주세요");
        }

        // 선호 음식 생성
        UserFoodPreference preference = UserFoodPreference.builder()
                .user(user)
                .foodName(request.getFoodName())
                .preferenceType(request.getPreferenceType())
                .preferenceScore(request.getPreferenceScore() != null ? request.getPreferenceScore() : 3)
                .category(request.getCategory())
                .tags(request.getTags())
                .memo(request.getMemo())
                .consumedCount(0)
                .build();

        UserFoodPreference savedPreference = preferenceRepository.save(preference);

        log.info("선호 음식 추가 완료 - 사용자 ID: {}, 음식: {}, 타입: {}",
                user.getUserId(), request.getFoodName(), request.getPreferenceType());

        return UserFoodPreferenceDto.FoodPreferenceResponse.builder()
                .success(true)
                .message("선호 음식이 추가되었습니다")
                .preference(UserFoodPreferenceDto.FoodPreferenceDetailResponse.from(savedPreference))
                .build();
    }

    /**
     * 선호 음식 수정
     */
    @Transactional
    public UserFoodPreferenceDto.FoodPreferenceResponse updateFoodPreference(
            CustomUserPrincipal userPrincipal,
            Long preferenceId,
            UserFoodPreferenceDto.FoodPreferenceUpdateRequest request) {

        User user = findUserByPrincipal(userPrincipal);

        UserFoodPreference preference = preferenceRepository.findById(preferenceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
                        "선호 음식을 찾을 수 없습니다"));

        // 본인의 선호 음식인지 확인
        if (!preference.getUser().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED,
                    "본인의 선호 음식만 수정할 수 있습니다");
        }

        // 수정
        if (request.getPreferenceType() != null) {
            preference.setPreferenceType(request.getPreferenceType());
        }
        if (request.getPreferenceScore() != null) {
            preference.setPreferenceScore(request.getPreferenceScore());
        }
        if (StringUtils.hasText(request.getCategory())) {
            preference.setCategory(request.getCategory());
        }
        if (StringUtils.hasText(request.getTags())) {
            preference.setTags(request.getTags());
        }
        if (request.getMemo() != null) {
            preference.setMemo(request.getMemo());
        }

        UserFoodPreference updatedPreference = preferenceRepository.save(preference);

        log.info("선호 음식 수정 완료 - 사용자 ID: {}, 음식: {}",
                user.getUserId(), preference.getFoodName());

        return UserFoodPreferenceDto.FoodPreferenceResponse.builder()
                .success(true)
                .message("선호 음식이 수정되었습니다")
                .preference(UserFoodPreferenceDto.FoodPreferenceDetailResponse.from(updatedPreference))
                .build();
    }

    /**
     * 선호 음식 삭제
     */
    @Transactional
    public UserFoodPreferenceDto.FoodPreferenceDeleteResponse deleteFoodPreference(
            CustomUserPrincipal userPrincipal,
            Long preferenceId) {

        User user = findUserByPrincipal(userPrincipal);

        UserFoodPreference preference = preferenceRepository.findById(preferenceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
                        "선호 음식을 찾을 수 없습니다"));

        // 본인의 선호 음식인지 확인
        if (!preference.getUser().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED,
                    "본인의 선호 음식만 삭제할 수 있습니다");
        }

        preferenceRepository.delete(preference);

        log.info("선호 음식 삭제 완료 - 사용자 ID: {}, 음식: {}",
                user.getUserId(), preference.getFoodName());

        return UserFoodPreferenceDto.FoodPreferenceDeleteResponse.builder()
                .success(true)
                .message("선호 음식이 삭제되었습니다")
                .build();
    }

    /**
     * 모든 선호 음식 조회
     */
    public UserFoodPreferenceDto.FoodPreferenceListResponse getAllFoodPreferences(
            CustomUserPrincipal userPrincipal) {

        User user = findUserByPrincipal(userPrincipal);

        List<UserFoodPreference> preferences = preferenceRepository.findByUserOrderByUpdatedAtDesc(user);

        log.info("선호 음식 목록 조회 - 사용자 ID: {}, 개수: {}",
                user.getUserId(), preferences.size());

        return UserFoodPreferenceDto.FoodPreferenceListResponse.from(preferences);
    }

    /**
     * 선호도 타입별 음식 조회
     */
    public UserFoodPreferenceDto.FoodPreferenceListResponse getFoodPreferencesByType(
            CustomUserPrincipal userPrincipal,
            UserFoodPreference.PreferenceType preferenceType) {

        User user = findUserByPrincipal(userPrincipal);

        List<UserFoodPreference> preferences = preferenceRepository
                .findByUserAndPreferenceTypeOrderByPreferenceScoreDesc(user, preferenceType);

        log.info("선호도 타입별 음식 조회 - 사용자 ID: {}, 타입: {}, 개수: {}",
                user.getUserId(), preferenceType, preferences.size());

        return UserFoodPreferenceDto.FoodPreferenceListResponse.from(preferences);
    }

    /**
     * 좋아하는 음식 조회
     */
    public UserFoodPreferenceDto.FoodPreferenceListResponse getLikedFoods(
            CustomUserPrincipal userPrincipal) {

        User user = findUserByPrincipal(userPrincipal);

        List<UserFoodPreference.PreferenceType> likeTypes = Arrays.asList(
                UserFoodPreference.PreferenceType.LIKE,
                UserFoodPreference.PreferenceType.FAVORITE
        );

        List<UserFoodPreference> preferences = preferenceRepository
                .findByUserAndPreferenceTypeInOrderByPreferenceScoreDesc(user, likeTypes);

        log.info("좋아하는 음식 조회 - 사용자 ID: {}, 개수: {}",
                user.getUserId(), preferences.size());

        return UserFoodPreferenceDto.FoodPreferenceListResponse.from(preferences);
    }

    /**
     * 싫어하는 음식 조회
     */
    public UserFoodPreferenceDto.FoodPreferenceListResponse getDislikedFoods(
            CustomUserPrincipal userPrincipal) {

        User user = findUserByPrincipal(userPrincipal);

        List<UserFoodPreference> preferences = preferenceRepository
                .findByUserAndPreferenceTypeOrderByUpdatedAtDesc(
                        user, UserFoodPreference.PreferenceType.DISLIKE);

        log.info("싫어하는 음식 조회 - 사용자 ID: {}, 개수: {}",
                user.getUserId(), preferences.size());

        return UserFoodPreferenceDto.FoodPreferenceListResponse.from(preferences);
    }

    /**
     * 자주 먹는 음식 조회
     */
    public List<UserFoodPreferenceDto.FrequentFoodResponse> getFrequentlyConsumedFoods(
            CustomUserPrincipal userPrincipal) {

        User user = findUserByPrincipal(userPrincipal);

        List<UserFoodPreference> preferences = preferenceRepository.findFrequentlyConsumedFoods(user);

        log.info("자주 먹는 음식 조회 - 사용자 ID: {}, 개수: {}",
                user.getUserId(), preferences.size());

        return preferences.stream()
                .map(UserFoodPreferenceDto.FrequentFoodResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * AI 추천용 선호 데이터 조회
     */
    public UserFoodPreferenceDto.PreferenceDataForAI getPreferenceDataForAI(
            CustomUserPrincipal userPrincipal) {

        User user = findUserByPrincipal(userPrincipal);

        // 좋아하는 음식
        List<UserFoodPreference.PreferenceType> likeTypes = Arrays.asList(
                UserFoodPreference.PreferenceType.LIKE,
                UserFoodPreference.PreferenceType.FAVORITE
        );
        List<String> likedFoods = preferenceRepository
                .findByUserAndPreferenceTypeInOrderByPreferenceScoreDesc(user, likeTypes)
                .stream()
                .map(UserFoodPreference::getFoodName)
                .collect(Collectors.toList());

        // 싫어하는 음식
        List<String> dislikedFoods = preferenceRepository
                .findByUserAndPreferenceTypeOrderByUpdatedAtDesc(
                        user, UserFoodPreference.PreferenceType.DISLIKE)
                .stream()
                .map(UserFoodPreference::getFoodName)
                .collect(Collectors.toList());

        // 자주 먹는 음식
        List<String> frequentFoods = preferenceRepository.findFrequentlyConsumedFoods(user)
                .stream()
                .limit(10)
                .map(UserFoodPreference::getFoodName)
                .collect(Collectors.toList());

        // 선호 카테고리 추출
        List<String> preferredCategories = preferenceRepository.findByUserOrderByUpdatedAtDesc(user)
                .stream()
                .map(UserFoodPreference::getCategory)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());

        // 선호 태그 추출
        List<String> preferredTags = preferenceRepository.findByUserOrderByUpdatedAtDesc(user)
                .stream()
                .map(UserFoodPreference::getTags)
                .filter(StringUtils::hasText)
                .flatMap(tags -> Arrays.stream(tags.split(",")))
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());

        log.info("AI 추천용 선호 데이터 조회 - 사용자 ID: {}", user.getUserId());

        return UserFoodPreferenceDto.PreferenceDataForAI.builder()
                .likedFoods(likedFoods)
                .dislikedFoods(dislikedFoods)
                .frequentFoods(frequentFoods)
                .preferredCategories(preferredCategories)
                .preferredTags(preferredTags)
                .build();
    }

    /**
     * 음식 섭취 기록 업데이트 (식사 추가 시 자동 호출)
     */
    @Transactional
    public void updateFoodConsumption(User user, String foodName) {
        UserFoodPreference preference = preferenceRepository.findByUserAndFoodName(user, foodName)
                .orElse(null);

        if (preference != null) {
            preference.incrementConsumedCount();
            preferenceRepository.save(preference);

            log.info("음식 섭취 횟수 업데이트 - 사용자 ID: {}, 음식: {}, 횟수: {}",
                    user.getUserId(), foodName, preference.getConsumedCount());
        }
    }

    // Private helper methods

    private User findUserByPrincipal(CustomUserPrincipal userPrincipal) {
        return userRepository.findByUserId(userPrincipal.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}