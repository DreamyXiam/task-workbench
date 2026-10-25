ALTER TABLE user_scope ADD CONSTRAINT ck_group_scope CHECK (role <> 'GROUP_LEADER' OR (company_id IS NULL AND dept_id IS NULL));
ALTER TABLE task ADD original_deadline TIMESTAMP NULL;
UPDATE task SET original_deadline=deadline WHERE original_deadline IS NULL;
ALTER TABLE sub_task ADD original_deadline TIMESTAMP NULL;
ALTER TABLE sub_task ADD ever_overdue BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE sub_task ADD reject_count INT NOT NULL DEFAULT 0;
UPDATE sub_task SET original_deadline=deadline WHERE original_deadline IS NULL;
CREATE TABLE task_deadline_history (
 id BIGINT PRIMARY KEY,task_id BIGINT NOT NULL,sub_task_id BIGINT NOT NULL,
 previous_deadline TIMESTAMP NOT NULL,current_deadline TIMESTAMP NOT NULL,
 reason VARCHAR(1000) NOT NULL,actor_id BIGINT NOT NULL,
 FOREIGN KEY(id) REFERENCES business_record(id),
 FOREIGN KEY(task_id) REFERENCES task(id),
 FOREIGN KEY(sub_task_id) REFERENCES sub_task(id),
 FOREIGN KEY(actor_id) REFERENCES app_user(id)
);
CREATE INDEX ix_deadline_history_task ON task_deadline_history(task_id,sub_task_id);
