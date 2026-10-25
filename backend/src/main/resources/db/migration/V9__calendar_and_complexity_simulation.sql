CREATE TABLE company_calendar_version (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 company_id BIGINT NOT NULL,
 version INT NOT NULL,
 name VARCHAR(100) NOT NULL,
 reason VARCHAR(1000) NOT NULL,
 payload LONGTEXT NOT NULL,
 created_by BIGINT NOT NULL,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT fk_calendar_company FOREIGN KEY (company_id) REFERENCES company(id),
 CONSTRAINT uq_calendar_version UNIQUE (company_id,version)
);
CREATE TABLE kpi_simulation_template (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 company_id BIGINT NOT NULL,
 version INT NOT NULL,
 name VARCHAR(100) NOT NULL,
 reason VARCHAR(1000) NOT NULL,
 payload LONGTEXT NOT NULL,
 created_by BIGINT NOT NULL,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT fk_sim_template_company FOREIGN KEY (company_id) REFERENCES company(id),
 CONSTRAINT uq_sim_template_version UNIQUE (company_id,version)
);
CREATE TABLE task_classification_version (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 task_id BIGINT NOT NULL,
 company_id BIGINT NOT NULL,
 version INT NOT NULL,
 complexity VARCHAR(16) NOT NULL,
 category VARCHAR(50) NOT NULL,
 reason VARCHAR(1000) NOT NULL,
 created_by BIGINT NOT NULL,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT fk_classification_task FOREIGN KEY (task_id) REFERENCES task(id),
 CONSTRAINT fk_classification_company FOREIGN KEY (company_id) REFERENCES company(id),
 CONSTRAINT uq_classification_version UNIQUE (task_id,version),
 CONSTRAINT ck_task_complexity CHECK (complexity IN ('SIMPLE','NORMAL','COMPLEX'))
);
CREATE TABLE calendar_simulation (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 company_id BIGINT NOT NULL,
 dept_id BIGINT NULL,
 period VARCHAR(7) NOT NULL,
 reason VARCHAR(1000) NOT NULL,
 input_hash VARCHAR(64) NOT NULL,
 input_snapshot LONGTEXT NOT NULL,
 result_json LONGTEXT NOT NULL,
 created_by BIGINT NOT NULL,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT fk_sim_company FOREIGN KEY (company_id) REFERENCES company(id),
 CONSTRAINT fk_sim_dept FOREIGN KEY (dept_id) REFERENCES department(id)
);
CREATE INDEX ix_calendar_sim_company_period ON calendar_simulation(company_id,period);
CREATE INDEX ix_calendar_sim_dept ON calendar_simulation(company_id,dept_id,id);
