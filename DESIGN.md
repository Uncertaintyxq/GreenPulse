---
name: GreenPulse
description: A waste-collection console that reads like the road the trucks drive, with guide-sign panels that give the dispatcher the next move.
colors:
  sign-green: "#0B5137"
  sign-green-deep: "#08402B"
  sign-green-hover: "#11613F"
  sign-legend: "#FFFFFF"
  sign-legend-soft: "#CFE3D6"
  regulatory-red: "#B42318"
  regulatory-red-ink: "#FFFFFF"
  regulatory-red-text: "#A3201A"
  regulatory-red-soft: "#FBE9E7"
  advisory-yellow: "#F2B705"
  advisory-yellow-ink: "#17231C"
  advisory-yellow-text: "#8A5A00"
  advisory-yellow-bar: "#A87A00"
  advisory-yellow-soft: "#FDF3D3"
  guide-ok: "#0B5137"
  guide-ok-text: "#0B5137"
  guide-ok-soft: "#E3EEE7"
  service-blue: "#1F4E8C"
  tourist-brown: "#7A4E24"
  fleet-grey: "#43534A"
  ground: "#F3F5F1"
  surface: "#FFFFFF"
  surface-2: "#F7F8F6"
  line: "#D5DCD6"
  line-2: "#EAEEEA"
  control-border: "#828D86"
  ink: "#17231C"
  ink-2: "#43534A"
  ink-3: "#5D6B63"
  row-selected: "#DCEBE1"
typography:
  display:
    fontFamily: "Overpass ExtraBold, Overpass, sans-serif"
    fontSize: "42px"
    fontWeight: 800
  headline:
    fontFamily: "Overpass ExtraBold, Overpass, sans-serif"
    fontSize: "28px"
    fontWeight: 800
  figure:
    fontFamily: "Overpass ExtraBold, Overpass, sans-serif"
    fontSize: "30px"
    fontWeight: 800
  sign-action:
    fontFamily: "Overpass Bold, Overpass, sans-serif"
    fontSize: "20px"
    fontWeight: 700
  title:
    fontFamily: "Overpass Bold, Overpass, sans-serif"
    fontSize: "17px"
    fontWeight: 700
  nav:
    fontFamily: "Overpass SemiBold, Overpass, sans-serif"
    fontSize: "15px"
    fontWeight: 600
  subtitle:
    fontFamily: "Overpass, sans-serif"
    fontSize: "15px"
    fontWeight: 400
  body:
    fontFamily: "Overpass, sans-serif"
    fontSize: "14px"
    fontWeight: 400
  button:
    fontFamily: "Overpass SemiBold, Overpass, sans-serif"
    fontSize: "14px"
    fontWeight: 600
  label:
    fontFamily: "Overpass SemiBold, Overpass, sans-serif"
    fontSize: "13px"
    fontWeight: 600
  caption:
    fontFamily: "Overpass, sans-serif"
    fontSize: "13px"
    fontWeight: 400
  badge:
    fontFamily: "Overpass Bold, Overpass, sans-serif"
    fontSize: "13px"
    fontWeight: 700
  mono-figure:
    fontFamily: "Overpass Mono Medium, Overpass Mono, monospace"
    fontSize: "14px"
    fontWeight: 500
  mono-code:
    fontFamily: "Overpass Mono SemiBold, Overpass Mono, monospace"
    fontSize: "13px"
    fontWeight: 600
  mono-axis:
    fontFamily: "Overpass Mono Medium, Overpass Mono, monospace"
    fontSize: "12px"
    fontWeight: 500
rounded:
  check: "4px"
  chip: "6px"
  segment: "7px"
  control: "8px"
  legend-border: "10px"
  plate: "12px"
  sign: "16px"
  pill: "999px"
spacing:
  s1: "4px"
  s2: "8px"
  s3: "12px"
  s4: "16px"
  s5: "24px"
  s6: "32px"
  s7: "48px"
components:
  button-primary:
    backgroundColor: "{colors.sign-green}"
    textColor: "{colors.sign-legend}"
    typography: "{typography.button}"
    rounded: "{rounded.control}"
    padding: "7px 16px"
  button-primary-hover:
    backgroundColor: "{colors.sign-green-hover}"
  button-primary-active:
    backgroundColor: "{colors.sign-green-deep}"
  button-secondary:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.ink}"
    typography: "{typography.button}"
    rounded: "{rounded.control}"
    padding: "7px 16px"
  button-danger:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.regulatory-red-text}"
    typography: "{typography.button}"
    rounded: "{rounded.control}"
    padding: "7px 16px"
  button-danger-hover:
    backgroundColor: "{colors.regulatory-red-soft}"
  button-quiet:
    textColor: "{colors.guide-ok-text}"
    typography: "{typography.button}"
    rounded: "{rounded.control}"
    padding: "7px 16px"
  input-field:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.ink}"
    typography: "{typography.body}"
    rounded: "{rounded.control}"
    padding: "6px 10px"
    height: "40px"
  segmented-control:
    backgroundColor: "{colors.surface}"
    rounded: "{rounded.legend-border}"
    padding: "3px"
  segmented-option:
    textColor: "{colors.ink-2}"
    typography: "{typography.label}"
    rounded: "{rounded.segment}"
    padding: "7px 14px"
  segmented-option-selected:
    backgroundColor: "{colors.sign-green}"
    textColor: "{colors.sign-legend}"
  nav-item:
    textColor: "{colors.sign-legend}"
    typography: "{typography.nav}"
    rounded: "{rounded.control}"
    padding: "10px 12px"
    height: "44px"
  nav-item-hover:
    backgroundColor: "{colors.sign-green-hover}"
  nav-item-active:
    backgroundColor: "{colors.sign-legend}"
    textColor: "{colors.sign-green}"
  sign-panel:
    backgroundColor: "{colors.sign-green}"
    textColor: "{colors.sign-legend}"
    rounded: "{rounded.sign}"
  plate:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.ink}"
    rounded: "{rounded.plate}"
    padding: "24px"
  bin-code-chip:
    backgroundColor: "{colors.surface-2}"
    textColor: "{colors.ink}"
    typography: "{typography.mono-code}"
    rounded: "{rounded.chip}"
    padding: "0 7px"
    height: "24px"
  status-critical:
    backgroundColor: "{colors.regulatory-red}"
    textColor: "{colors.regulatory-red-ink}"
    typography: "{typography.badge}"
    rounded: "{rounded.chip}"
    padding: "0 10px"
    height: "24px"
  status-due:
    backgroundColor: "{colors.advisory-yellow}"
    textColor: "{colors.advisory-yellow-ink}"
    typography: "{typography.badge}"
    rounded: "{rounded.chip}"
    padding: "0 10px"
    height: "24px"
  status-normal:
    backgroundColor: "{colors.guide-ok-soft}"
    textColor: "{colors.guide-ok-text}"
    typography: "{typography.badge}"
    rounded: "{rounded.chip}"
    padding: "0 10px"
    height: "24px"
  next-tab:
    backgroundColor: "{colors.advisory-yellow}"
    textColor: "{colors.advisory-yellow-ink}"
    typography: "{typography.badge}"
    rounded: "{rounded.chip}"
    padding: "0 8px"
    height: "22px"
  table-header:
    backgroundColor: "{colors.surface-2}"
    textColor: "{colors.ink-2}"
    typography: "{typography.label}"
    padding: "0 12px"
    height: "38px"
  table-row:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.ink}"
    typography: "{typography.body}"
    padding: "0 12px"
    height: "40px"
  table-row-selected:
    backgroundColor: "{colors.row-selected}"
---

# Design System: GreenPulse

<!-- Recorded from the shipped Swing build (src/main/resources/com/greenpulse/theme/*.properties, view/Theme.java, Ui.java, Renderers.java, Icons.java, MainDashboardFrame.java and the .impeccable/review captures). Direction: "Road Guide Sign", seed:e6a50bba138dff49. Frontmatter values are the day (light) theme; night values live in .impeccable/design.json colorMeta and in FlatDarkLaf.properties, which is the source of truth for both. -->

## Overview

**Creative North Star: "The Road Guide Sign"**

The console reads like the road the collection trucks drive. A sign-green rail on the left is drawn as a real guide sign: a flat green panel, an inset white legend border, white legends, and a faint retroreflective prism lattice in the sheeting. Across the top of Monitor an overhead gantry sign carries the fleet counts in big white figures, and its exit panel tells the dispatcher where to go next. Everything else sits on a pale, green-tinted ground on white working plates with 1px hairlines. That is how a reflective sign panel separates from the verge.

The system is an operator's desk tool built to hold up on a projector. It is dense but readable: 40px table rows, 14px body text, figures in Overpass Mono so columns line up, and every text pair at WCAG AA or better. Colour does work and never decorates. It follows the classes a driver already knows: regulatory red for critical, advisory yellow with black ink for due, guide green for normal. Service blue and tourist brown are kept for the two data series, recycling and compost. The night theme is the same sign seen at night. The sign panels stay sign green, the ground drops to a near-black green, and the state and series colours lighten so they stay legible.

The build deliberately turns away from the category default: a slate sidebar, an emerald accent, and four KPI cards with coloured left borders.

**Key Characteristics:**
- One flat sign green (#0B5137) for every sign panel, in both themes.
- A prism lattice on the ground and on signs. It is sheeting texture, never a pattern for its own sake.
- White plates with hairlines carry all working content. Colour lives inside bounded plates.
- Overpass for every word, Overpass Mono for every bin code and figure.
- An authored 18-unit icon set with a 1.75 round stroke, drawn in Java2D.
- Light (day) and dark (night) themes, switched live. Every component re-reads its token on the switch.

## Colors

A sign-sheeting palette: one deep guide green, three sign-class state colours, two series colours, and one green-tinted neutral ramp.

### Primary
- **Sign Green** (sign-green): fills the rail, the gantry, the login sign, primary buttons, the selected segment and the progress and slider fill. It stays the same value at night. Hover is **Sign Green Lit** (sign-green-hover) and pressed is **Sign Green Deep** (sign-green-deep). In the night theme, primary buttons and selected segments use the lit green at rest and the base green on hover, so they hold their edge against the dark ground.
- **Sign Legend** (sign-legend) and **Soft Legend** (sign-legend-soft): white legends and the inset border on sign green, with the soft legend for captions, the role line and the quiet "No bins need collection" state (6.95:1 on sign green).

### Secondary (sign classes as state)
- **Regulatory Red** (regulatory-red): the Critical status plate and critical fill bars. Its text-safe sibling **Regulatory Red Text** (regulatory-red-text) is for critical figures, error lines and the Danger button's outline and label. **Regulatory Red Soft** is only the Danger hover wash.
- **Advisory Yellow** (advisory-yellow), always with black ink (advisory-yellow-ink, 8.93:1): the Due status plate and the gantry's NEXT tab. **Advisory Yellow Bar** is the darker fill for due-level bars on white (3.85:1 as a graphic). **Advisory Yellow Text** is for yellow-class words on plates.
- **Guide OK** (guide-ok, guide-ok-text, guide-ok-soft): the Normal status plate (green text on a pale green plate), quiet buttons, the role line, collected stops and pick-mode prompts.

### Tertiary (data series)
- **Service Blue** (service-blue): recycling series and column headings.
- **Tourist Brown** (tourist-brown): compost series and column headings.
- **Fleet Grey** (fleet-grey): the fleet-average series. It shares its value with ink-2.

### Neutral
- **Ground** (ground): the page field, under a lattice of sign green at about 4% alpha (`GP.lattice`).
- **Surface** (surface) and **Surface 2** (surface-2): plates, tables and inputs; table headers, chip fills and hover washes.
- **Line** (line) and **Line 2** (line-2): plate hairlines and header rule; table grid and empty fill-bar track. These are structural, not control boundaries.
- **Control Border** (control-border): the boundary of every input, combo, secondary button and segmented control (3.44:1 on white, 3.14:1 on ground).
- **Ink, Ink 2, Ink 3** (ink, ink-2, ink-3): primary text; secondary text and table headers; captions, placeholders and inline icons (5.6:1 on white at the lowest step).
- **Row Selected** (row-selected): the selected table row, a pale green.

### Named Rules
**The Plate Rule.** Colour appears only inside a bounded shape: a sign panel, a status plate, a chip, a bar, a button, a chart on its plate. The ground and the page header stay neutral. The login window is the one full-bleed sign, and it is still a sign.

**The One Marking Rule.** Only one reserved marking exists per job. The white plate on the green rail marks the current page. The yellow NEXT tab on the gantry marks the next action. Nothing else may wear the white-on-rail plate, and yellow fills appear only on the NEXT tab and the Due status.

**The One Ramp Rule.** Every neutral comes from the single green-tinted GP ramp (ground, surface, surface-2, line, line-2, ink, ink-2, ink-3, plus control-border). No cool slate greys and no pure #000.

**The Contrast Floor Rule.** Text meets WCAG AA (4.5:1 body; every recorded text pair clears it in both themes). Control borders hold at least 3:1 against what they sit on. The night control border (#5F7066) sits exactly at 3.0:1 on the night input fill, which is the floor, not headroom.

## Typography

**Display Font:** Overpass (ExtraBold, Bold, SemiBold, Regular; bundled TTFs, SIL OFL)
**Body Font:** Overpass Regular
**Label/Mono Font:** Overpass Mono (Medium, SemiBold)

**Character:** Overpass is derived from Highway Gothic, the road-sign face, so legends on the signs read as real signage. Overpass Mono is its tabular sibling and keeps codes and columns of figures aligned.

### Hierarchy
- **Display** (ExtraBold 800, 42px): gantry counts in white on the sign. Only on the gantry.
- **Headline** (ExtraBold 800, 28px): the page title, one per page. The login brand uses 34px and the rail brand 24px in the same weight.
- **Figure** (ExtraBold 800, 30px): headline figures in the Analytics figures plate.
- **Sign Action** (Bold 700, 20px, 23px when alerting): the exit-panel line on the gantry.
- **Title** (Bold 700, 17px): plate titles and empty-state headings.
- **Nav** (SemiBold 600, 15px): rail items.
- **Subtitle** (Regular 400, 15px): the one-line purpose under a page title, in ink-2.
- **Body** (Regular 400, 14px): table cells, field text, prose. Base `defaultFont = 14 Overpass`.
- **Button** (SemiBold 600, 14px): all buttons. Login's sign-in button uses Bold 15px.
- **Label** (SemiBold 600, 13px): field labels, table headers, segment options, gantry captions at 15px.
- **Caption** (Regular 400, 13px): plate subtitles and helper text in ink-3.
- **Badge** (Bold 700, 13px): status plates and the NEXT tab.
- **Mono Figure** (Overpass Mono Medium 500, 14px; SemiBold for critical values): fill percentages, priorities, readings, coordinates.
- **Mono Code** (Overpass Mono SemiBold 600, 13px): bin IDs.
- **Mono Axis** (Overpass Mono Medium 500, 12px): chart axes and ticks.

### Named Rules
**The Mono Figure Rule.** Every number a dispatcher compares (fill, priority, reading, coordinate, axis tick) is set in Overpass Mono and right-aligned in tables. Big hero counts stay in Overpass ExtraBold.

**The Sentence Case Rule.** Legends, buttons and badges are sentence case ("Critical", "Mark collected"). The NEXT tab is the one uppercase legend, because it copies the exit tab on a real sign.

## Layout

A fixed 256px sign rail on the left, set 10px into the window with 14px padding, and a main column with 32px top and bottom and 40px side gutters. Pages stack vertically: a page header (title, 4px, subtitle), then 24px gaps between the gantry or filter row, the toolbar and the content plates. Spacing follows the S scale (4, 8, 12, 16, 24, 32, 48px). Plates pad 24px, or 32px for form and profile plates. Field label to field is 6px. The 40px page gutter is the one recurring off-scale value. Page content scrolls only vertically and always tracks the window width (`ScrollBody`). The target window is about 1320×780, so the layout does not reflow for small screens.

Toolbars place everyday controls on the left (search 280×40, segmented filter) and actions on the right, with the destructive action last and separated by a 24px gap. Tables use 40px rows, a 38px header, 12px cell margins, horizontal rules only, and right-aligned numeric columns with 24px trailing room.

Dispatch is a split: a scrolling control plate (24px padding, hairline-ruled sections) beside the map canvas, which carries a 2px sign-green frame.

## Elevation & Depth

The system is flat. Sign panels, plates and chips sit at one level and separate by colour and hairline, never by shadow. Depth comes from tone instead: green-tinted ground, white plate, then a 1px line, with the sign green as the most saturated layer. The prism lattice adds texture, not lift: diagonal 60° lines every 26px on the ground and every 22px on signs (a stronger lattice on the full-bleed login sign). FlatLaf's 2px focus ring (sign green at 40% alpha) is the only halo.

### Named Rules
**The Flat Sheeting Rule.** No drop shadows on plates, signs, chips or buttons. If something needs to read as separate, give it a plate and a hairline.

## Shapes

Gently rounded and sign-like. Controls and buttons use 8px corners. Plates use 12px. Sign panels use 16px outer corners with a 1.5px white legend border inset 7 to 14px at 10px corners. That inset border is the sign's signature, and it appears on every Sign. Chips, status plates and the NEXT tab use 6px corners. Segments use 7px inside a 10px track. Fill bars and scrollbar thumbs are fully rounded. A full-bleed sign (login) drops its outer corners and keeps the inset border.

Icons come from an authored set on an 18-unit grid with one 1.75-unit round-cap, round-join stroke, drawn at 14 to 26px in the component's text colour or a named token: gauge, chart, sliders, route, bin, person, play, stop, search, check, trash, arrows, pin, and others.

## Components

### Buttons
Sign-panel buttons: firm, rectangular-ish, and quiet unless they lead.
- **Shape:** 8px corners, 7px × 16px margin, SemiBold 14, 16px authored icon with an 8px gap.
- **Primary:** solid sign green with a white label (lit green at rest in the night theme). Use one per surface for the leading action: Dispatch, Save, Sign in.
- **Secondary:** white (night: surface-2) with the control border and an ink label. On hover the border turns sign green.
- **Danger:** outlined only. The label and border use regulatory-red-text, and hover adds the red-soft wash. Never filled.
- **Quiet:** borderless, with a guide-ok-text label, for low-stakes view actions ("Fit depot and bins in view").
- **On signs:** rail buttons invert. Outlined white on green at rest. White with a green legend when running.

### Segmented control
- **Style:** one bordered white track (control border, 10px, 3px padding) holding SemiBold 13 options at 7px × 14px.
- **State:** the selected option is a solid sign-green plate with a white legend, and hover is a surface-2 wash. Focus shows a 2px guide-ok ring. Used for the Monitor filter, the theme switch and Dispatch speed.

### Plates
- **Corner Style:** 12px.
- **Background:** surface, with a 1px line hairline.
- **Shadow Strategy:** none (see The Flat Sheeting Rule).
- **Internal Padding:** 24px default, 32px for forms and profile, 0 when a table or figure grid fills it.
- **Header:** Bold 17 title, a 2px gap, then a Regular 13 ink-3 caption.

### Inputs / Fields
- **Style:** white fill, control border, 8px corners, 40px tall, 6px × 10px margin, Regular 14. Placeholder in ink-3. Bin IDs and coordinates use Overpass Mono.
- **Label:** SemiBold 13 ink-2 above the field, with a 6px gap.
- **Focus:** the border turns sign green with a 2px focus ring.
- **Disabled / read-only:** surface-2 fill with a muted foreground.

### Tables
- **Header:** surface-2, SemiBold 13 ink-2, 38px, with a line rule below.
- **Rows:** 40px, line-2 rules, no vertical lines, pale-green selection. A trailing arrow-only cell (ink-3) opens a related page and carries its label as tooltip and accessible name.
- **Fill bar:** an 8px rounded bar on a line-2 track with a right-aligned mono percentage. Red at critical, advisory-bar at due, green otherwise. Critical figures are bold and red.
- **Empty state:** a centred Bold 17 heading and a Regular 14 ink-3 line that names the next step.

### Bin code chip
**The One Code Rule.** A bin ID is always Overpass Mono SemiBold 13 on a 24px surface-2 chip with a 1px line border and 6px corners (white when its row is selected). This applies in the Monitor table and both Analytics tables. New surfaces showing a bin ID in a table or list must use this chip.

### Status plates
Sign-class plates, 24px tall with 6px corners and Bold 13: **Critical** in white on regulatory red, **Due** in black on advisory yellow, **Normal** in guide-ok-text on guide-ok-soft. The table keeps the raw values for filtering.

### Navigation (sign rail)
- **Style:** a Sign panel (inset 10, padding 14) holding the brand (ExtraBold 24), six items, the auto-simulation control, and a user footer under a soft-legend hairline.
- **Items:** SemiBold 15 white legend with an 18px authored icon, 10px × 12px, 44px tall, 8px corners.
- **States:** hover and focus fill sign-green-hover, and keyboard focus adds a soft-legend outline. The active page is a white plate with a sign-green legend (the reserved marking). Its accessible description reads "Current page".

### Gantry sign with exit panel (signature)
A full-width Sign (inset 7) on Monitor. On the left are four counts (ExtraBold 42 white over SemiBold 15 soft-legend captions). On the right, behind a 2px white divider, is the exit panel (380×108). It shows a yellow NEXT tab and a Bold 23 line ("Dispatch N critical bins"), or an outlined white tab with Bold 20 for due bins, then a 26px arrow. It opens Dispatch. With nothing critical or due it drops the tab and reads "No bins need collection" in the soft legend. Hover and focus fill sign-green-hover.

### Login
A full-bleed Sign across the window with the strong lattice. On the left are the brand legend and sign-arrow rows. On the right is a white plate (32px × 44px padding) holding the sign-in form and a 46px Bold 15 primary button.

### Analytics by role
Administrators get the detailed operator view: figure strip, charts, comparison grid, forecast table. Staff and drivers get the simple view, which uses the same data and calculations with none of the charts or percentages.
- **Answers plate:** one plate of up to five plain-language answer rows, split by LINE2 hairlines. Each row is a 24px state-coloured icon, a Bold 19 headline sentence and a Regular 15 INK2 detail line. Only the "collect now" or "next route" answer carries an action, a primary "Open Dispatch →" button.
- **Bins to watch:** the top five bins by priority. Each row shows the bin's code chip, its location, a fill level in words (`PlainLanguage.fillLevel`: nearly empty → almost full) and a rounded badge: Collect now (red), Next route (yellow) or Fine (soft green).
- **Wording:** all words come from `com.greenpulse.analytics.PlainLanguage`, which has unit tests. The fill bands follow `CollectionPolicy`. Durations are rounded to friendly units ("about 2 hours").
- **Period:** a two-option Segmented control, Last 24 hours or Last 7 days. Simulated readings are always included.

### Toast
A tooltip-coloured rounded (10px) message at the top right with a leading icon, SemiBold 14, flat (no shadow). It closes after 4 seconds, and a new message restarts the timer.

### Motion
Not built. The direction contract asked for 150–200 ms state-change motion. The shipped build changes state instantly, with no animated transitions on hover, selection, theme switch or the exit panel's live update. Future work may add it within that 150–200 ms, state-change-only budget. Do not treat instant changes as a rule.

### Out of scope: the Dispatch map canvas
`MapCanvas` in RouteMapPanel.java is owner-protected and outside this design system. That covers its tiles, routing, pins, truck animation, depot and bin placement, its overlays and its own colours and glyphs. This system governs only the Dispatch control plate around it and the 2px sign-green frame. Do not restyle, re-token or "fix" the canvas.

## Do's and Don'ts

### Do:
- **Do** read every colour through a GP token (`Theme.color`, `Theme.bg`, `Theme.fg`, `Theme.onThemeChange`) so it re-applies on the live theme switch, and define new tokens in both FlatLightLaf and FlatDarkLaf properties.
- **Do** put working content on a Plate (surface, 1px line, 12px) on the lattice ground, and put sign-class colour only inside a bounded plate, chip, bar or sign.
- **Do** keep a single reserved marking: the white plate in the rail for the current page, and the yellow NEXT tab for the next action.
- **Do** isolate the destructive action. It goes last in its row, behind a 24px gap, as an outlined Danger button in regulatory-red-text, never filled.
- **Do** render every bin ID with the one mono chip, and every compared figure in Overpass Mono, right-aligned.
- **Do** keep text at WCAG AA or better and control borders at 3:1 or better in both themes, and check night values separately.
- **Do** use the authored 18-unit, 1.75-stroke icon set, and add new glyphs to Icons.java on the same grid and stroke.
- **Do** use the S1–S7 spacing scale (4–48px), with the 40px page gutter as the only exception.

### Don't:
- **Don't** use emoji or Unicode glyphs as icons anywhere in the console chrome.
- **Don't** add eyebrow or kicker labels above titles. The page title and its subtitle carry the context. (The NEXT tab is the sign's exit tab, not an eyebrow.)
- **Don't** build cards with coloured left borders, or a row of KPI cards striped in accent colours. (The gantry's white divider separates two zones of one sign. It is not a card stripe.)
- **Don't** introduce neutrals outside the GP ramp, cool slate greys, or a second green.
- **Don't** spend advisory yellow, regulatory red or the white-on-rail plate on decoration, emphasis or branding.
- **Don't** add drop shadows to plates, signs, chips or buttons.
- **Don't** touch the Dispatch map canvas or its behaviour to make it match this system.
