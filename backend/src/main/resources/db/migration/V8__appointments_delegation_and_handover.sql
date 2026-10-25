ALTER TABLE user_scope ADD valid_from TIMESTAMP NULL;
ALTER TABLE user_scope ADD valid_until TIMESTAMP NULL;
ALTER TABLE user_scope ADD employment_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE user_scope ADD appointment_type VARCHAR(20) NOT NULL DEFAULT 'PRIMARY';
ALTER TABLE user_scope ADD version INT NOT NULL DEFAULT 0;
ALTER TABLE user_scope ADD CONSTRAINT ck_appointment_window CHECK (valid_until IS NULL OR valid_from IS NULL OR valid_until>valid_from);
ALTER TABLE user_scope ADD CONSTRAINT ck_employment_status CHECK (employment_status IN ('ACTIVE','ON_LEAVE','LEFT'));
ALTER TABLE user_scope ADD CONSTRAINT ck_appointment_type CHECK (appointment_type IN ('PRIMARY','CONCURRENT'));
CREATE INDEX ix_appointment_effective ON user_scope(user_id,enabled,employment_status,valid_until);
CREATE TABLE scope_delegation (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,delegator_id BIGINT NOT NULL,delegate_user_id BIGINT NOT NULL,
 source_scope_id BIGINT NOT NULL,company_id BIGINT NOT NULL,dept_id BIGINT NOT NULL,
 permissions_json VARCHAR(1000) NOT NULL,valid_from TIMESTAMP NOT NULL,valid_until TIMESTAMP NOT NULL,
 status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',version INT NOT NULL DEFAULT 0,
 reason VARCHAR(1000) NOT NULL,revocation_reason VARCHAR(1000) NULL,revoked_by BIGINT NULL,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,revoked_at TIMESTAMP NULL,
 FOREIGN KEY(delegator_id) REFERENCES app_user(id),FOREIGN KEY(delegate_user_id) REFERENCES app_user(id),
 FOREIGN KEY(source_scope_id) REFERENCES user_scope(id),FOREIGN KEY(company_id) REFERENCES company(id),FOREIGN KEY(dept_id) REFERENCES department(id),
 CONSTRAINT ck_delegation_window CHECK(valid_until>valid_from),CONSTRAINT ck_delegation_users CHECK(delegator_id<>delegate_user_id),
 CONSTRAINT ck_delegation_status CHECK(status IN ('ACTIVE','REVOKED'))
);
CREATE INDEX ix_delegation_delegate ON scope_delegation(delegate_user_id,status,valid_until);
CREATE TABLE task_handover (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,task_id BIGINT NOT NULL,sub_task_id BIGINT NOT NULL,company_id BIGINT NOT NULL,dept_id BIGINT NOT NULL,
 previous_owner_id BIGINT NULL,new_owner_id BIGINT NOT NULL,actor_id BIGINT NOT NULL,delegation_id BIGINT NULL,
 reason VARCHAR(1000) NOT NULL,previous_owner_snapshot LONGTEXT NOT NULL,new_owner_snapshot LONGTEXT NOT NULL,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(task_id) REFERENCES task(id),FOREIGN KEY(sub_task_id) REFERENCES sub_task(id),FOREIGN KEY(company_id) REFERENCES company(id),
 FOREIGN KEY(dept_id) REFERENCES department(id),FOREIGN KEY(previous_owner_id) REFERENCES app_user(id),FOREIGN KEY(new_owner_id) REFERENCES app_user(id),
 FOREIGN KEY(actor_id) REFERENCES app_user(id),FOREIGN KEY(delegation_id) REFERENCES scope_delegation(id)
);
CREATE INDEX ix_handover_scope ON task_handover(company_id,dept_id,sub_task_id);
ALTER TABLE audit_event ADD delegation_id BIGINT NULL;
