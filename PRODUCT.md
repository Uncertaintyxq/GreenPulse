# Product

<!-- impeccable:product-schema 1 -->

## Platform

desktop (Java 17 Swing with FlatLaf; not a web, iOS or Android surface)

## Users
Two audiences, confirmed by the owner as "both":
- **Now:** a school project, demonstrated on a laptop and often on a projector to teachers and panelists.
- **The bar it must meet:** a working tool for city or barangay waste-office staff at a desk through the workday. Roles are Administrator, Staff and Driver; drivers land on Dispatch and see fewer pages.

## Product Purpose
GreenPulse monitors smart waste bins, records simulated and real sensor readings, ranks bins by collection priority, shows fleet analytics and forecasts, and plans road-based collection routes with a driver dispatch run, backed by MySQL. Success is a dispatcher seeing which bins need collecting, sending a truck along a sensible road route, and recording each stop.

## Positioning
Priority comes from per-type sensors (contamination for recycling bins, temperature and moisture for compost bins) and feeds a road-optimized stop order on a real street map (OpenStreetMap tiles, OSRM routing), with a time-to-critical forecast per bin. It is set in the Philippines: the default depot is Davao City and coordinates are validated to the Philippine region.

## Operating Context
Desktop windows around 1320×780, used at a desk and shown on projectors, where low-contrast text washes out. Workflows: monitor the fleet, simulate or receive telemetry, register bins, analyse trends, dispatch and work through route stops, export route manifests (CSV or printable HTML).

## Capabilities and Constraints
- Swing + FlatLaf only. Charts are drawn with Java2D, with no charting library.
- Role permissions are enforced in the service layer, not only by hiding controls.
- Optional IoT HTTP endpoint for administrators.
- **The Dispatch map and how it works must not change** (owner's explicit constraint): tiles, routing, pins, truck animation, placing bins and the depot, and the map's interactions.

## Brand Commitments
- Product name: GreenPulse.
- The Dispatch map's rendering and behavior stay as they are (see above).

## Evidence on Hand
- Classroom demo accounts (admin, staff, driver) are created on a fresh database.
- Real bin data comes from the user's MySQL database. There are no testimonials, customers or deployment claims, and none may be invented.

## Product Principles
1. The dispatcher's next action is always obvious: what is critical, what to collect, where to drive.
2. Readable at a distance: every figure and label holds up on a projector.
3. Honest data: simulated readings are labeled as simulated, and forecasts state their confidence.
4. A real tool, not a demo skin: it must hold up to all-day desk use as well as a ten-minute presentation.

## Accessibility & Inclusion
Text contrast must meet WCAG AA at minimum (4.5:1 for body text) because of projector use.
