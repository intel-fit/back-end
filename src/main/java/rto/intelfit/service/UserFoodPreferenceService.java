package rto.intelfit.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rto.intelfit.domain.User;
import rto.intelfit.domain.UserFoodPreference;
import rto.intelfit.dto.UserFoodPreferenceDto;
import rto.intelfit.repository.UserFoodPreferenceRepository;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserFoodPreferenceService {

    private final UserFoodPreferenceRepository repository;
    private final ObjectMapper objectMapper;

    private List<String> parseList(String json) {
        try {
            if (json == null || json.isBlank()) return new ArrayList<>();
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private String toJson(List<String> list) {
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            return "[]";
        }
    }

    // -----------------------------
    // 1) 선호/비선호 전체 조회
    // -----------------------------
    @Transactional(readOnly = true)
    public UserFoodPreferenceDto.Response getPreferences(User user) {

        UserFoodPreference pref = repository.findByUser(user)
                .orElse(UserFoodPreference.builder()
                        .user(user)
                        .preferredFoods("[]")
                        .dislikedFoods("[]")
                        .build());

        return UserFoodPreferenceDto.Response.builder()
                .preferredFoods(parseList(pref.getPreferredFoods()))
                .dislikedFoods(parseList(pref.getDislikedFoods()))
                .build();
    }

    // -----------------------------
    // 2) 비선호 음식만 가져오기 (AI 서버에서 사용)
    // -----------------------------
    @Transactional(readOnly = true)
    public List<String> getDislikedFoods(User user) {

        UserFoodPreference pref = repository.findByUser(user)
                .orElse(UserFoodPreference.builder()
                        .user(user)
                        .preferredFoods("[]")
                        .dislikedFoods("[]")
                        .build());

        return parseList(pref.getDislikedFoods());
    }
    public void addFoodConsumption(User user, String foodName) {
        // 기본 구현: 추후 선호도 알고리즘 확장용
        // 지금은 로그만 남기고 패스하도록 처리 가능

        log.debug("음식 섭취 기록 - user: {}, food: {}", user.getUserId(), foodName);
    }

    // -----------------------------
    // 3) 선호 음식만 가져오기 (확장용)
    // -----------------------------
    @Transactional(readOnly = true)
    public List<String> getPreferredFoods(User user) {

        UserFoodPreference pref = repository.findByUser(user)
                .orElse(UserFoodPreference.builder()
                        .user(user)
                        .preferredFoods("[]")
                        .dislikedFoods("[]")
                        .build());

        return parseList(pref.getPreferredFoods());
    }

    // -----------------------------
    // 4) 비선호 음식 추가
    // -----------------------------
    public void addDislikedFood(User user, String foodName) {

        UserFoodPreference pref = repository.findByUser(user)
                .orElse(UserFoodPreference.builder()
                        .user(user)
                        .preferredFoods("[]")
                        .dislikedFoods("[]")
                        .build());

        List<String> disliked = parseList(pref.getDislikedFoods());

        if (!disliked.contains(foodName)) {
            disliked.add(foodName);
        }

        pref.setDislikedFoods(toJson(disliked));
        repository.save(pref);
    }
}
