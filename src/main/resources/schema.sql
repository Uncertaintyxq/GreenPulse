-- GreenPulse database schema (single source of truth).
-- Loaded automatically by DatabaseConnection on startup; every statement is idempotent.
-- Needs MySQL 8.0.16+ (or MariaDB 10.2+) for CHECK constraints to be enforced.
-- Demo users are NOT created here: the application seeds them with hashed passwords.

CREATE TABLE IF NOT EXISTS users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role ENUM('ADMIN', 'STAFF', 'DRIVER') NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP NULL DEFAULT NULL
);

CREATE TABLE IF NOT EXISTS smart_bins (
    bin_id VARCHAR(50) PRIMARY KEY,
    bin_type ENUM('RecyclingBin', 'CompostBin') NOT NULL,
    capacity_litres DECIMAL(8,2) NOT NULL DEFAULT 120.00,
    fill_percentage DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    location VARCHAR(100) NOT NULL,
    latitude DECIMAL(9,6) NULL,
    longitude DECIMAL(9,6) NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    contamination_percentage DECIMAL(5,2) NULL,
    optical_sensor_id VARCHAR(50) NULL,
    moisture_level DECIMAL(5,2) NULL,
    temp_celsius DECIMAL(5,2) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_bin_capacity CHECK (capacity_litres > 0),
    CONSTRAINT chk_bin_fill CHECK (fill_percentage BETWEEN 0 AND 100),
    CONSTRAINT chk_bin_contamination CHECK (contamination_percentage IS NULL OR contamination_percentage BETWEEN 0 AND 100),
    CONSTRAINT chk_bin_moisture CHECK (moisture_level IS NULL OR moisture_level BETWEEN 0 AND 100),
    CONSTRAINT chk_bin_temp CHECK (temp_celsius IS NULL OR temp_celsius BETWEEN -50 AND 100),
    CONSTRAINT chk_bin_coords CHECK ((latitude IS NULL AND longitude IS NULL)
        OR (latitude BETWEEN 4.5 AND 21.5 AND longitude BETWEEN 116.0 AND 127.5))
);

CREATE TABLE IF NOT EXISTS bin_telemetry_logs (
    log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    bin_id VARCHAR(50) NOT NULL,
    fill_percentage DECIMAL(5,2) NOT NULL,
    temperature DECIMAL(5,2) NULL,
    moisture DECIMAL(5,2) NULL,
    contamination DECIMAL(5,2) NULL,
    source ENUM('SENSOR', 'SIMULATOR', 'MANUAL', 'COLLECTION') NOT NULL DEFAULT 'SENSOR',
    logged_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_telemetry_bin FOREIGN KEY (bin_id) REFERENCES smart_bins(bin_id) ON DELETE CASCADE,
    INDEX idx_telemetry_bin_time (bin_id, logged_at)
);

CREATE TABLE IF NOT EXISTS vehicles (
    vehicle_id VARCHAR(30) PRIMARY KEY,
    vehicle_name VARCHAR(80) NOT NULL,
    fuel_type ENUM('DIESEL', 'ELECTRIC') NOT NULL,
    consumption_per_km DECIMAL(5,2) NOT NULL COMMENT 'litres per km for DIESEL, kWh per km for ELECTRIC',
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS drivers (
    driver_id VARCHAR(50) PRIMARY KEY,
    driver_name VARCHAR(100) NOT NULL,
    assigned_vehicle VARCHAR(50) NULL COMMENT 'legacy free-text column, superseded by vehicle_id',
    phone VARCHAR(30) NULL,
    user_id INT NULL,
    vehicle_id VARCHAR(30) NULL,
    CONSTRAINT fk_driver_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE SET NULL,
    CONSTRAINT fk_driver_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(vehicle_id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS routes (
    route_id VARCHAR(40) PRIMARY KEY,
    driver_id VARCHAR(50) NULL,
    created_by INT NULL,
    depot_latitude DECIMAL(9,6) NOT NULL,
    depot_longitude DECIMAL(9,6) NOT NULL,
    total_distance_m INT NOT NULL,
    est_duration_s INT NOT NULL,
    status ENUM('PLANNED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED') NOT NULL DEFAULT 'PLANNED',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_route_driver FOREIGN KEY (driver_id) REFERENCES drivers(driver_id) ON DELETE SET NULL,
    CONSTRAINT fk_route_user FOREIGN KEY (created_by) REFERENCES users(user_id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS route_stops (
    route_id VARCHAR(40) NOT NULL,
    stop_sequence INT NOT NULL,
    bin_id VARCHAR(50) NOT NULL,
    fill_at_planning DECIMAL(5,2) NOT NULL,
    priority_at_planning INT NOT NULL,
    status ENUM('PENDING', 'COLLECTED', 'SKIPPED') NOT NULL DEFAULT 'PENDING',
    skip_reason VARCHAR(120) NULL,
    resolved_at TIMESTAMP NULL DEFAULT NULL,
    PRIMARY KEY (route_id, stop_sequence),
    CONSTRAINT fk_stop_route FOREIGN KEY (route_id) REFERENCES routes(route_id) ON DELETE CASCADE,
    CONSTRAINT fk_stop_bin FOREIGN KEY (bin_id) REFERENCES smart_bins(bin_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS collection_events (
    event_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    bin_id VARCHAR(50) NOT NULL,
    collected_by INT NULL,
    route_id VARCHAR(40) NULL,
    fill_before DECIMAL(5,2) NOT NULL,
    collected_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_event_bin FOREIGN KEY (bin_id) REFERENCES smart_bins(bin_id) ON DELETE CASCADE,
    CONSTRAINT fk_event_user FOREIGN KEY (collected_by) REFERENCES users(user_id) ON DELETE SET NULL,
    CONSTRAINT fk_event_route FOREIGN KEY (route_id) REFERENCES routes(route_id) ON DELETE SET NULL,
    INDEX idx_event_bin_time (bin_id, collected_at)
);

-- No foreign keys on purpose: audit history must survive deleted users and bins.
CREATE TABLE IF NOT EXISTS audit_log (
    audit_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NULL,
    action VARCHAR(40) NOT NULL,
    entity_type VARCHAR(30) NOT NULL,
    entity_id VARCHAR(60) NOT NULL,
    details VARCHAR(255) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_audit_time (created_at)
)
