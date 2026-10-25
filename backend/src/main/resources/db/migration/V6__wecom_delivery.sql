-- Durable independent station / WeCom delivery state. External keys never enter this table.
CREATE TABLE notification_delivery (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 outbox_id BIGINT NOT NULL,
 channel_type VARCHAR(12) NOT NULL,
 channel_id BIGINT NOT NULL,
 status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
 attempts INT NOT NULL DEFAULT 0,
 next_attempt TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 lease_until TIMESTAMP NULL,
 lease_token VARCHAR(36) NULL,
 last_error VARCHAR(80) NULL,
 completed_at TIMESTAMP NULL,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE(outbox_id,channel_type,channel_id),
 FOREIGN KEY(outbox_id) REFERENCES notification_outbox(id),
 CHECK(channel_type IN ('STATION','WECOM')),
 CHECK(status IN ('PENDING','IN_FLIGHT','SENT','SKIPPED','DEAD')),
 CHECK(attempts>=0),
 CHECK((channel_type='STATION' AND channel_id=0) OR (channel_type='WECOM' AND channel_id>0))
);
CREATE INDEX ix_delivery_due ON notification_delivery(status,next_attempt,lease_until,id);
