# GreenPulse entity-relationship diagram

```mermaid
erDiagram
    users ||--o{ drivers : "may be"
    vehicles ||--o{ drivers : "assigned to"
    drivers ||--o{ routes : "drives"
    users ||--o{ routes : "plans"
    routes ||--|{ route_stops : "has ordered"
    smart_bins ||--o{ route_stops : "visited at"
    smart_bins ||--o{ bin_telemetry_logs : "reports"
    smart_bins ||--o{ collection_events : "emptied in"
    users ||--o{ collection_events : "records"
    routes ||--o{ collection_events : "completes"

    users { int user_id PK
            string username UK
            string password "PBKDF2 hash"
            enum role "ADMIN STAFF DRIVER"
            timestamp last_login }
    smart_bins { string bin_id PK
                 enum bin_type "RecyclingBin CompostBin"
                 decimal fill_percentage "CHECK 0-100"
                 decimal latitude
                 decimal longitude
                 boolean is_active "soft delete" }
    bin_telemetry_logs { bigint log_id PK
                         string bin_id FK
                         decimal fill_percentage
                         enum source "SENSOR SIMULATOR MANUAL COLLECTION"
                         timestamp logged_at }
    vehicles { string vehicle_id PK
               enum fuel_type
               decimal consumption_per_km }
    drivers { string driver_id PK
              int user_id FK
              string vehicle_id FK }
    routes { string route_id PK
             string driver_id FK
             int total_distance_m
             enum status "PLANNED IN_PROGRESS COMPLETED CANCELLED" }
    route_stops { string route_id PK
                  int stop_sequence PK
                  string bin_id FK
                  enum status "PENDING COLLECTED SKIPPED"
                  string skip_reason }
    collection_events { bigint event_id PK
                        string bin_id FK
                        int collected_by FK
                        string route_id FK
                        decimal fill_before }
    audit_log { bigint audit_id PK
                int user_id "no FK, history survives deletes"
                string action
                string entity_id }
```

## Design notes

- `smart_bins` uses single-table inheritance: recycling bins fill `contamination_percentage`, compost bins fill `moisture_level` and `temp_celsius`, and the other columns stay `NULL`. The model layer hides this behind `SensorReading`.
- `bin_telemetry_logs` is append-only and indexed on `(bin_id, logged_at)` for trend queries.
- `audit_log` deliberately has no foreign keys so the history survives deleted users and bins.
- `drivers.assigned_vehicle` is a legacy free-text column kept for databases created by older versions; `vehicle_id` is the normalized replacement.
