---
version: 1
slug: "m-greenpulse-view-maindashboardframe-java-028dd1dc"
primary_target: "src/main/java/com/greenpulse/view/MainDashboardFrame.java"
related_targets: ["src/main/java/com/greenpulse/view/LoginFrame.java","src/main/java/com/greenpulse/view/AnalyticsPanel.java","src/main/java/com/greenpulse/view/SimulatePanel.java","src/main/java/com/greenpulse/view/AddBinPanel.java","src/main/java/com/greenpulse/view/UserProfilePanel.java","src/main/java/com/greenpulse/view/RouteMapPanel.java"]
---

# GreenPulse console (all Swing screens)

Scope: the desktop console shell and its screens (Login, Monitor, Analytics, Sensor simulation, Dispatch controls, Register bin, Profile). Visitor mode: Operate. The Dispatch map canvas and its behavior are out of scope and must not change (owner constraint).

Audience and job: dispatchers, staff and drivers at a waste office desk, and panelists watching it on a projector. Find critical bins, dispatch a road route, and record the stops.

Constraints: Swing + FlatLaf only; WCAG AA contrast; white is the default theme and the dark switch stays (owner, 2026-10-27); no new claims or data.

## Direction contract

THESIS: A calm white workbench: every screen is a clean white sheet, and colour appears only where a bin needs someone. It replaces the green guide-sign rail and refuses the category default of a slate sidebar, an emerald accent and four striped KPI cards.

OWN-WORLD: White sheets (#FFFFFF) on a barely green-grey ground (#F5F7F6), 1px #E2E7E4 hairlines and a soft two-step shadow under each sheet; 14px sheet corners, 10px controls, pill status chips led by a state dot. One forest-green accent (#0E6E46) for the primary action, the current page and selection. States: red critical, amber due, green normal. Geist for every word, Geist Mono for bin codes and compared figures. Authored 1.75-stroke icons. The night theme is the same sheets in graphite.

STORY: The dispatcher reads the fill line, sees which bins crossed the due and critical marks, presses the one green Dispatch button, and works the stops.

FIRST VIEWPORT: Monitor. A 248px white sidebar with a hairline edge: logo mark and wordmark, six nav rows, the auto-simulation toggle, an avatar footer. Main column: "Monitor" at 26px SemiBold with the updated time, then the fleet sheet: four counts in one row (critical in red when above zero) over the fill line, every bin a dot on a 0–100% track with Due 60% and Critical 85% marks, and the primary "Dispatch N critical bins" button at the sheet's right. Then the toolbar (search, pill filter, Mark collected, Remove bin isolated) and the bin table sheet.

FORM: User-pinned world ("clean aesthetic, smooth white with fonts"; font chosen by the owner: Geist + Geist Mono). No concept roll, because the owner pinned the world. seed: none (user-pinned). Kept from the previous contract: one reserved marking for the current page and the next action, destructive actions isolated and outlined, one bin-code style, and mono figures.

Signature interaction: the fill line. Hovering a dot names the bin and its fill; clicking it selects that row in the table. It updates live while auto-simulation runs.

FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance
