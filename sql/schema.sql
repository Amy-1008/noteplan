-- 创建数据库
CREATE DATABASE IF NOT EXISTS noteplan
DEFAULT CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE noteplan;
SET NAMES utf8mb4;

-- 清理旧表
DROP TABLE IF EXISTS schedule_note;
DROP TABLE IF EXISTS note_tag;
DROP TABLE IF EXISTS note_version;
DROP TABLE IF EXISTS schedule;
DROP TABLE IF EXISTS note;
DROP TABLE IF EXISTS tag;

-- 标签表
CREATE TABLE tag (
                     id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                     name VARCHAR(100) NOT NULL COMMENT '标签名，唯一',
                     `rank` TINYINT NOT NULL DEFAULT 0 COMMENT '标签级别 0普通 1重要',
                     create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                     UNIQUE KEY uk_tag_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='标签字典';

-- 笔记表
CREATE TABLE note (
                      id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                      title VARCHAR(500) DEFAULT NULL COMMENT '标题，空则从正文截取',
                      content VARCHAR(5000) NOT NULL COMMENT '正文，最大5000字符',
                      create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                      update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                      status TINYINT NOT NULL DEFAULT 0 COMMENT '0正常 1已删除',
                      `rank` INT NOT NULL DEFAULT 0 COMMENT '排序/重要性',
                      KEY idx_note_update_time (update_time),
                      KEY idx_note_status (status),
                      KEY idx_note_title (title(191))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='备忘录';

-- 笔记历史版本表
CREATE TABLE note_version (
                              id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                              note_id BIGINT NOT NULL,
                              version_no INT NOT NULL COMMENT '版本号，从1递增',
                              title VARCHAR(500) DEFAULT NULL COMMENT '该版本创建时的标题',
                              content VARCHAR(5000) NOT NULL,
                              tag_id BIGINT DEFAULT NULL COMMENT '该版本创建时的标签ID',
                              save_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              UNIQUE KEY uk_note_version (note_id, version_no),
                              KEY idx_nv_note_id (note_id),
                              CONSTRAINT fk_nv_note FOREIGN KEY (note_id) REFERENCES note (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='备忘录历史版本';

-- 日程表
CREATE TABLE schedule (
                          id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                          title VARCHAR(500) NOT NULL COMMENT '日程标题',
                          start_time DATETIME DEFAULT NULL COMMENT '开始时间，NULL表示是时间点（无开始时间）',
                          end_time DATETIME NOT NULL COMMENT '结束时间（时间点时即为此时间，时间段时为结束时间，精确到分钟）',
                          repeat_rule VARCHAR(30) NOT NULL DEFAULT 'none' COMMENT '重复规则：none/daily/weekly/monthly/yearly/workday/holiday',
                          remark VARCHAR(2000) DEFAULT NULL COMMENT '备注',
                          completed TINYINT NOT NULL DEFAULT 0 COMMENT '0未完成 1已完成（重复日程完成后不会变成1，而是修改时间）',
                          status TINYINT NOT NULL DEFAULT 0 COMMENT '0正常 1已删除',
                          `rank` INT NOT NULL DEFAULT 0 COMMENT '优先级/排序',
                          create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                          KEY idx_schedule_end_time (end_time),
                          KEY idx_schedule_status (status),
                          KEY idx_schedule_completed (completed),
                          KEY idx_schedule_repeat (repeat_rule)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='日程表';

-- 日程关联笔记表（多对多）
CREATE TABLE schedule_note (
                               id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                               schedule_id BIGINT NOT NULL COMMENT '日程ID',
                               note_id BIGINT DEFAULT NULL COMMENT '笔记ID，NULL表示原笔记已删除',
                               create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               UNIQUE KEY uk_schedule_note (schedule_id, note_id),
                               KEY idx_sn_schedule (schedule_id),
                               KEY idx_sn_note (note_id),
                               CONSTRAINT fk_sn_schedule FOREIGN KEY (schedule_id) REFERENCES schedule (id) ON DELETE CASCADE,
                               CONSTRAINT fk_sn_note FOREIGN KEY (note_id) REFERENCES note (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='日程关联笔记';

-- 触发器，一个日程最多关联 5 篇笔记
DELIMITER $$

CREATE TRIGGER check_schedule_note_limit
    BEFORE INSERT ON schedule_note
    FOR EACH ROW
BEGIN
    DECLARE note_count INT;
    SELECT COUNT(*) INTO note_count
    FROM schedule_note
    WHERE schedule_id = NEW.schedule_id;
    IF note_count >= 5 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = '每个日程最多只能关联 5 篇笔记';
END IF;
END$$

DELIMITER ;

-- 标签关联表
CREATE TABLE note_tag (
                          id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                          target_id BIGINT NOT NULL COMMENT '笔记ID 或 日程ID',
                          target_type VARCHAR(20) NOT NULL COMMENT 'NOTE 或 SCHEDULE',
                          tag_id BIGINT NOT NULL,
                          UNIQUE KEY uk_note_tag (target_id, target_type, tag_id),
                          KEY idx_nt_tag (tag_id),
                          CONSTRAINT fk_nt_tag FOREIGN KEY (tag_id) REFERENCES tag (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='标签关联';

SHOW TABLES;