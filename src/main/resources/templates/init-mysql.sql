-- 由于uuid字段是每个表都新增的，如果启动报错，先执行下面的SQL，为每张表增加UUID字段----
-- 1. attendance_group
ALTER TABLE attendance_group ADD COLUMN uuid VARCHAR(36);
ALTER TABLE attendance_group MODIFY COLUMN uuid VARCHAR(36) NOT NULL DEFAULT (UUID());
CREATE UNIQUE INDEX idx_attendance_group_uuid ON attendance_group(uuid);

-- 2. attendance_logs
ALTER TABLE attendance_logs ADD COLUMN uuid VARCHAR(36);
ALTER TABLE attendance_logs MODIFY COLUMN uuid VARCHAR(36) NOT NULL DEFAULT (UUID());
CREATE UNIQUE INDEX idx_attendance_logs_uuid ON attendance_logs(uuid);

-- 3. device_commands
ALTER TABLE device_commands ADD COLUMN uuid VARCHAR(36);
ALTER TABLE device_commands MODIFY COLUMN uuid VARCHAR(36) NOT NULL DEFAULT (UUID());
CREATE UNIQUE INDEX idx_device_commands_uuid ON device_commands(uuid);

-- 4. card_template
ALTER TABLE card_template ADD COLUMN uuid VARCHAR(36);
ALTER TABLE card_template MODIFY COLUMN uuid VARCHAR(36) NOT NULL DEFAULT (UUID());
CREATE UNIQUE INDEX idx_card_template_uuid ON card_template(uuid);

-- 5. devices
ALTER TABLE devices ADD COLUMN uuid VARCHAR(36);
ALTER TABLE devices MODIFY COLUMN uuid VARCHAR(36) NOT NULL DEFAULT (UUID());
CREATE UNIQUE INDEX idx_devices_uuid ON devices(uuid);

-- 6. employee
ALTER TABLE employee ADD COLUMN uuid VARCHAR(36);
ALTER TABLE employee MODIFY COLUMN uuid VARCHAR(36) NOT NULL DEFAULT (UUID());
CREATE UNIQUE INDEX idx_employee_uuid ON employee(uuid);

-- 7. employee_sync_queue
ALTER TABLE employee_sync_queue ADD COLUMN uuid VARCHAR(36);
ALTER TABLE employee_sync_queue MODIFY COLUMN uuid VARCHAR(36) NOT NULL DEFAULT (UUID());
CREATE UNIQUE INDEX idx_employee_sync_queue_uuid ON employee_sync_queue(uuid);

-- 8. group_device_mapping
ALTER TABLE group_device_mapping ADD COLUMN uuid VARCHAR(36);
ALTER TABLE group_device_mapping MODIFY COLUMN uuid VARCHAR(36) NOT NULL DEFAULT (UUID());
CREATE UNIQUE INDEX idx_group_device_mapping_uuid ON group_device_mapping(uuid);

-- 9. oa_employee
ALTER TABLE oa_employee ADD COLUMN uuid VARCHAR(36);
ALTER TABLE oa_employee MODIFY COLUMN uuid VARCHAR(36) NOT NULL DEFAULT (UUID());
CREATE UNIQUE INDEX idx_oa_employee_uuid ON oa_employee(uuid);

-- 10. operation_log
ALTER TABLE operation_log ADD COLUMN uuid VARCHAR(36);
ALTER TABLE operation_log MODIFY COLUMN uuid VARCHAR(36) NOT NULL DEFAULT (UUID());
CREATE UNIQUE INDEX idx_operation_log_uuid ON operation_log(uuid);

-- 11. sys_app_version
ALTER TABLE sys_app_version ADD COLUMN uuid VARCHAR(36);
ALTER TABLE sys_app_version MODIFY COLUMN uuid VARCHAR(36) NOT NULL DEFAULT (UUID());
CREATE UNIQUE INDEX idx_sys_app_version_uuid ON sys_app_version(uuid);

-- 12. sys_config
ALTER TABLE sys_config ADD COLUMN uuid VARCHAR(36);
ALTER TABLE sys_config MODIFY COLUMN uuid VARCHAR(36) NOT NULL DEFAULT (UUID());
CREATE UNIQUE INDEX idx_sys_config_uuid ON sys_config(uuid);

--- 为每张表原本数据列uuid为空的生成uuid
UPDATE attendance_group SET uuid = UUID() WHERE uuid IS NULL;
UPDATE attendance_logs SET uuid = UUID() WHERE uuid IS NULL;
UPDATE device_commands SET uuid = UUID() WHERE uuid IS NULL;
UPDATE card_template SET uuid = UUID() WHERE uuid IS NULL;
UPDATE devices SET uuid = UUID() WHERE uuid IS NULL;
UPDATE employee SET uuid = UUID() WHERE uuid IS NULL;
UPDATE employee_sync_queue SET uuid = UUID() WHERE uuid IS NULL;
UPDATE group_device_mapping SET uuid = UUID() WHERE uuid IS NULL;
UPDATE oa_employee SET uuid = UUID() WHERE uuid IS NULL;
UPDATE operation_log SET uuid = UUID() WHERE uuid IS NULL;
UPDATE sys_app_version SET uuid = UUID() WHERE uuid IS NULL;
UPDATE sys_config SET uuid = UUID() WHERE uuid IS NULL;

-- 为每个表增加触发器，插入时自动添加uuid

DELIMITER $$
-- 1. attendance_group
DROP TRIGGER IF EXISTS trg_attendance_group_uuid$$
CREATE TRIGGER trg_attendance_group_uuid BEFORE INSERT ON attendance_group
    FOR EACH ROW
BEGIN
    IF NEW.uuid IS NULL OR NEW.uuid = '' THEN
        SET NEW.uuid = UUID();
END IF;
END$$

-- 2. attendance_logs
DROP TRIGGER IF EXISTS trg_attendance_logs_uuid$$
CREATE TRIGGER trg_attendance_logs_uuid BEFORE INSERT ON attendance_logs
    FOR EACH ROW
BEGIN
    IF NEW.uuid IS NULL OR NEW.uuid = '' THEN
        SET NEW.uuid = UUID();
END IF;
END$$

-- 3. device_commands
DROP TRIGGER IF EXISTS trg_device_commands_uuid$$
CREATE TRIGGER trg_device_commands_uuid BEFORE INSERT ON device_commands
    FOR EACH ROW
BEGIN
    IF NEW.uuid IS NULL OR NEW.uuid = '' THEN
        SET NEW.uuid = UUID();
END IF;
END$$

-- 4. devices
DROP TRIGGER IF EXISTS trg_devices_uuid$$
CREATE TRIGGER trg_devices_uuid BEFORE INSERT ON devices
    FOR EACH ROW
BEGIN
    IF NEW.uuid IS NULL OR NEW.uuid = '' THEN
        SET NEW.uuid = UUID();
END IF;
END$$

-- 5. employee
DROP TRIGGER IF EXISTS trg_employee_uuid$$
CREATE TRIGGER trg_employee_uuid BEFORE INSERT ON employee
    FOR EACH ROW
BEGIN
    IF NEW.uuid IS NULL OR NEW.uuid = '' THEN
        SET NEW.uuid = UUID();
END IF;
END$$

-- 6. employee_sync_queue
DROP TRIGGER IF EXISTS trg_employee_sync_queue_uuid$$
CREATE TRIGGER trg_employee_sync_queue_uuid BEFORE INSERT ON employee_sync_queue
    FOR EACH ROW
BEGIN
    IF NEW.uuid IS NULL OR NEW.uuid = '' THEN
        SET NEW.uuid = UUID();
END IF;
END$$

-- 7. group_device_mapping
DROP TRIGGER IF EXISTS trg_group_device_mapping_uuid$$
CREATE TRIGGER trg_group_device_mapping_uuid BEFORE INSERT ON group_device_mapping
    FOR EACH ROW
BEGIN
    IF NEW.uuid IS NULL OR NEW.uuid = '' THEN
        SET NEW.uuid = UUID();
END IF;
END$$

-- 8. sys_app_version
DROP TRIGGER IF EXISTS trg_sys_app_version_uuid$$
CREATE TRIGGER trg_sys_app_version_uuid BEFORE INSERT ON sys_app_version
    FOR EACH ROW
BEGIN
    IF NEW.uuid IS NULL OR NEW.uuid = '' THEN
        SET NEW.uuid = UUID();
END IF;
END$$

-- 9. sys_config
DROP TRIGGER IF EXISTS trg_sys_config_uuid$$
CREATE TRIGGER trg_sys_config_uuid BEFORE INSERT ON sys_config
    FOR EACH ROW
BEGIN
    IF NEW.uuid IS NULL OR NEW.uuid = '' THEN
        SET NEW.uuid = UUID();
END IF;
END$$

-- card_template
DROP TRIGGER IF EXISTS trg_card_template_uuid$$
CREATE TRIGGER trg_card_template_uuid BEFORE INSERT ON card_template
    FOR EACH ROW
BEGIN
    IF NEW.uuid IS NULL OR NEW.uuid = '' THEN
        SET NEW.uuid = UUID();
END IF;
END$$

-- oa_employee
DROP TRIGGER IF EXISTS trg_oa_employee_uuid$$
CREATE TRIGGER trg_oa_employee_uuid BEFORE INSERT ON oa_employee
    FOR EACH ROW
BEGIN
    IF NEW.uuid IS NULL OR NEW.uuid = '' THEN
        SET NEW.uuid = UUID();
END IF;
END$$

DELIMITER ;

-- sys_operation_log
DROP TRIGGER IF EXISTS trg_sys_operation_log_uuid$$
CREATE TRIGGER trg_sys_operation_log_uuid BEFORE INSERT ON sys_operation_log
    FOR EACH ROW
BEGIN
    IF NEW.uuid IS NULL OR NEW.uuid = '' THEN
        SET NEW.uuid = UUID();
END IF;
END$$

DELIMITER ;