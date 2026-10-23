# GreenPulse — Smart Waste Management

GreenPulse is a Java 17+ desktop application built with Swing and FlatLaf. It provides smart-bin management, simulated and real telemetry, collection prioritization, analytics, road-based dispatch, and route reports, backed by MySQL.

## Requirements

- Java 17 or newer
- MySQL 8.0.16+ (or MariaDB 10.2+) running locally, so CHECK constraints are enforced
- Maven 3.8+

## Setup

1. Start MySQL Server.
2. Configure your MySQL credentials. GreenPulse reads `GREENPULSE_MYSQL_USER` and `GREENPULSE_MYSQL_PASSWORD`; the defaults are `root` and an empty password for a local classroom setup. You can also override the JDBC URLs with `GREENPULSE_MYSQL_BASE_URL` and `GREENPULSE_MYSQL_URL`. For anything beyond a classroom demo, use a restricted database account.
3. From this directory, run `mvn clean compile`, then `mvn exec:java` to launch.
4. Run the unit tests with `mvn test`.

On first launch the app creates `greenpulse_db` and all tables from **`src/main/resources/schema.sql`**, which is the single source of truth for the schema. The statements are idempotent, and columns missing from databases created by older versions are added automatically. Older databases keep their original `DOUBLE` column types; a fresh database uses `DECIMAL` columns and CHECK constraints.

## Demo accounts

On a brand-new database the app creates these classroom demo accounts, stored as salted PBKDF2 hashes (no plaintext passwords appear in the SQL):

| Role | Username | Initial password |
|---|---|---|
| Administrator | `admin` | `admin123` |
| Staff | `staff` | `staff123` |
| Driver | `driver` | `driver123` |

Change or remove these accounts before using real operational data. A database created by an older version that still holds plaintext demo passwords upgrades each one to a hash on its next successful login.

## Roles and permissions

Permissions are enforced in the service layer (`BinService`), not only by hiding buttons.

| Action | Admin | Staff | Driver |
|---|---|---|---|
| View monitor, dispatch, record collections | ✔ | ✔ | ✔ |
| Register bins, enter manual sensor readings | ✔ | ✔ | — |
| Remove (archive) bins | ✔ | — | — |

## Demonstration workflow

1. Sign in with a demo account.
2. Register a smart bin with its type, capacity, and a recognizable location. New bins start empty (0% fill). GPS coordinates are optional; place the bin later on the Dispatch map.
3. Start telemetry simulation and watch fill levels change. A toast appears the first time a bin reaches the critical threshold.
4. Open Analytics to see fill trends over time, recycling vs compost, collections and the bins needing attention (see below).
5. Open Dispatch, confirm the depot, review the road-optimized stop order, then pick a driver (fuel or energy estimates come from the vehicle assigned in the database).
6. Work through the **Route stops** list: ✔ collects a stop (the bin is emptied and its fill level resets), ⏭ skips it with a reason. **Auto-collect as truck passes** (the green tick box at the top right of the Dispatch screen) is ticked by default, so pressing Dispatch collects each stop (critical or not) as the animated truck reaches it; untick it to confirm stops yourself. **Collect All Remaining Stops** finishes the rest in one go, and **Save Route & Export…** writes a CSV or printable HTML manifest.
7. On the monitor, select a bin and click **Mark Collected** to record a one-off collection outside a route.
8. Open Analytics to see the effect: collections, time at critical, the fill-level sawtooth, and the forecast table.

## Architecture

| Package | Responsibility |
|---|---|
| `model` | `SmartBin` (abstract) with `RecyclingBin` / `CompostBin`, `BinType`, `Role`, `SensorReading`, `CollectionPolicy` (all thresholds and weights), `SmartBinFactory`, `User`, `Driver`, `CollectionRoute` |
| `dao` | JDBC access: `SmartBinDAO`, `CollectionDAO`, `RouteDAO`, `DriverDAO`, `UserDAO`, `AnalyticsDAO`, plus transactional audit logging |
| `analytics` | Pure calculations for the Analytics page (`AnalyticsCalculator`) and the data records it works on |
| `service` | `BinService` and `RouteService` (role checks and business rules), `StopLocator` (where each stop lies on the route), `RoadRoutingService`, `TelemetrySimulatorEngine`, `ReportExporter`, `PasswordUtil` |
| `event` | Observer pattern: `BinEventBus`, `BinUpdateListener`, `ThresholdAlertListener` (alerts once per critical crossing), `NotificationChannel` |
| `server` | Optional authenticated IoT HTTP endpoint |
| `view` | Swing screens |
| `config` | `DatabaseConnection` and `SchemaManager` |

OOP concepts demonstrated: abstraction and polymorphism (`calculatePriority`, `getSensorReading`, `simulateSensorDrift` are overridden per bin type, so DAOs and the simulator never use `instanceof`), encapsulation with validated setters, a factory, enums, an interface-based Observer, and a custom checked exception.

## Database

See [docs/ER_DIAGRAM.md](docs/ER_DIAGRAM.md). Main tables: `users`, `smart_bins`, `bin_telemetry_logs` (with a `source` column separating sensor, simulator, manual and collection readings), `vehicles`, `drivers`, `routes`, `route_stops`, `collection_events`, and `audit_log`.

- Removing a bin is a **soft delete** (`is_active = FALSE`); its telemetry and collection history stay available for reports. `SmartBinDAO.purge` still permanently deletes a bin and its history, but no screen calls it.
- Sensor readings update only the reading columns, so a stale in-memory copy cannot overwrite a bin's location or GPS.
- Registration, readings, collections, route saves and removals are transactions and write to `audit_log`.

## Optional IoT sensor endpoint

Set `GREENPULSE_IOT_API_KEY` (at least 8 characters) before launching and sign in as an administrator; the app then listens on port 8090 (change with `GREENPULSE_IOT_PORT`).

```
POST /api/telemetry
X-API-Key: <your key>
{"bin_id": "BIN-001", "fill": 72.5}
```

It only updates bins that are already registered (unknown IDs return 404) and rejects fill values outside 0–100.

## Running a route

A route is saved the first time a stop is collected or skipped, with every stop starting as `PENDING`. Each ✔ or ⏭ is one database transaction that updates the stop, the bin (for a collection), the collection history, the audit log and the route status (`PLANNED` → `IN_PROGRESS` → `COMPLETED`). Collected stops turn into a dark-green ✓ on the map and skipped stops into a grey ×. A stop cannot be resolved twice, and a skip always needs a reason (blocked access, bin not full, vehicle full, out of time, bin damaged or missing).

Auto-collect is on by default and is a demo aid: it records a collection when the animated truck reaches the stop, using the nearest point on the drawn road route, and it writes to the database like any other collection. Untick it when you want the driver to confirm each stop with ✔; the route note then tells you when the truck has reached a stop that is still waiting. If you leave the Dispatch screen the route is recalculated when you come back, so a route that was only partly finished stays `IN_PROGRESS` in the database.

## Analytics page

The page combines the current bins with history from `bin_telemetry_logs` and `collection_events`. Filters: period (hour, 24 hours, 7 days, 30 days), bin type, location, and whether simulated readings are included.

- **Summary cards:** bins in view, critical now, average fill, collections in the period (with average fill at pickup), and the average time bins spent at critical level before being collected.
- **Fill level over time:** average fill per bin type and for the fleet, with the critical threshold marked. A bin that has not reported keeps its last known value.
- **Recycling vs compost:** bin counts, critical bins, average fill and priority, plus each type's own sensors (contamination for recycling; temperature and moisture for compost).
- **Collections:** how many bins were emptied in each slice of the period.
- **Needs attention:** the top 5 bins by priority with the readings behind each score.
- **Forecast:** for each bin, the fill rate and the estimated time until it reaches the critical level and 100%, from a straight-line fit of its readings since it was last emptied, marked *steady* or *irregular* depending on how well the line fits. Together with a short list of generated observations (for example "BIN-0017 is next to reach 85%, in about 1.8 h").

The page refreshes itself every 15 seconds while it is open. History is downsampled in SQL (one reading per bin per time bucket), so long periods stay fast. Time at critical is approximate: it is limited by how often readings arrive. Forecasts are simple trend estimates based on fill level only (not temperature or contamination), need at least 4 readings spanning 2 minutes, and are only as good as the period selected.

## Important limitations

- Sensor readings are simulated unless a real device is explicitly connected.
- Email and SMS are not implemented. Alerts appear as an on-screen toast and in the application log. `NotificationChannel` is the extension point for adding email or SMS later.
- The MySQL schema changes and SQL were written against MySQL 8 syntax but have not been run against a live server in this review; test on a throwaway database first.
- Demo credentials appear on the login screen's quick-demo buttons and are for demonstrations only.
- Dispatch uses OpenStreetMap tiles and OSRM's public demo server; they are best-effort services, need internet access, and provide no live traffic or vehicle GPS.

## Real-street dispatch map

- The map loads visible OpenStreetMap tiles on demand and caches tiles locally for at least seven days. The map displays the required `© OpenStreetMap contributors` attribution.
- Latitude and longitude are optional on the Register a Bin screen. If left blank, the bin is saved and marked `MISSING GPS` until it is placed on the Dispatch map or coordinates are entered with **Set GPS Coordinates** (available to administrators and staff). Existing records are retained.
- Dispatch asks OSRM for a road-distance matrix, creates a low-distance stop order, improves it with 2-opt, then asks OSRM for the street-following route geometry, road distance, and typical driving duration.
- Route metrics add an assumed four minutes of service time per stop. Compactor and heavy-duty fuel figures use illustrative assumptions of 0.32 L/km and 0.42 L/km; the electric truck estimate uses 1.2 kWh/km. These are editable classroom assumptions, not measured vehicle performance.
- The depot defaults to a configurable demonstration coordinate in Davao City. Change the latitude/longitude in Dispatch to the real depot before using site-specific coordinates.
- Road access restrictions are respected only to the extent represented in the routing data/profile. Public OSRM does not provide live traffic here, and the truck is animated on the planned route rather than tracked using live GPS.
- The public map/routing endpoints are best-effort and may be rate-limited or unavailable. For production use, configure an approved tile provider and a managed/self-hosted routing service. The OSRM base URL can be overridden with the `greenpulse.osrm.url` Java system property.

To find coordinates, use a trusted map source and copy the exact latitude and longitude for the bin's physical location. Do not use a street address or a guessed coordinate as if it were a GPS measurement.

## Easier Philippines-only map placement

- GreenPulse accepts dispatch coordinates inside an approximate Philippines bounding box (latitude 4.5–21.5, longitude 116.0–127.5). This is a practical project guard, not a precise maritime boundary.
- In **GIS Driver Dispatch**, use **Quick jump to a Philippine city** if needed, then click **Place Depot on Map** and click the depot's location on the map.
- Choose a bin from the list and click **Place Selected Bin on Map**, then click the street/sidewalk location for that bin. GreenPulse saves the coordinates to MySQL and recalculates the route.
- For new bins, latitude/longitude can be left blank; register the bin first and place it using the dispatch map. Existing coordinate fields remain available for manual entry.
- Use the mouse wheel to zoom in/out, drag the map to pan, or double-click to zoom in when map placement is not active. **Fit Depot & Bins in Map** returns to the automatic center. Internet access is needed for uncached OpenStreetMap tiles and OSRM routing.

## Modern, guided user interface

- The login screen now separates branding and sign-in, keeps demo access available, and supports Enter to submit the form.
- The main monitor has clearer page headings, summary cards, a searchable bin table, improved table spacing, and more descriptive navigation labels. Search filters the visible table by bin ID, type, location, status, or coordinate text.
- Register a Bin is a scrollable, three-step form: bin details, sensor settings, and optional Philippines map coordinates. The bin ID is generated automatically, labels change for recycling versus compost bins, input errors are explained in plain language, and Clear Form resets the values.
- Sensor Simulation now explains the selected bin, displays the fill percentage as you adjust it, and clarifies that submitted values are test readings saved to the database.
- Analytics, Profile, and Dispatch have consistent headings, spacing, color use, and clearer helper text. Profile information is read-only to avoid suggesting that changing a text field will update the account.
- The app uses FlatLaf styling with consistent rounded controls and a green accent.

## Changes in this version

- Removed the hard-coded sample bins that appeared when the database was empty; the monitor now shows an empty table and real database errors.
- Removed `instanceof` checks from the DAO and simulator; bin types now describe themselves through polymorphic methods.
- Centralized fill and priority thresholds in `CollectionPolicy`. Analytics previously counted a bin as critical at priority >= 75 while the dashboard used > 75; both now use the same rule.
- Compost priority now converts temperature to a 0–100 score before weighting it.
- Added `collection_events`, `routes`, `route_stops`, `vehicles` and `audit_log` tables, soft delete, indexes, and CHECK constraints.
- Dispatch now loads drivers and vehicle fuel data from the database, can save and export routes, and can mark a route as collected.
- Added role checks in `BinService`, an Observer-based alert system, and an authenticated, registered-bins-only IoT endpoint.
- Removed the unused email/SMS `AlertService` (it posted phone numbers to a public test service) and the JavaMail dependency.
- Report export escapes CSV and HTML output and writes UTF-8.
- Route stops are now tracked one by one (collect or skip with a reason), with optional auto-collect during the truck animation and route status in the database.
- Added a fill-level forecast to Analytics.
- Rebuilt the Analytics page around history (trends, per-type comparison, collections, time at critical) using plain Java2D charts, so the JFreeChart dependency was removed.
- Added JUnit tests for priorities, validation, the factory, alerts, report escaping and password hashing.
