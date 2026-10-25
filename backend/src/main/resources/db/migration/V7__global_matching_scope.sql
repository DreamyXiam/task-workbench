ALTER TABLE function_tag MODIFY company_id BIGINT NULL;
ALTER TABLE matching_rule MODIFY company_id BIGINT NULL;
ALTER TABLE function_tag ADD CONSTRAINT fk_global_tag_dept FOREIGN KEY(dept_id) REFERENCES department(id);
