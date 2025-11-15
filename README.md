운동/식단 영양 분석 프로그램
backend project

Git 명령어

    git init   #git 저장소 초기화, .git 파일 생성
    git remote add origin https://github.com/intel-fit/back-end.git  #원격 저장소 연결
    git pull origin dev
    
    git checkout -b <브랜치명>   #로컬 브랜치 생성하면서 이동
    git checkout <브랜치명>      #로컬 브랜치 이동
    
    git add <파일명?>
    git commit -m "커밋 메시지"
    git push -u origin <브랜치명>   #로컬 브랜치를 원격에 생성하면서 커밋 사항 푸쉬
    git push            #동기화된 원경-로컬 브랜치에서 로컬 변경사항 반영하면서 자동 반영
    
    git rebase origin/dev
    git revert 





code convention


    #

    ### 네이밍 규칙
    - 클래스: PascalCase
    - 메서드/변수: camelCase  
    - 상수: UPPER_SNAKE_CASE
    - 패키지: lowercase
    
    ### 아키텍처
    - 레이어드 아키텍처 (Controller-Service-Repository-Domain)
    - RESTful API 설계
    - DTO 패턴 사용
    
    ### 코드 스타일
    - 들여쓰기: 4칸 스페이스
    - K&R 중괄호 스타일
    - Lombok 적극 활용
    - Spring Boot 어노테이션 기반 개발

## 인바디 결과지 OCR 초안 기능

- 엔드포인트: `POST /api/inbody/upload` (`multipart/form-data`, `file` 필드에 이미지 전송)
- 흐름: 이미지 업로드 → S3 저장 → S3에서 다시 다운로드 → OpenCV 전처리(문서 검출/보정, 조명/노이즈 제거, 대비 강화, 리샘플링) → Gemini Vision OCR → **초안 데이터(draft)** 만 반환 (DB 미저장)
- 품질 향상: blur/skew 감지, 저품질 시 고강도 전처리 재시도, 원본/전처리 이미지를 동시에 Gemini에 전달해 가장 신뢰도 높은 값을 단일 패스로 추출
- 응답: `imageUrl`(S3 경로) + `draftData`(최종 JSON). 프론트는 `draftData`를 "인바디 수기 입력" 기본값으로 사용 후 `POST /api/inbody`를 호출해 저장.
- 환경 변수
  - `AWS_REGION`, `AWS_S3_BUCKET`, `AWS_S3_INBODY_FOLDER`(선택), `AWS_S3_BASE_URL`(선택)
  - `GEMINI_API_KEY` (bashrc에 이미 등록되어 있어야 함)
- 기존 수기 입력 API (`POST /api/inbody`)는 그대로 사용합니다.
