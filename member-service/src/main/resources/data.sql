-- 초기 데이터 삽입 (개발/테스트용)
-- H2 데이터베이스와 JPA가 테이블을 생성한 후 실행됩니다.

-- 기본 관리자 및 테스트 사용자 데이터
INSERT INTO members (username, password, email, full_name, phone_number, status) 
VALUES 
('admin', 'admin123', 'admin@example.com', '관리자', '010-1234-5678', 'ACTIVE'),
('user1', 'password123', 'user1@example.com', '홍길동', '010-1111-2222', 'ACTIVE'),
('user2', 'password123', 'user2@example.com', '김영희', '010-3333-4444', 'ACTIVE'),
('user3', 'password123', 'user3@example.com', '박철수', '010-5555-6666', 'INACTIVE'),
('user4', 'password123', 'user4@example.com', '이미영', '010-7777-8888', 'ACTIVE');

-- 추가 테스트 데이터
INSERT INTO members (username, password, email, full_name, phone_number, status) 
VALUES 
('testuser1', 'test123', 'test1@test.com', '테스트사용자1', '010-1000-1001', 'ACTIVE'),
('testuser2', 'test123', 'test2@test.com', '테스트사용자2', '010-1000-1002', 'ACTIVE'),
('testuser3', 'test123', 'test3@test.com', '테스트사용자3', '010-1000-1003', 'SUSPENDED');