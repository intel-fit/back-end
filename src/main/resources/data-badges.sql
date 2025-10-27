-- 뱃지 초기 데이터 삽입

-- 운동 관련 뱃지
INSERT INTO badges (badge_type, name, description, icon_url, required_count, display_order)
VALUES 
    ('EXERCISE_10', '운동 입문자', '10회 운동을 완료한 입문자! 시작이 반이에요! 💪', '/badges/exercise_10.png', 10, 1),
    ('EXERCISE_30', '운동 도전자', '30회 운동을 완료한 도전자! 대단해요! 🔥', '/badges/exercise_30.png', 30, 2),
    ('EXERCISE_50', '운동 열정가', '50회 운동을 완료한 열정가! 멋져요! ⭐', '/badges/exercise_50.png', 50, 3),
    ('EXERCISE_100', '운동 마스터', '100회 운동을 완료한 마스터! 최고예요! 👑', '/badges/exercise_100.png', 100, 4),
    ('EXERCISE_STREAK_7', '7일 연속 운동', '7일 연속으로 운동한 끈기의 소유자! 🔥', '/badges/streak_7.png', 7, 5),
    ('EXERCISE_STREAK_30', '30일 연속 운동', '30일 연속으로 운동한 초인! 정말 대단해요! 💎', '/badges/streak_30.png', 30, 6);

-- 식단 관련 뱃지
INSERT INTO badges (badge_type, name, description, icon_url, required_count, display_order)
VALUES 
    ('MEAL_10', '식단 기록 시작', '10회 식단을 기록한 건강 관리자! 🍽️', '/badges/meal_10.png', 10, 7),
    ('MEAL_30', '식단 관리자', '30회 식단을 기록한 관리 전문가! 📝', '/badges/meal_30.png', 30, 8),
    ('MEAL_50', '영양 전문가', '50회 식단을 기록한 영양 전문가! 🥗', '/badges/meal_50.png', 50, 9),
    ('MEAL_100', '식단 마스터', '100회 식단을 기록한 마스터! 완벽해요! 👨‍🍳', '/badges/meal_100.png', 100, 10),
    ('CALORIE_GOAL_30', '칼로리 달인', '30일간 칼로리 목표를 달성한 달인! 🎯', '/badges/calorie_30.png', 30, 11),
    ('HEALTHY_EATING_7', '건강식 챔피언', '7일간 건강한 식단을 유지한 챔피언! 🌿', '/badges/healthy_7.png', 7, 12);

-- 체중 관련 뱃지
INSERT INTO badges (badge_type, name, description, icon_url, required_count, display_order)
VALUES 
    ('WEIGHT_GOAL_ACHIEVED', '목표 체중 달성', '목표 체중을 달성한 승리자! 축하합니다! 🎉', '/badges/weight_goal.png', 1, 13),
    ('WEIGHT_LOSS_5KG', '5kg 감량 성공', '5kg 감량에 성공한 다이어터! 대단해요! 📉', '/badges/loss_5kg.png', 1, 14),
    ('WEIGHT_LOSS_10KG', '10kg 감량 성공', '10kg 감량에 성공한 초인! 정말 멋져요! 🏆', '/badges/loss_10kg.png', 1, 15),
    ('MUSCLE_GAIN_5KG', '5kg 증량 성공', '5kg 근육 증량에 성공한 강자! 💪', '/badges/gain_5kg.png', 1, 16),
    ('BODY_FAT_REDUCED', '체지방 5% 감소', '체지방률 5% 감소에 성공! 최고예요! 🔥', '/badges/fat_reduced.png', 1, 17);

-- 종합 관련 뱃지
INSERT INTO badges (badge_type, name, description, icon_url, required_count, display_order)
VALUES 
    ('BEGINNER', '입문자', '인텔핏 여정을 시작한 입문자! 환영합니다! 🌱', '/badges/beginner.png', 1, 18),
    ('INTERMEDIATE', '중급자', '꾸준히 성장하고 있는 중급자! 멋져요! 🌿', '/badges/intermediate.png', 1, 19),
    ('ADVANCED', '고급자', '실력이 뛰어난 고급자! 대단해요! 🌳', '/badges/advanced.png', 1, 20),
    ('MASTER', '마스터', '모든 것을 정복한 마스터! 당신은 전설입니다! 👑', '/badges/master.png', 1, 21);
