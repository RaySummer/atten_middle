-- 开启扩展（如果已存在会跳过）
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 定义一个通用的操作：添加字段 -> 设置默认值 -> 修复旧数据 -> 设置非空约束 -> 添加唯一索引
-- 1. attendance_group
ALTER TABLE attendance_group ADD COLUMN IF NOT EXISTS uuid VARCHAR(36);
UPDATE attendance_group SET uuid = uuid_generate_v4()::text WHERE uuid IS NULL;
ALTER TABLE attendance_group ALTER COLUMN uuid SET DEFAULT uuid_generate_v4()::text;
ALTER TABLE attendance_group ALTER COLUMN uuid SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_attendance_group_uuid ON attendance_group(uuid);

-- 2. attendance_logs
ALTER TABLE attendance_logs ADD COLUMN IF NOT EXISTS uuid VARCHAR(36);
UPDATE attendance_logs SET uuid = uuid_generate_v4()::text WHERE uuid IS NULL;
ALTER TABLE attendance_logs ALTER COLUMN uuid SET DEFAULT uuid_generate_v4()::text;
ALTER TABLE attendance_logs ALTER COLUMN uuid SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_attendance_logs_uuid ON attendance_logs(uuid);

-- 3. device_commands
ALTER TABLE device_commands ADD COLUMN IF NOT EXISTS uuid VARCHAR(36);
UPDATE device_commands SET uuid = uuid_generate_v4()::text WHERE uuid IS NULL;
ALTER TABLE device_commands ALTER COLUMN uuid SET DEFAULT uuid_generate_v4()::text;
ALTER TABLE device_commands ALTER COLUMN uuid SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_device_commands_uuid ON device_commands(uuid);

-- 4. card_template
ALTER TABLE card_template ADD COLUMN IF NOT EXISTS uuid VARCHAR(36);
UPDATE card_template SET uuid = uuid_generate_v4()::text WHERE uuid IS NULL;
ALTER TABLE card_template ALTER COLUMN uuid SET DEFAULT uuid_generate_v4()::text;
ALTER TABLE card_template ALTER COLUMN uuid SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_card_template_uuid ON card_template(uuid);

-- 5. devices
ALTER TABLE devices ADD COLUMN IF NOT EXISTS uuid VARCHAR(36);
UPDATE devices SET uuid = uuid_generate_v4()::text WHERE uuid IS NULL;
ALTER TABLE devices ALTER COLUMN uuid SET DEFAULT uuid_generate_v4()::text;
ALTER TABLE devices ALTER COLUMN uuid SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_devices_uuid ON devices(uuid);

-- 6. employee
ALTER TABLE employee ADD COLUMN IF NOT EXISTS uuid VARCHAR(36);
UPDATE employee SET uuid = uuid_generate_v4()::text WHERE uuid IS NULL;
ALTER TABLE employee ALTER COLUMN uuid SET DEFAULT uuid_generate_v4()::text;
ALTER TABLE employee ALTER COLUMN uuid SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_employee_uuid ON employee(uuid);

-- 7. employee_sync_queue
ALTER TABLE employee_sync_queue ADD COLUMN IF NOT EXISTS uuid VARCHAR(36);
UPDATE employee_sync_queue SET uuid = uuid_generate_v4()::text WHERE uuid IS NULL;
ALTER TABLE employee_sync_queue ALTER COLUMN uuid SET DEFAULT uuid_generate_v4()::text;
ALTER TABLE employee_sync_queue ALTER COLUMN uuid SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_employee_sync_queue_uuid ON employee_sync_queue(uuid);

-- 8. group_device_mapping
ALTER TABLE group_device_mapping ADD COLUMN IF NOT EXISTS uuid VARCHAR(36);
UPDATE group_device_mapping SET uuid = uuid_generate_v4()::text WHERE uuid IS NULL;
ALTER TABLE group_device_mapping ALTER COLUMN uuid SET DEFAULT uuid_generate_v4()::text;
ALTER TABLE group_device_mapping ALTER COLUMN uuid SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_group_device_mapping_uuid ON group_device_mapping(uuid);

-- 9. oa_employee
ALTER TABLE oa_employee ADD COLUMN IF NOT EXISTS uuid VARCHAR(36);
UPDATE oa_employee SET uuid = uuid_generate_v4()::text WHERE uuid IS NULL;
ALTER TABLE oa_employee ALTER COLUMN uuid SET DEFAULT uuid_generate_v4()::text;
ALTER TABLE oa_employee ALTER COLUMN uuid SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_oa_employee_uuid ON oa_employee(uuid);

-- 10. operation_log
ALTER TABLE operation_log ADD COLUMN IF NOT EXISTS uuid VARCHAR(36);
UPDATE operation_log SET uuid = uuid_generate_v4()::text WHERE uuid IS NULL;
ALTER TABLE operation_log ALTER COLUMN uuid SET DEFAULT uuid_generate_v4()::text;
ALTER TABLE operation_log ALTER COLUMN uuid SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_operation_log_uuid ON operation_log(uuid);

-- 11. sys_app_version
ALTER TABLE sys_app_version ADD COLUMN IF NOT EXISTS uuid VARCHAR(36);
UPDATE sys_app_version SET uuid = uuid_generate_v4()::text WHERE uuid IS NULL;
ALTER TABLE sys_app_version ALTER COLUMN uuid SET DEFAULT uuid_generate_v4()::text;
ALTER TABLE sys_app_version ALTER COLUMN uuid SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_sys_app_version_uuid ON sys_app_version(uuid);

-- 12. sys_config
ALTER TABLE sys_config ADD COLUMN IF NOT EXISTS uuid VARCHAR(36);
UPDATE sys_config SET uuid = uuid_generate_v4()::text WHERE uuid IS NULL;
ALTER TABLE sys_config ALTER COLUMN uuid SET DEFAULT uuid_generate_v4()::text;
ALTER TABLE sys_config ALTER COLUMN uuid SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_sys_config_uuid ON sys_config(uuid);

-- create admin company mapping
-- 2. 管理员与公司的多对多关联表
CREATE TABLE `admin_company_mapping` (
     `admin_id` BIGINT NOT NULL,
     `company_id` BIGINT NOT NULL,
     PRIMARY KEY (`admin_id`, `company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE admin_company_mapping ADD COLUMN IF NOT EXISTS uuid VARCHAR(36);
UPDATE admin_company_mapping SET uuid = uuid_generate_v4()::text WHERE uuid IS NULL;
ALTER TABLE admin_company_mapping ALTER COLUMN uuid SET DEFAULT uuid_generate_v4()::text;
ALTER TABLE admin_company_mapping ALTER COLUMN uuid SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_admin_company_mapping_uuid ON admin_company_mapping(uuid);

ALTER TABLE sys_company ADD COLUMN IF NOT EXISTS uuid VARCHAR(36);
UPDATE sys_company SET uuid = uuid_generate_v4()::text WHERE uuid IS NULL;
ALTER TABLE sys_company ALTER COLUMN uuid SET DEFAULT uuid_generate_v4()::text;
ALTER TABLE sys_company ALTER COLUMN uuid SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_sys_company_uuid ON sys_company(uuid);

ALTER TABLE sys_admin_user ADD COLUMN IF NOT EXISTS uuid VARCHAR(36);
UPDATE sys_admin_user SET uuid = uuid_generate_v4()::text WHERE uuid IS NULL;
ALTER TABLE sys_admin_user ALTER COLUMN uuid SET DEFAULT uuid_generate_v4()::text;
ALTER TABLE sys_admin_user ALTER COLUMN uuid SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_sys_admin_user_uuid ON sys_admin_user(uuid);

ALTER TABLE sys_operation_log ADD COLUMN IF NOT EXISTS uuid VARCHAR(36);
UPDATE sys_operation_log SET uuid = uuid_generate_v4()::text WHERE uuid IS NULL;
ALTER TABLE sys_operation_log ALTER COLUMN uuid SET DEFAULT uuid_generate_v4()::text;
ALTER TABLE sys_operation_log ALTER COLUMN uuid SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_sys_operation_log_uuid ON sys_operation_log(uuid);