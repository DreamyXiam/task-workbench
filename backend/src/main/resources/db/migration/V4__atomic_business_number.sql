ALTER TABLE company ADD COLUMN company_code VARCHAR(40) NOT NULL DEFAULT 'UNSET';
ALTER TABLE task ADD COLUMN code VARCHAR(100) NULL;
CREATE UNIQUE INDEX uq_task_business_code ON task(code);
CREATE TABLE daily_task_sequence(company_id BIGINT NOT NULL,business_date DATE NOT NULL,next_value BIGINT NOT NULL DEFAULT 0,PRIMARY KEY(company_id,business_date),FOREIGN KEY(company_id) REFERENCES company(id));
