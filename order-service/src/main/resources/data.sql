-- 초기 주문 데이터 삽입 (개발/테스트용)
-- H2 데이터베이스와 JPA가 테이블을 생성한 후 실행됩니다.

-- 기본 테스트 주문 데이터 (Member Service의 회원 ID와 연동)
INSERT INTO orders (member_id, product_name, quantity, unit_price, total_amount, status, order_memo) 
VALUES 
(1, '노트북', 1, 1500000.00, 1500000.00, 'CONFIRMED', '업무용 노트북 주문'),
(1, '마우스', 2, 50000.00, 100000.00, 'DELIVERED', '무선 마우스 2개'),
(2, '키보드', 1, 120000.00, 120000.00, 'SHIPPED', '기계식 키보드'),
(2, '모니터', 2, 300000.00, 600000.00, 'PROCESSING', '27인치 모니터 2대'),
(3, '헤드셋', 1, 80000.00, 80000.00, 'PENDING', '게이밍 헤드셋'),
(4, '태블릿', 1, 800000.00, 800000.00, 'CONFIRMED', '업무용 태블릿'),
(5, '스마트폰', 1, 1200000.00, 1200000.00, 'DELIVERED', '최신 스마트폰'),
(1, 'USB 메모리', 5, 20000.00, 100000.00, 'CANCELLED', '32GB USB 메모리 5개 - 취소됨');

-- 추가 테스트 데이터 (다양한 상태와 금액)
INSERT INTO orders (member_id, product_name, quantity, unit_price, total_amount, status, order_memo) 
VALUES 
(2, '외장하드', 1, 150000.00, 150000.00, 'PENDING', '1TB 외장하드'),
(3, '웹캠', 1, 100000.00, 100000.00, 'SHIPPED', '화상회의용 웹캠'),
(4, '책상', 1, 200000.00, 200000.00, 'PROCESSING', '높이조절 책상'),
(5, '의자', 1, 350000.00, 350000.00, 'CONFIRMED', '게이밍 의자');


