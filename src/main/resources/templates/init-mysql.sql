-- 1. attendance_group
ALTER TABLE attendance_group ADD COLUMN uuid VARCHAR(36);
UPDATE attendance_group SET uuid = UUID() WHERE uuid IS NULL;
ALTER TABLE attendance_group MODIFY COLUMN uuid VARCHAR(36) NOT NULL DEFAULT (UUID());
CREATE UNIQUE INDEX idx_attendance_group_uuid ON attendance_group(uuid);

-- 2. attendance_logs
ALTER TABLE attendance_logs ADD COLUMN uuid VARCHAR(36);
UPDATE attendance_logs SET uuid = UUID() WHERE uuid IS NULL;
ALTER TABLE attendance_logs MODIFY COLUMN uuid VARCHAR(36) NOT NULL DEFAULT (UUID());
CREATE UNIQUE INDEX idx_attendance_logs_uuid ON attendance_logs(uuid);

-- 3. device_commands
ALTER TABLE device_commands ADD COLUMN uuid VARCHAR(36);
UPDATE device_commands SET uuid = UUID() WHERE uuid IS NULL;
ALTER TABLE device_commands MODIFY COLUMN uuid VARCHAR(36) NOT NULL DEFAULT (UUID());
CREATE UNIQUE INDEX idx_device_commands_uuid ON device_commands(uuid);

-- 4. card_template
ALTER TABLE card_template ADD COLUMN uuid VARCHAR(36);
UPDATE card_template SET uuid = UUID() WHERE uuid IS NULL;
ALTER TABLE card_template MODIFY COLUMN uuid VARCHAR(36) NOT NULL DEFAULT (UUID());
CREATE UNIQUE INDEX idx_card_template_uuid ON card_template(uuid);

-- 5. devices
ALTER TABLE devices ADD COLUMN uuid VARCHAR(36);
UPDATE devices SET uuid = UUID() WHERE uuid IS NULL;
ALTER TABLE devices MODIFY COLUMN uuid VARCHAR(36) NOT NULL DEFAULT (UUID());
CREATE UNIQUE INDEX idx_devices_uuid ON devices(uuid);

-- 6. employee
ALTER TABLE employee ADD COLUMN uuid VARCHAR(36);
UPDATE employee SET uuid = UUID() WHERE uuid IS NULL;
ALTER TABLE employee MODIFY COLUMN uuid VARCHAR(36) NOT NULL DEFAULT (UUID());
CREATE UNIQUE INDEX idx_employee_uuid ON employee(uuid);

-- 7. employee_sync_queue
ALTER TABLE employee_sync_queue ADD COLUMN uuid VARCHAR(36);
UPDATE employee_sync_queue SET uuid = UUID() WHERE uuid IS NULL;
ALTER TABLE employee_sync_queue MODIFY COLUMN uuid VARCHAR(36) NOT NULL DEFAULT (UUID());
CREATE UNIQUE INDEX idx_employee_sync_queue_uuid ON employee_sync_queue(uuid);

-- 8. group_device_mapping
ALTER TABLE group_device_mapping ADD COLUMN uuid VARCHAR(36);
UPDATE group_device_mapping SET uuid = UUID() WHERE uuid IS NULL;
ALTER TABLE group_device_mapping MODIFY COLUMN uuid VARCHAR(36) NOT NULL DEFAULT (UUID());
CREATE UNIQUE INDEX idx_group_device_mapping_uuid ON group_device_mapping(uuid);

-- 9. oa_employee
ALTER TABLE oa_employee ADD COLUMN uuid VARCHAR(36);
UPDATE oa_employee SET uuid = UUID() WHERE uuid IS NULL;
ALTER TABLE oa_employee MODIFY COLUMN uuid VARCHAR(36) NOT NULL DEFAULT (UUID());
CREATE UNIQUE INDEX idx_oa_employee_uuid ON oa_employee(uuid);

-- 10. operation_log
ALTER TABLE operation_log ADD COLUMN uuid VARCHAR(36);
UPDATE operation_log SET uuid = UUID() WHERE uuid IS NULL;
ALTER TABLE operation_log MODIFY COLUMN uuid VARCHAR(36) NOT NULL DEFAULT (UUID());
CREATE UNIQUE INDEX idx_operation_log_uuid ON operation_log(uuid);

-- 11. sys_app_version
ALTER TABLE sys_app_version ADD COLUMN uuid VARCHAR(36);
UPDATE sys_app_version SET uuid = UUID() WHERE uuid IS NULL;
ALTER TABLE sys_app_version MODIFY COLUMN uuid VARCHAR(36) NOT NULL DEFAULT (UUID());
CREATE UNIQUE INDEX idx_sys_app_version_uuid ON sys_app_version(uuid);

-- 12. sys_config
ALTER TABLE sys_config ADD COLUMN uuid VARCHAR(36);
UPDATE sys_config SET uuid = UUID() WHERE uuid IS NULL;
ALTER TABLE sys_config MODIFY COLUMN uuid VARCHAR(36) NOT NULL DEFAULT (UUID());
CREATE UNIQUE INDEX idx_sys_config_uuid ON sys_config(uuid);