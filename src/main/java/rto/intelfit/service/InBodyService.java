package rto.intelfit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rto.intelfit.domain.InBody;
import rto.intelfit.domain.User;
import rto.intelfit.dto.InBodyDto;
import rto.intelfit.exception.BusinessException;
import rto.intelfit.exception.ErrorCode;
import rto.intelfit.repository.InBodyRepository;
import rto.intelfit.repository.UserRepository;
import rto.intelfit.security.CustomUserPrincipal;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InBodyService {

    private final InBodyRepository inBodyRepository;
    private final UserRepository userRepository;

    /**
     * 인바디 정보 등록
     */
    @Transactional
    public InBodyDto.InBodyCreateResponse createInBody(
            CustomUserPrincipal userPrincipal,
            InBodyDto.InBodyCreateRequest request) {

        User user = findUserByPrincipal(userPrincipal);

        // 동일 날짜에 이미 기록이 있는지 확인
        if (inBodyRepository.findByUserAndMeasurementDate(user, request.getMeasurementDate()).isPresent()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "해당 날짜에 이미 인바디 기록이 존재합니다");
        }

        InBody inBody = InBody.builder()
                .user(user)
                .measurementDate(request.getMeasurementDate())
                .weight(request.getWeight())
                .muscleMass(request.getMuscleMass())
                .bodyFatMass(request.getBodyFatMass())
                .skeletalMuscleMass(request.getSkeletalMuscleMass())
                .bodyFatPercentage(request.getBodyFatPercentage())
                .leftArmMuscle(request.getLeftArmMuscle())
                .rightArmMuscle(request.getRightArmMuscle())
                .trunkMuscle(request.getTrunkMuscle())
                .leftLegMuscle(request.getLeftLegMuscle())
                .rightLegMuscle(request.getRightLegMuscle())
                .leftArmFat(request.getLeftArmFat())
                .rightArmFat(request.getRightArmFat())
                .trunkFat(request.getTrunkFat())
                .leftLegFat(request.getLeftLegFat())
                .rightLegFat(request.getRightLegFat())
                .totalBodyWater(request.getTotalBodyWater())
                .protein(request.getProtein())
                .mineral(request.getMineral())
                .bmi(request.getBmi())
                .bodyFatPercentageStandard(request.getBodyFatPercentageStandard())
                .obesityDegree(request.getObesityDegree())
                .visceralFatLevel(request.getVisceralFatLevel())
                .basalMetabolicRate(request.getBasalMetabolicRate())
                .achievementBadge(InBody.AchievementBadge.NONE) // 초기값은 NONE
                .build();

        InBody savedInBody = inBodyRepository.save(inBody);

        log.info("인바디 정보 등록 완료 - 사용자 ID: {}, 측정 날짜: {}",
                user.getUserId(), request.getMeasurementDate());

        return InBodyDto.InBodyCreateResponse.builder()
                .success(true)
                .message("인바디 정보가 등록되었습니다")
                .inBody(InBodyDto.InBodyDetailResponse.from(savedInBody, user))
                .build();
    }

    /**
     * 최신 인바디 기록 조회 (화면용)
     */
    public InBodyDto.InBodyDetailResponse getLatestInBody(CustomUserPrincipal userPrincipal) {

        User user = findUserByPrincipal(userPrincipal);

        InBody latestInBody = inBodyRepository.findFirstByUserOrderByMeasurementDateDesc(user)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "인바디 기록이 없습니다"));

        log.info("최신 인바디 조회 - 사용자 ID: {}, 측정 날짜: {}",
                user.getUserId(), latestInBody.getMeasurementDate());

        return InBodyDto.InBodyDetailResponse.from(latestInBody, user);
    }

    /**
     * 인바디 정보 수정
     */
    @Transactional
    public InBodyDto.InBodyUpdateResponse updateInBody(
            CustomUserPrincipal userPrincipal,
            Long inBodyId,
            InBodyDto.InBodyUpdateRequest request) {

        User user = findUserByPrincipal(userPrincipal);

        InBody inBody = inBodyRepository.findById(inBodyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "인바디 기록을 찾을 수 없습니다"));

        // 본인의 기록인지 확인
        if (!inBody.getUser().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "본인의 인바디 기록만 수정할 수 있습니다");
        }

        // 수정 가능한 필드들 업데이트
        updateInBodyFields(inBody, request);
        InBody updatedInBody = inBodyRepository.save(inBody);

        log.info("인바디 정보 수정 완료 - 사용자 ID: {}, 인바디 ID: {}",
                user.getUserId(), inBodyId);

        return InBodyDto.InBodyUpdateResponse.builder()
                .success(true)
                .message("인바디 정보가 수정되었습니다")
                .inBody(InBodyDto.InBodyDetailResponse.from(updatedInBody, user))
                .build();
    }

    //private 메서드

    private User findUserByPrincipal(CustomUserPrincipal userPrincipal) {
        return userRepository.findByUserId(userPrincipal.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private void updateInBodyFields(InBody inBody, InBodyDto.InBodyUpdateRequest request) {
        if (request.getMeasurementDate() != null) {
            inBody.setMeasurementDate(request.getMeasurementDate());
        }
        if (request.getWeight() != null) {
            inBody.setWeight(request.getWeight());
        }
        if (request.getMuscleMass() != null) {
            inBody.setMuscleMass(request.getMuscleMass());
        }
        if (request.getBodyFatMass() != null) {
            inBody.setBodyFatMass(request.getBodyFatMass());
        }
        if (request.getSkeletalMuscleMass() != null) {
            inBody.setSkeletalMuscleMass(request.getSkeletalMuscleMass());
        }
        if (request.getBodyFatPercentage() != null) {
            inBody.setBodyFatPercentage(request.getBodyFatPercentage());
        }
        if (request.getLeftArmMuscle() != null) {
            inBody.setLeftArmMuscle(request.getLeftArmMuscle());
        }
        if (request.getRightArmMuscle() != null) {
            inBody.setRightArmMuscle(request.getRightArmMuscle());
        }
        if (request.getTrunkMuscle() != null) {
            inBody.setTrunkMuscle(request.getTrunkMuscle());
        }
        if (request.getLeftLegMuscle() != null) {
            inBody.setLeftLegMuscle(request.getLeftLegMuscle());
        }
        if (request.getRightLegMuscle() != null) {
            inBody.setRightLegMuscle(request.getRightLegMuscle());
        }
        if (request.getLeftArmFat() != null) {
            inBody.setLeftArmFat(request.getLeftArmFat());
        }
        if (request.getRightArmFat() != null) {
            inBody.setRightArmFat(request.getRightArmFat());
        }
        if (request.getTrunkFat() != null) {
            inBody.setTrunkFat(request.getTrunkFat());
        }
        if (request.getLeftLegFat() != null) {
            inBody.setLeftLegFat(request.getLeftLegFat());
        }
        if (request.getRightLegFat() != null) {
            inBody.setRightLegFat(request.getRightLegFat());
        }
        if (request.getTotalBodyWater() != null) {
            inBody.setTotalBodyWater(request.getTotalBodyWater());
        }
        if (request.getProtein() != null) {
            inBody.setProtein(request.getProtein());
        }
        if (request.getMineral() != null) {
            inBody.setMineral(request.getMineral());
        }
        if (request.getBmi() != null) {
            inBody.setBmi(request.getBmi());
        }
        if (request.getBodyFatPercentageStandard() != null) {
            inBody.setBodyFatPercentageStandard(request.getBodyFatPercentageStandard());
        }
        if (request.getObesityDegree() != null) {
            inBody.setObesityDegree(request.getObesityDegree());
        }
        if (request.getVisceralFatLevel() != null) {
            inBody.setVisceralFatLevel(request.getVisceralFatLevel());
        }
        if (request.getBasalMetabolicRate() != null) {
            inBody.setBasalMetabolicRate(request.getBasalMetabolicRate());
        }
        if (request.getAchievementBadge() != null) {
            inBody.setAchievementBadge(request.getAchievementBadge());
        }
    }
}