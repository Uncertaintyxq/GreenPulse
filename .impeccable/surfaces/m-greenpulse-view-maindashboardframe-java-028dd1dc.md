---
version: 1
slug: "m-greenpulse-view-maindashboardframe-java-028dd1dc"
primary_target: "src/main/java/com/greenpulse/view/MainDashboardFrame.java"
related_targets: ["src/main/java/com/greenpulse/view/LoginFrame.java","src/main/java/com/greenpulse/view/AnalyticsPanel.java","src/main/java/com/greenpulse/view/SimulatePanel.java","src/main/java/com/greenpulse/view/AddBinPanel.java","src/main/java/com/greenpulse/view/UserProfilePanel.java","src/main/java/com/greenpulse/view/RouteMapPanel.java"]
---

# GreenPulse console (all Swing screens)

Scope: the desktop console shell and its screens (Login, Monitor, Analytics, Sensor simulation, Dispatch controls, Register bin, Profile). Visitor mode: Operate. The Dispatch map canvas and its behavior are out of scope and must not change (owner constraint).

Audience and job: dispatchers, staff and drivers at a waste office desk, and panelists watching it on a projector. Find critical bins, dispatch a road route, and record the stops.

Constraints: Swing + FlatLaf only; WCAG AA contrast; working light and dark themes; no new claims or data.

## Direction contract

THESIS: The console reads like the road the collection trucks drive: guide-sign panels give the dispatcher the next move. It refuses the category default of a slate sidebar, an emerald accent and four KPI cards with coloured left borders.

OWN-WORLD: Sign green #0B5137 rail drawn as a guide sign, with an inset 1.5px white border, 10px corners and white legends. A #F3F5F1 ground carrying a faint retroreflective prism lattice. White plates with 1px hairlines. State colours follow sign classes: regulatory red #B42318 for critical, advisory yellow #F2B705 with black ink for filling, guide green for normal. Data series use service blue #1F4E8C for recycling and tourist brown #7A4E24 for compost. Overpass for everything, Overpass Mono for bin codes and figures. Authored stroke icons, no emoji. Dark theme is the night sign.

STORY: The dispatcher reads fleet state off the gantry, sees what is critical, takes the exit to Dispatch and works the stops. Analytics answers whether collection is keeping up.

FIRST VIEWPORT: Monitor. A 256px sign rail on the left holds the brand, six nav items, the auto-simulation control and the user footer. Main column: the title with its last-updated time, then an overhead gantry sign at full width, with active, critical, normal and average-fill counts in 44px white on the left and, on the right, an exit panel with a yellow NEXT tab reading "Dispatch N critical bins →" (the primary action). Below it a toolbar (search, segmented filter, Mark collected, and Remove bin isolated in red outline), then the bin table plate.

FORM: Road Guide Sign, position 3 of 7 on the ordered list (RA 9003 segregation code, NAMRIA topo sheet, DPWH guide signs, PAGASA bulletins, DENR-EMB forms, truck livery, field-guide plates). seed:e6a50bba138dff49

Raises kept from declined challengers: colour only inside bounded plates (lexicon); one reserved marking for the active page and next action (orienteering); flat sign colour (zoo); destructive actions isolated and outlined (console); one bin-code style everywhere (doujin); one fixed tonal ramp for neutrals (exposure).

Signature interaction: the gantry's exit panel updates live with the critical count and opens Dispatch. When no bin is critical or due it drops the tab and reads "No bins need collection" in quiet green (a bin can be critical below 85% through its priority score, so the 85% wording would be untrue). Motion: state changes only, 150–200 ms.

FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance
