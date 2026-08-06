-- 자동 로그인 테이블 생성 (없으면 로그아웃 시 에러)
CREATE TABLE persistent_logins (
    username VARCHAR(64) NOT NULL,
    series VARCHAR(64) PRIMARY KEY,
    token VARCHAR(64) NOT NULL,
    last_used TIMESTAMP NOT NULL
);

-- 기본 근무 정책 데이터 삽입 (ID: 1)
INSERT INTO work_schedule_policy (
    work_schedule_policy_no,
    start_time_min,
    start_time_max,
    work_duration,
    week_work_min_time,
    week_work_max_time,
    apply_to_all
) VALUES (
    1,
    '09:00:00',
    '18:00:00',
    8.0,
    40.0,
    52.0,
    'Y'
);

-- 권한
INSERT INTO role (role_no, role_name, role_nickname) VALUES (1, 'ROLE_ADMIN', '관리자');
INSERT INTO role (role_no, role_name, role_nickname) VALUES (2, 'ROLE_USER', '사용자');
INSERT INTO role (role_no, role_name, role_nickname) VALUES (3, 'ROLE_HR', '인사담당자');

-- 부서
INSERT INTO dept (dept_no, dept_name, dept_status) VALUES (1, '임원', 1);
INSERT INTO dept (dept_no, dept_name, dept_status) VALUES (2, '영업부', 1);
INSERT INTO dept (dept_no, dept_name, dept_status) VALUES (3, '품질부', 1);
INSERT INTO dept (dept_no, dept_name, dept_status) VALUES (4, '개발부', 1);
INSERT INTO dept (dept_no, dept_name, dept_status) VALUES (5, '기술부', 1);
INSERT INTO dept (dept_no, dept_name, dept_status) VALUES (6, '해석부', 1);
INSERT INTO dept (dept_no, dept_name, dept_status) VALUES (7, '연구소', 1);

-- 직급
INSERT INTO pos (pos_no, pos_name, pos_order) VALUES (1, '대표이사', 1);
INSERT INTO pos (pos_no, pos_name, pos_order) VALUES (2, '전무이사', 2);
INSERT INTO pos (pos_no, pos_name, pos_order) VALUES (3, '부사장', 3);
INSERT INTO pos (pos_no, pos_name, pos_order) VALUES (4, '상무', 4);
INSERT INTO pos (pos_no, pos_name, pos_order) VALUES (5, '이사', 5);
INSERT INTO pos (pos_no, pos_name, pos_order) VALUES (6, '부장', 6);
INSERT INTO pos (pos_no, pos_name, pos_order) VALUES (7, '수석', 7);
INSERT INTO pos (pos_no, pos_name, pos_order) VALUES (8, '차장', 8);
INSERT INTO pos (pos_no, pos_name, pos_order) VALUES (9, '책임', 9);
INSERT INTO pos (pos_no, pos_name, pos_order) VALUES (10, '과장', 10);
INSERT INTO pos (pos_no, pos_name, pos_order) VALUES (11, '선임', 11);
INSERT INTO pos (pos_no, pos_name, pos_order) VALUES (12, '대리', 12);
INSERT INTO pos (pos_no, pos_name, pos_order) VALUES (13, '주임', 13);
INSERT INTO pos (pos_no, pos_name, pos_order) VALUES (14, '사원', 14);
INSERT INTO pos (pos_no, pos_name, pos_order) VALUES (15, '프로', 15);

-- Create an Admin Account (ID: admin / Password: 1qaz2wsx!@)
INSERT INTO member (member_id, member_pw, member_name, status, reg_date, role_no, dept_no, pos_no, annual_leave)
VALUES ('admin', '$2a$10$TpIx3QDLm2duTuR9QUGIj.Mmheth6J89QoyXkzMOv7BxY2KqtkoKS', '관리자', 100, '2026-05-28', 1, NULL, NULL, 15.0);

-- Create a Regular User Account (ID: user / Password: 1234)
INSERT INTO member (member_id, member_pw, member_name, status, reg_date, role_no, dept_no, pos_no, annual_leave)
VALUES ('csyang', '$2a$10$WdH.UkTL7ZTLamfb8n7NIuMNbST3k6YrgG5o5wpe3guE5E6yW6jyu', '양철수', 100, '2026-05-28', 3, 1, 1, 15.0);

INSERT INTO member (member_id, member_pw, member_name, status, reg_date, role_no, dept_no, pos_no, annual_leave)
VALUES ('isyang', '$2a$10$WdH.UkTL7ZTLamfb8n7NIuMNbST3k6YrgG5o5wpe3guE5E6yW6jyu', '양인수', 100, '2026-05-28', 2, 1, 2, 15.0);

INSERT INTO member (member_id, member_pw, member_name, status, reg_date, role_no, dept_no, pos_no, annual_leave)
VALUES ('hclee', '$2a$10$WdH.UkTL7ZTLamfb8n7NIuMNbST3k6YrgG5o5wpe3guE5E6yW6jyu', '이호창', 100, '2026-05-28', 2, 2, 5, 15.0);

INSERT INTO member (member_id, member_pw, member_name, status, reg_date, role_no, dept_no, pos_no, annual_leave)
VALUES ('gskang', '$2a$10$WdH.UkTL7ZTLamfb8n7NIuMNbST3k6YrgG5o5wpe3guE5E6yW6jyu', '강근식', 100, '2026-05-28', 2, 3, 6, 15.0);

INSERT INTO member (member_id, member_pw, member_name, status, reg_date, role_no, dept_no, pos_no, annual_leave)
VALUES ('khchoi', '$2a$10$WdH.UkTL7ZTLamfb8n7NIuMNbST3k6YrgG5o5wpe3guE5E6yW6jyu', '최광훈', 100, '2026-05-28', 2, 4, 6, 15.0);

INSERT INTO member (member_id, member_pw, member_name, status, reg_date, role_no, dept_no, pos_no, annual_leave)
VALUES ('dcjung', '$2a$10$WdH.UkTL7ZTLamfb8n7NIuMNbST3k6YrgG5o5wpe3guE5E6yW6jyu', '정대창', 100, '2026-05-28', 2, 5, 8, 15.0);

INSERT INTO member (member_id, member_pw, member_name, status, reg_date, role_no, dept_no, pos_no, annual_leave)
VALUES ('swjang', '$2a$10$WdH.UkTL7ZTLamfb8n7NIuMNbST3k6YrgG5o5wpe3guE5E6yW6jyu', '장성원', 100, '2026-05-28', 2, 3, 8, 15.0);

INSERT INTO member (member_id, member_pw, member_name, status, reg_date, role_no, dept_no, pos_no, annual_leave)
VALUES ('yhkwon', '$2a$10$WdH.UkTL7ZTLamfb8n7NIuMNbST3k6YrgG5o5wpe3guE5E6yW6jyu', '권용호', 100, '2026-05-28', 2, 7, 9, 15.0);

INSERT INTO member (member_id, member_pw, member_name, status, reg_date, role_no, dept_no, pos_no, annual_leave)
VALUES ('msseo', '$2a$10$WdH.UkTL7ZTLamfb8n7NIuMNbST3k6YrgG5o5wpe3guE5E6yW6jyu', '서민석', 100, '2026-05-28', 2, 5, 15, 15.0);

INSERT INTO member (member_id, member_pw, member_name, status, reg_date, role_no, dept_no, pos_no, annual_leave)
VALUES ('jwpark', '$2a$10$WdH.UkTL7ZTLamfb8n7NIuMNbST3k6YrgG5o5wpe3guE5E6yW6jyu', '박진우', 100, '2026-05-28', 2, 5, 15, 15.0);

INSERT INTO member (member_id, member_pw, member_name, status, reg_date, role_no, dept_no, pos_no, annual_leave)
VALUES ('gyyang', '$2a$10$WdH.UkTL7ZTLamfb8n7NIuMNbST3k6YrgG5o5wpe3guE5E6yW6jyu', '양근영', 100, '2026-05-28', 2, 7, 11, 15.0);

INSERT INTO member (member_id, member_pw, member_name, status, reg_date, role_no, dept_no, pos_no, annual_leave)
VALUES ('hhshin', '$2a$10$WdH.UkTL7ZTLamfb8n7NIuMNbST3k6YrgG5o5wpe3guE5E6yW6jyu', '신현호', 100, '2026-05-28', 2, 7, 11, 15.0);

INSERT INTO member (member_id, member_pw, member_name, status, reg_date, role_no, dept_no, pos_no, annual_leave)
VALUES ('nyan', '$2a$10$WdH.UkTL7ZTLamfb8n7NIuMNbST3k6YrgG5o5wpe3guE5E6yW6jyu', '안나영', 100, '2026-05-28', 2, 6, 10, 15.0);

INSERT INTO member (member_id, member_pw, member_name, status, reg_date, role_no, dept_no, pos_no, annual_leave)
VALUES ('gmlee', '$2a$10$WdH.UkTL7ZTLamfb8n7NIuMNbST3k6YrgG5o5wpe3guE5E6yW6jyu', '이경민', 100, '2026-05-28', 2, 6, 14, 15.0);

-- 1번부터 36번까지의 시스템 전체 기능 리스트 주입
INSERT INTO func (func_no, func_name, func_code, func_status, parent_func_no, func_order, reg_date) VALUES
(1, '기본 홈', 'HOME', 1, NULL, 1, NOW()),
(2, '근태관리 대시보드', 'ATTEND_DASH', 1, NULL, 2, NOW()),
(3, '근태이력 조회', 'ATTEND_HISTORY', 1, NULL, 3, NOW()),
(4, '내 근태 현황', 'ATTEND_MY_STATUS', 1, NULL, 4, NOW()),
(5, '근태 설정', 'ATTEND_SET', 1, NULL, 5, NOW()),
(6, '전자결재 홈', 'APP_HOME', 1, NULL, 6, NOW()),
(7, '기안하기', 'APP_CREATE', 1, NULL, 7, NOW()),
(8, '결재대기함', 'APP_WAIT', 1, NULL, 8, NOW()),
(9, '결재진행함', 'APP_PROGRESS', 1, NULL, 9, NOW()),
(10, '결재완료함', 'APP_COMPLETE', 1, NULL, 10, NOW()),
(11, '반려함', 'APP_REJECT', 1, NULL, 11, NOW()),
(12, '인사 사원등록', 'MEMBER_ADMIN_C', 1, NULL, 12, NOW()),
(13, '인사 부서관리', 'DEPT_ADMIN_CRU', 1, NULL, 13, NOW()),
(14, '인사 직급관리', 'POS_ADMIN_CRUD', 1, NULL, 14, NOW()),
(15, '인사 인사관리', 'MEMBER_ADMIN_RU', 1, NULL, 15, NOW()),
(16, '역할 권한관리', 'SYS_ROLE_MGMT', 1, NULL, 16, NOW()),
(17, '전자결재', 'SYS_MENU_CONTROL', 1, NULL, 17, NOW()),
(18, '근태관리', 'SYS_COMPANY', 1, NULL, 18, NOW()),
(19, '근무이력', 'SYS_CONFIG', 1, NULL, 19, NOW()),
(20, '일정 조회', 'CALENDAR_USER', 1, NULL, 20, NOW()),
(21, '채팅방 진입', 'CHAT_USER', 1, NULL, 21, NOW()),
(22, '공유문서함 조회', 'SHARED_USER', 1, NULL, 22, NOW()),
(23, '문서 공유관리', 'SHARED_MGMT', 1, NULL, 23, NOW()),
(24, '공지사항 조회', 'NOTICE_R', 1, NULL, 24, NOW()),
(25, '공지사항 작성/수정/삭제', 'NOTICE_CRU', 1, NULL, 25, NOW()),
(26, '자유게시판 조회', 'BOARD_R', 1, NULL, 26, NOW()),
(27, '자유게시판 작성/수정/삭제', 'BOARD_CRU', 1, NULL, 27, NOW()),
(28, '회의실 관리자', 'MEETING_ADMIN', 1, NULL, 28, NOW()),
(29, '회의실 예약', 'MEETING_USER', 1, NULL, 29, NOW()),
(30, '일정 등록', 'CALENDAR_CREATE', 1, NULL, 30, NOW()),
(31, '사내복지 관리자', 'WELFARE_ADMIN', 1, NULL, 31, NOW()),
(32, '자산 예약', 'ASSET_RESERVE', 1, NULL, 32, NOW()),
(33, '연차 관리', 'SYS_FUNC_CONTROL', 1, NULL, 33, NOW()),
(34, '회의실 예약 보조', 'ROOM_RESERVE', 1, NULL, 34, NOW()),
(35, '기타 설정', 'ETC_SETTING', 1, NULL, 35, NOW()),
(36, '회의실 예약 관리자', 'MEETING_RESERVATION_ADMIN', 1, NULL, 36, NOW()),
(37, '전체 결재 관리', 'APP_ADMIN_LIST', 1, NULL, 37, NOW());

-- 관리자 권한 (ROLE_ADMIN, role_no: 1) 에게 1번부터 37번까지 모든 기능 매핑 (마스터 권한 부여)
INSERT INTO func_mapping (func_no, role_no) VALUES
(1, 1), (2, 1), (3, 1), (4, 1), (5, 1), (6, 1), (7, 1), (8, 1), (9, 1), (10, 1),
(11, 1), (12, 1), (13, 1), (14, 1), (15, 1), (16, 1), (17, 1), (18, 1), (19, 1), (20, 1),
(21, 1), (22, 1), (23, 1), (24, 1), (25, 1), (26, 1), (27, 1), (28, 1), (29, 1), (30, 1),
(31, 1), (32, 1), (33, 1), (34, 1), (35, 1), (36, 1), (37, 1);

-- 일반 사원 권한 (ROLE_USER, role_no: 2) 에게 기능 매핑
INSERT INTO func_mapping (func_no, role_no) VALUES
(17, 2), -- 전자결재
(20, 2), -- 일정 조회
(21, 2); -- 채팅방 진입

-- 인사 담당자 권한 (ROLE_HR, role_no: 3) 에게 기능 매핑 (인사 사원관리/수정 권한 + 일반 기능 + 전체 결재 관리 권한)
INSERT INTO func_mapping (func_no, role_no) VALUES
(15, 3), -- 인사 관리
(17, 3), -- 전자결재
(20, 3), -- 일정 조회
(21, 3), -- 채팅방 진입
(37, 3); -- 전체 결재 관리

-- 대리 작성자 사원번호 (FK)
ALTER TABLE approval ADD COLUMN proxy_drafter BIGINT NULL;
-- 대리 기안 여부 ('Y' / 'N')
ALTER TABLE approval ADD COLUMN is_proxy VARCHAR(1) DEFAULT 'N';
-- FK 외래키 제약조건 설정 (선택 사항)
ALTER TABLE approval ADD CONSTRAINT fk_approval_proxy_drafter FOREIGN KEY (proxy_drafter) REFERENCES member(member_no);

-- 참조/회람 확인 상태 ('Y' / 'N')
ALTER TABLE appr_referencer ADD COLUMN referencer_status CHAR(1) DEFAULT 'N';

-- --------------------------------------------------------
-- 양식/부서별 결재선 테이블
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS approval_line (
    line_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    approval_form_no BIGINT NOT NULL,
    dept_no BIGINT NULL,
    line_name VARCHAR(100) NOT NULL,
    use_yn VARCHAR(1) DEFAULT 'Y',
    FOREIGN KEY (approval_form_no) REFERENCES approval_form(approval_form_no),
    FOREIGN KEY (dept_no) REFERENCES dept(dept_no)
);

CREATE TABLE IF NOT EXISTS approval_line_detail (
    detail_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    line_id BIGINT NOT NULL,
    member_no BIGINT NOT NULL,
    appr_type VARCHAR(20) NOT NULL,
    appr_order INT NOT NULL,
    FOREIGN KEY (line_id) REFERENCES approval_line(line_id),
    FOREIGN KEY (member_no) REFERENCES member(member_no)
);

-- 전사 공통 일일업무일지 (1차: 양인수 전무이사, 2차: 양철수 대표이사)
INSERT INTO approval_line (line_id, approval_form_no, dept_no, line_name, use_yn)
VALUES (1, 1, NULL, '일일업무일지', 'Y')
ON DUPLICATE KEY UPDATE line_name = VALUES(line_name);

INSERT INTO approval_line_detail (line_id, member_no, appr_type, appr_order)
VALUES (1, 3, 'APPROVER', 1),
       (1, 2, 'APPROVER', 2)
ON DUPLICATE KEY UPDATE appr_type = VALUES(appr_type);

-- [영업부 (dept_no = 2)] 일일업무일지 (1차: 이호창 이사, 2차: 양인수 전무이사, 3차: 양철수 대표이사)
INSERT INTO approval_line (line_id, approval_form_no, dept_no, line_name, use_yn)
VALUES (2, 1, 2, '일일업무일지', 'Y')
ON DUPLICATE KEY UPDATE line_name = VALUES(line_name);

INSERT INTO approval_line_detail (line_id, member_no, appr_type, appr_order)
VALUES (2, 4, 'APPROVER', 1),
       (2, 3, 'APPROVER', 2),
       (2, 2, 'APPROVER', 3)
ON DUPLICATE KEY UPDATE appr_type = VALUES(appr_type);

-- [품질부 (dept_no = 3)] 일일업무일지 (1차: 강근식 부장, 2차: 양인수 전무이사, 3차: 양철수 대표이사)
INSERT INTO approval_line (line_id, approval_form_no, dept_no, line_name, use_yn)
VALUES (3, 1, 3, '일일업무일지', 'Y')
ON DUPLICATE KEY UPDATE line_name = VALUES(line_name);

INSERT INTO approval_line_detail (line_id, member_no, appr_type, appr_order)
VALUES (3, 5, 'APPROVER', 1),
       (3, 3, 'APPROVER', 2),
       (3, 2, 'APPROVER', 3)
ON DUPLICATE KEY UPDATE appr_type = VALUES(appr_type);

-- [개발부 (dept_no = 4)] 일일업무일지 (1차: 최광훈 부장, 2차: 양인수 전무이사, 3차: 양철수 대표이사)
INSERT INTO approval_line (line_id, approval_form_no, dept_no, line_name, use_yn)
VALUES (4, 1, 4, '일일업무일지', 'Y')
ON DUPLICATE KEY UPDATE line_name = VALUES(line_name);

INSERT INTO approval_line_detail (line_id, member_no, appr_type, appr_order)
VALUES (4, 6, 'APPROVER', 1),
       (4, 3, 'APPROVER', 2),
       (4, 2, 'APPROVER', 3)
ON DUPLICATE KEY UPDATE appr_type = VALUES(appr_type);

-- [기술부 (dept_no = 5)] 일일업무일지 (1차: 정대창 차장, 2차: 양인수 전무이사, 3차: 양철수 대표이사)
INSERT INTO approval_line (line_id, approval_form_no, dept_no, line_name, use_yn)
VALUES (5, 1, 5, '일일업무일지', 'Y')
ON DUPLICATE KEY UPDATE line_name = VALUES(line_name);

INSERT INTO approval_line_detail (line_id, member_no, appr_type, appr_order)
VALUES (5, 7, 'APPROVER', 1),
       (5, 3, 'APPROVER', 2),
       (5, 2, 'APPROVER', 3)
ON DUPLICATE KEY UPDATE appr_type = VALUES(appr_type);

-- [해석부 (dept_no = 6)] 일일업무일지 (1차: 안나영 과장, 2차: 양인수 전무이사, 3차: 양철수 대표이사)
INSERT INTO approval_line (line_id, approval_form_no, dept_no, line_name, use_yn)
VALUES (6, 1, 6, '일일업무일지', 'Y')
ON DUPLICATE KEY UPDATE line_name = VALUES(line_name);

INSERT INTO approval_line_detail (line_id, member_no, appr_type, appr_order)
VALUES (6, 14, 'APPROVER', 1),
       (6, 3, 'APPROVER', 2),
       (6, 2, 'APPROVER', 3)
ON DUPLICATE KEY UPDATE appr_type = VALUES(appr_type);

-- [연구소 (dept_no = 7)] 일일업무일지 (1차: 양인수 전무이사, 2차: 양철수 대표이사)
INSERT INTO approval_line (line_id, approval_form_no, dept_no, line_name, use_yn)
VALUES (7, 1, 7, '일일업무일지', 'Y')
ON DUPLICATE KEY UPDATE line_name = VALUES(line_name);

INSERT INTO approval_line_detail (line_id, member_no, appr_type, appr_order)
VALUES (7, 3, 'APPROVER', 1),
       (7, 2, 'APPROVER', 2)
ON DUPLICATE KEY UPDATE appr_type = VALUES(appr_type);
