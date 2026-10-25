ALTER TABLE app_user ADD COLUMN totp_failed_attempts INT NOT NULL DEFAULT 0;
ALTER TABLE app_user ADD COLUMN totp_locked_until TIMESTAMP NULL;
CREATE TABLE audit_chain_anchor(id INT PRIMARY KEY,last_hash VARCHAR(64) NOT NULL);
INSERT INTO audit_chain_anchor(id,last_hash) VALUES(1,'GENESIS');
