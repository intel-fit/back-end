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
