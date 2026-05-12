-- NotePlan 数据库初始化（MySQL 8.0 + InnoDB）
-- 首次执行前请先创建库（按需调整字符集）：
-- CREATE DATABASE IF NOT EXISTS noteplan DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE noteplan;

SET NAMES utf8mb4;

-- 备忘录（当前内容）
DROP TABLE IF EXISTS note_tag;
DROP TABLE IF EXISTS note_version;
DROP TABLE IF EXISTS schedule;
DROP TABLE IF EXISTS note;
DROP TABLE IF EXISTS tag;

CREATE TABLE tag (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '标签名，唯一',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_tag_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='标签字典';

CREATE TABLE note (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(500) DEFAULT NULL COMMENT '标题，空则从正文截取',
    content VARCHAR(5000) NOT NULL COMMENT '正文，最大5000字符',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0正常 1已删除',
    rank INT NOT NULL DEFAULT 0 COMMENT '扩展：排序/重要性',
    KEY idx_note_update_time (update_time),
    KEY idx_note_status (status),
    KEY idx_note_title (title(191))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='备忘录';

CREATE TABLE note_version (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    note_id BIGINT NOT NULL,
    version_no INT NOT NULL COMMENT '版本号，从1递增',
    content VARCHAR(5000) NOT NULL,
    save_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_note_version (note_id, version_no),
    KEY idx_nv_note_id (note_id),
    CONSTRAINT fk_nv_note FOREIGN KEY (note_id) REFERENCES note (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='备忘录历史版本';

CREATE TABLE schedule (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    start_time DATETIME NOT NULL COMMENT '提醒时间，精确到分钟',
    remark VARCHAR(2000) DEFAULT NULL,
    completed TINYINT NOT NULL DEFAULT 0 COMMENT '0未完成 1完成',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0正常 1已删除',
    linked_note_id BIGINT DEFAULT NULL,
    rank INT NOT NULL DEFAULT 0 COMMENT '扩展字段',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_schedule_start (start_time),
    KEY idx_schedule_status (status),
    CONSTRAINT fk_schedule_note FOREIGN KEY (linked_note_id) REFERENCES note (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='日程';

-- 笔记或日程与标签多对多：target_type = NOTE | SCHEDULE
CREATE TABLE note_tag (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    target_id BIGINT NOT NULL,
    target_type VARCHAR(20) NOT NULL,
    tag_id BIGINT NOT NULL,
    UNIQUE KEY uk_note_tag (target_id, target_type, tag_id),
    KEY idx_nt_tag (tag_id),
    CONSTRAINT fk_nt_tag FOREIGN KEY (tag_id) REFERENCES tag (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='标签关联';
