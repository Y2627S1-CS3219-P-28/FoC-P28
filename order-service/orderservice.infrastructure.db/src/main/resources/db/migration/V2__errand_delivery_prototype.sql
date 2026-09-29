CREATE TABLE errands (
    id varchar(36) PRIMARY KEY, requester_id varchar(128) NOT NULL,
    description varchar(100) NOT NULL, pickup_supplier_id varchar(128) NOT NULL,
    delivery_supplier_id varchar(128) NOT NULL, credit_amount bigint NOT NULL CHECK (credit_amount > 0),
    delivery_duration_minutes integer NOT NULL CHECK (delivery_duration_minutes BETWEEN 15 AND 1440),
    expires_at timestamptz NOT NULL, created_at timestamptz NOT NULL,
    status varchar(20) NOT NULL CHECK (status IN ('OPEN','ACCEPTED')), order_id varchar(36) UNIQUE,
    request_fingerprint varchar(64) NOT NULL, version bigint NOT NULL DEFAULT 0,
    CHECK (pickup_supplier_id <> delivery_supplier_id),
    CHECK ((status = 'OPEN' AND order_id IS NULL) OR (status = 'ACCEPTED' AND order_id IS NOT NULL))
);
CREATE INDEX errands_available ON errands(status, expires_at, created_at);
CREATE TABLE delivery_orders (
    id varchar(36) PRIMARY KEY, errand_id varchar(36) NOT NULL REFERENCES errands(id),
    courier_id varchar(128) NOT NULL, delivery_duration_minutes integer NOT NULL,
    status varchar(20) NOT NULL CHECK (status IN ('ACCEPTED','IN_PROGRESS','PICKED_UP','DELIVERED')),
    accepted_at timestamptz NOT NULL, started_at timestamptz, picked_up_at timestamptz, delivered_at timestamptz,
    version bigint NOT NULL DEFAULT 0
);
ALTER TABLE errands ADD CONSTRAINT errand_current_order_fk FOREIGN KEY(order_id)
    REFERENCES delivery_orders(id) DEFERRABLE INITIALLY DEFERRED;
CREATE TABLE order_checkpoints (
    id varchar(36) PRIMARY KEY, order_id varchar(36) NOT NULL REFERENCES delivery_orders(id),
    courier_id varchar(128) NOT NULL, status varchar(20) NOT NULL,
    occurred_at timestamptz NOT NULL, supplier_id varchar(128),
    UNIQUE(order_id, status)
);
CREATE TABLE order_commands (id varchar(36) PRIMARY KEY, fingerprint varchar(64) NOT NULL);
