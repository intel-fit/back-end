package rto.intelfit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import rto.intelfit.domain.InBody;
import rto.intelfit.domain.User;
import rto.intelfit.dto.InBodyDto;
import rto.intelfit.exception.BusinessException;
import rto.intelfit.exception.ErrorCode;
import rto.intelfit.repository.InBodyRepository;
import rto.intelfit.repository.UserRepository;
import rto.intelfit.security.CustomUserPrincipal;
import rto.intelfit.service.ocr.InBodyOcrPipeline;
import rto.intelfit.service.InBodyAnalysisService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InBodyService {

    private final InBodyRepository inBodyRepository;
    private final UserRepository userRepository;
    private final S3StorageService s3StorageService;
    private final InBodyOcrPipeline inBodyOcrPipeline;
    private final AIServerClient aiServerClient;
    private final InBodyAnalysisService inBodyAnalysisService;

    public String getDailyWeightComment(CustomUserPrincipal userPrincipal) {
        User user = findUserByPrincipal(userPrincipal);
        InBodyCommentRequest request = buildInBodyCommentRequest(user);
        return aiServerClient.analyzeInBodyComment(
                user.getUserId(),
                request.startDate(),
                request.endDate(),
                request.records());
    }

    public String getDailyFatComment(CustomUserPrincipal userPrincipal) {
        User user = findUserByPrincipal(userPrincipal);
        InBodyCommentRequest request = buildInBodyCommentRequest(user);
        return aiServerClient.analyzeInBodyComment(
                user.getUserId(),
                request.startDate(),
                request.endDate(),
                request.records());
    }

    public String getDailyMuscleComment(CustomUserPrincipal userPrincipal) {
        User user = findUserByPrincipal(userPrincipal);
        InBodyCommentRequest request = buildInBodyCommentRequest(user);
        return aiServerClient.analyzeInBodyComment(
                user.getUserId(),
                request.startDate(),
                request.endDate(),
                request.records());
    }

    /**
     * 인바디 정보 등록
     */
    @Transactional
    public InBodyDto.InBodyCreateResponse createInBody(
            CustomUserPrincipal userPrincipal,
            InBodyDto.InBodyCreateRequest request) {

        User user = findUserByPrincipal(userPrincipal);

        // 동일 날짜에 이미 기록이 있는지 확인
        ensureUniqueMeasurement(user, request.getMeasurementDate());

        InBody inBody = buildInBodyFromRequest(user, request);

        InBody savedInBody = inBodyRepository.save(inBody);

        log.info("인바디 정보 등록 완료 - 사용자 ID: {}, 측정 날짜: {}",
                user.getUserId(), request.getMeasurementDate());

        triggerAiInbodyAnalysis(user, savedInBody);

        return InBodyDto.InBodyCreateResponse.builder()
                .success(true)
                .message("인바디 정보가 등록되었습니다")
                .inBody(InBodyDto.InBodyDetailResponse.from(savedInBody, user))
                .build();
    }

    /**
     * 인바디 결과지 이미지 업로드 -> S3 저장 + Gemini OCR 초안 생성
     */
    @Transactional
    public InBodyDto.InBodyOcrUploadResponse uploadInBodyFromImage(
            CustomUserPrincipal userPrincipal,
            MultipartFile file) {

        User user = findUserByPrincipal(userPrincipal);
        log.info("인바디 결과지 업로드 요청 - 사용자 ID: {}", user.getUserId());

        S3StorageService.UploadResult uploadResult = s3StorageService.uploadInBodyImageWithKey(user.getUserId(), file);
        byte[] downloadedImage = s3StorageService.downloadImage(uploadResult.objectKey());
        InBodyOcrPipeline.PipelineResult pipelineResult = inBodyOcrPipeline.execute(downloadedImage);
        return InBodyDto.InBodyOcrUploadResponse.builder()
                .success(true)
                .message("AI가 추출한 인바디 초안 데이터를 확인해 주세요")
                .imageUrl(uploadResult.imageUrl())
                .draftData(pipelineResult.getFinalResult())
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
     * 인바디 기록 목록 조회
     */
    public List<InBodyDto.InBodySummaryResponse> getInBodyList(CustomUserPrincipal userPrincipal) {
        User user = findUserByPrincipal(userPrincipal);

        List<InBody> inBodyList = inBodyRepository.findByUserOrderByMeasurementDateDesc(user);

        log.info("인바디 목록 조회 - 사용자 ID: {}, 총 {}건", user.getUserId(), inBodyList.size());

        return inBodyList.stream()
                .map(InBodyDto.InBodySummaryResponse::from)
                .toList();
    }

    /**
     * 특정 인바디 기록 상세 조회
     */
    public InBodyDto.InBodyDetailResponse getInBodyById(CustomUserPrincipal userPrincipal, Long inBodyId) {
        User user = findUserByPrincipal(userPrincipal);

        InBody inBody = inBodyRepository.findById(inBodyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "인바디 기록을 찾을 수 없습니다"));

        // 본인의 기록인지 확인
        if (!inBody.getUser().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "본인의 인바디 기록만 조회할 수 있습니다");
        }

        log.info("인바디 상세 조회 - 사용자 ID: {}, 인바디 ID: {}", user.getUserId(), inBodyId);

        return InBodyDto.InBodyDetailResponse.from(inBody, user);
    }

    /**
     * 날짜별 인바디 기록 조회
     */
    public InBodyDto.InBodyDetailResponse getInBodyByDate(CustomUserPrincipal userPrincipal, LocalDate date) {
        User user = findUserByPrincipal(userPrincipal);

        InBody inBody = inBodyRepository.findByUserAndMeasurementDate(user, date)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
                        "해당 날짜(" + date + ")의 인바디 기록이 없습니다"));

        log.info("날짜별 인바디 조회 - 사용자 ID: {}, 날짜: {}", user.getUserId(), date);

        return InBodyDto.InBodyDetailResponse.from(inBody, user);
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

    private void triggerAiInbodyAnalysis(User user, InBody inBody) {
        Runnable analysisTask = () -> {
            try {
                String analysisText = aiServerClient.requestInBodyPeriodAnalysis(
                        user.getUserId(),
                        inBody.getMeasurementDate(),
                        inBody.getMeasurementDate());
                inBodyAnalysisService.saveAnalysisResult(
                        user,
                        inBody.getMeasurementDate(),
                        inBody.getMeasurementDate(),
                        analysisText);
                log.info("AI 서버 인바디 분석 요청 완료 - 사용자 ID: {}, 날짜: {}",
                        user.getUserId(), inBody.getMeasurementDate());
            } catch (Exception e) {
                log.error("AI 서버 인바디 분석 요청 실패 - 사용자 ID: {}, 날짜: {}, 오류: {}",
                        user.getUserId(), inBody.getMeasurementDate(), e.getMessage(), e);
            }
        };

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    analysisTask.run();
                }
            });
        } else {
            analysisTask.run();
        }
    }

    private InBody buildInBodyFromRequest(User user, InBodyDto.InBodyCreateRequest request) {
        return InBody.builder()
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
                .achievementBadge(InBody.AchievementBadge.NONE)
                .build();
    }

    private void ensureUniqueMeasurement(User user, LocalDate measurementDate) {
        if (measurementDate == null) {
            throw new BusinessException(ErrorCode.INVALID_MEASUREMENT_DATE, "측정 날짜가 필요합니다");
        }
        inBodyRepository.findByUserAndMeasurementDate(user, measurementDate)
                .ifPresent(existing -> {
                    throw new BusinessException(ErrorCode.DUPLICATE_INBODY_DATE, "해당 날짜에 이미 인바디 기록이 존재합니다");
                });
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

    private InBodyCommentRequest buildInBodyCommentRequest(User user) {
        List<InBody> records = inBodyRepository.findByUserOrderByMeasurementDateDesc(user);
        if (records.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "인바디 기록이 없습니다");
        }
        List<InBody> sortedRecords = records.stream()
                .sorted(Comparator.comparing(InBody::getMeasurementDate))
                .toList();

        LocalDate startDate = sortedRecords.get(0).getMeasurementDate();
        LocalDate endDate = sortedRecords.get(sortedRecords.size() - 1).getMeasurementDate();
        List<Map<String, Object>> payloadRecords = sortedRecords.stream()
                .map(this::mapInBodyForAiPayload)
                .toList();

        return new InBodyCommentRequest(startDate, endDate, payloadRecords);
    }

    private Map<String, Object> mapInBodyForAiPayload(InBody inBody) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("measurement_date", inBody.getMeasurementDate());
        payload.put("weight", toDouble(inBody.getWeight()));
        payload.put("skeletal_muscle_mass", toDouble(inBody.getSkeletalMuscleMass()));
        payload.put("body_fat_percentage", toDouble(inBody.getBodyFatPercentage()));
        return payload;
    }

    private Double toDouble(BigDecimal value) {
        return value != null ? value.doubleValue() : null;
    }

    private record InBodyCommentRequest(LocalDate startDate,
                                        LocalDate endDate,
                                        List<Map<String, Object>> records) {
    }
}
