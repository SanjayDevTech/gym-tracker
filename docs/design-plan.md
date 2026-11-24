# Design plan — Gym Tracker UI/UX rewrite

> **Status: proposal, 2026-11-23. Nothing here is implemented.** Written in two passes: a complete
> shot-free draft from the current-state inventory and food-tracker's conventions, then a revision
> against six reference shots (§0 "Revision from shots"). The owner settled all eleven open
> questions on 2026-11-23 (§9, cited in the body as "decision N"); the body reflects those decisions.

## 0. Preliminaries

### Reference shots — seen 2026-11-23

Six Dribbble shots, supplied as images (S1–S6; phones a/b/c left to right). They were read only
after a complete shot-free draft (v1) existed, so each adoption below is a revision of a decision
already made, not the source of it. No URLs are cited.

**The set is not one direction, it is three, and only two of them fit.**

- **A — soft / photographic** (S1, S5, S6): filled rounded cards at 16–32dp, rings, tinted icon
  discs, stock photography, emoji, charcoal (not black) canvases in S1 and S6, gamification (S6's
  "points"). It is the anatomy food-tracker's editorial pass existed to remove.
- **B — editorial / instrument** (S4): true black, flat, hairlines instead of cards, uppercase
  tracked labels, a large hero numeral with its reference value right-aligned between two rules, a
  three-column label/value row. This is the family voice v1 already wrote.
- **C — the logger** (S2, S3): a real strength tracker on true black. Its *anatomy* is right — a big
  centred elapsed clock with Finish beside it, a `SET / WEIGHT / REPS / DONE` table, a full-width
  tint on completed rows, collapsible exercise blocks with `1/3` progress, a sticky bottom CTA on the
  exercise picker — but it is built from `#1C1C1E` cards and orange icon discs.

**DECISION: B's voice, C's structure.** Screens keep v1's standing heads, eyebrows and hairlines (S4).
Where S2/S3 lay a working screen out better than v1 did, the layout is taken and its cards are
dissolved into rules. A is used only for two interaction idioms (S5-g's timer transport, S5-d's
full-width Resume button) and refused for everything visual.

**Adopted from specific shots:**

| From | Pattern | Changes |
|---|---|---|
| S2-b | **Centred elapsed clock in the top bar + outlined `Finish` beside it** | §4.3 — Finish leaves the bottom dock; clock goes from subtitle `bodyS` to centred `numberL` |
| S2-b | **Full-width tint on a completed set row** | §4.3, §1 house rule 3 (gym exception) — replaces v1's 16dp dot as the "done" signal |
| S2-b | **Collapsible exercise blocks with `x/y` progress** | §4.3 — finished exercises collapse to one line |
| S5-g | **Timer transport: big mm:ss, thin progress, a centre button between two side actions** | §4.3 dock, §4.4 — `−15 s  [Skip]  +15 s` under the rest clock |
| S5-d | **Resume card with a full-width primary button** | §4.1 — the live strip gains a full-width `Resume` |
| S2-a | **Routine row with an embedded trend + `Last · date` + Start** | §4.1 — plan rows get a 96×28dp volume sparkline of their last 8 sessions |
| S3-b | **Exercise picker as a page: search, filter button, checkbox multi-select, sticky `Next`** | §4.13 — sheet becomes a page; add several exercises in one pass |
| S2-c | **Per-exercise block with right-aligned set rows (`15 kg … 10 reps`)** | §4.9 — Session detail set rows become a two-column table |
| S4-b | **Hero numeral left, reference value right, between two hairlines** | §4.11 (est. 1RM / best), §4.12 (training days / weeks on target) |
| S4-b, S4-c | **Three/four-column uppercase label / value row** | §4.5, §4.9 — confirms `StatColumns`; adds a PRs column |
| S4-b | **Bar with a bright top cap, dashed reference line, % tag over the selected bar** | §4.12 — weekly volume vs the previous 4-week average |
| S6-b | **`‹ 4 Jan – 10 Jan ›` period pager** | §4.12 — step back through weeks/months (was "now" only) |
| S2-a | **Line chart with y values, date axis and a highlighted last point** | §2 `LineChart` — confirms v1's axes; adds the last-point ring + value |
| S4-b | **Day eyebrows grouping a list (`TODAY`, `THURSDAY`)** | §4.8 — History rows lose their date column |

**Explicitly refused:**

- **Cards as structure** (S1, S2, S3, S5, S6) — on `#000000` a `#1C1C1E` slab is the loudest thing on
  screen; the hairline does the same job in 1dp (house rule 1).
- **Rings** (S1-a/c, S2-a's tri-ring, S5-d tiles) — S2-a's "Tonnage 70/100" has an invented
  denominator, and the family has no rings. The weekly target (decision 7) is drawn as a `Meter`.
- **Tinted icon discs, monograms, colour-coded muscle chips** (S2, S3-b, S6-a) — six muscle hues on
  black would be a second palette with no meaning elsewhere in the app; house rule 4.
- **Category circles with count badges** (S6-a) — icon discs again.
- **Photography, play-button rows, onboarding splashes** (S1, S4-a/c, S5) — 176 seeded exercises, no
  images, none coming.
- **Kcal, steps, sleep, water, points, day-rating emoji** (S1, S4, S5, S6) — not tracked.
- **Greeting + avatar + bell headers** (S1, S2, S5) — a personal app has no inbox.
- **Charcoal canvases and purple gradients** (S1, S6) — OLED black is a requirement.
- **Filled segmented control** (S6-b) and **filled selected day** (S2-c) — selection is a rule
  (house rule 3).
- **Square, zero-radius geometry and uppercase screen titles** (S4) — the eyebrow already carries the
  uppercase voice; a second uppercase level flattens the hierarchy, and family radii stay.
- **A "previous" column in the set table** (suggested for S2-b) — the hint numerals already *are*
  last session's values, rendered in place; a column would cost the ~80dp the 28sp numerals need.
- **Inline "Take a 2 minutes break" rows** (S5-h) — there is no per-exercise rest in the data model;
  the rest target is per session.
- **Horizontal day strip on History** (S2-c) — at ≤1 session a day most cells are empty and it hides
  the list it filters.
- **Hatched "goal" portion of bars** (S4-b) — the weekly target (decision 7) counts sessions, the bars
  show volume; a hatched goal would mix the two. Weeks that met target are marked on the calendar instead.
- **Plan-builder review step** (S3-c) — the Plan editor is already that step.
- **Trailing "Show All" links in section heads** (S2) — the trailing slot holds the section's figure;
  actions sit in the foot.

**What survives the move to OLED.** S2, S3 and S4 are already true black and transfer directly. The
colour values do not: S2's `#FF5722` orange and S4's `#5CE6D6` aqua are tuned for marketing renders and
would glare at full screen brightness in a dim gym. The method is food's — copy the *relationships*
(S4's accent-to-canvas, label-to-value, hairline-to-black) and re-derive the values.

**Accent re-checked against the references and unchanged.** S2 is orange, the closest functional
match, and orange is the app's current identity. It is still refused: orange sits on top of the
family's failure/over hue (`#F08A4B`) and next to destructive red, and in a logger where failure sets
are marked that collision is constant. S4's aqua and S5's mint are food-tracker's territory. S2's own
third ring is a lime (`#C6F432`), which independently confirms volt as a gym hue. Volt stands (decision 6).

**Revision from shots (v1 → v2).**

1. **Logger re-laid out after S2-b**: Finish moves to the top bar beside a centred elapsed clock; the
   dock becomes the rest clock with an S5-g transport; completed rows get a full-width tint (a
   documented exception to house rule 3); finished exercises collapse.
2. **Exercise picker becomes a page with multi-select** (S3-b) — v1 kept a single-pick sheet.
3. **Progress gains a period pager and a reference-line bar chart** (S4-b, S6-b); its consistency and
   Exercise detail's 1RM heroes take S4-b's number-left / reference-right layout.
4. **Train**: the live strip gets a full-width Resume (S5-d); plan rows get a volume sparkline (S2-a).
5. **History and Session detail**: day eyebrows instead of a date column (S4-b); set rows become a
   right-aligned table (S2-c); a PRs stat column (S4-c).
6. **Library**: v1's three-dropdown `FilterBar` → search + filter button + active-filter line +
   filter sheet (S3-b), possible now that the picker is a page.
7. **Unchanged after checking**: palette, type scale, no cards, no rings. The shots added one open
   question (the muscle-volume band, now decision 11).

### What I actually read

The full current-state inventory (10 routes, 13 sheets/dialogs, 42 UX issues), the food-tracker
plan, `ui-house-rules.md` and its implemented tokens, and these gym-tracker files to settle
points the inventory left open: `ui/theme/Theme.kt`, `Dimens.kt`, `data/ExerciseLibrary.kt`,
`MuscleGroup.kt`, `SetType.kt`, `db/SessionEntity.kt`, `db/SessionDraftEntity.kt`,
`SessionLoggerViewModel.kt` (init/hydrate/commit), `navigation/AppNav.kt` (transitions),
`ExclusiveButtonGroup.kt`, `SessionNotification.kt`, `MainActivity.kt`, `app/build.gradle.kts`.
UI is ~6.3k of the app's 9.5k Kotlin lines; `SessionLoggerScreen.kt` alone is 912. For pass 2,
the six reference images themselves and a written pattern catalogue of them.

### Five concrete faults I found while reading (these drive the "rewrite" calls)

1. **The app already looks like the family's *old* look, in orange.** Black canvas, `surfaceContainer`
   filled cards at five radii, `FilterChip` rows, `TagChip` pills, icon-led settings cards. Re-skinning
   that in a new accent would repeat food-tracker's mistake exactly: every screen below therefore
   states its visual delta in a **vs today** line, and the house rules (§1) ban the anatomy, not just
   the colours.
2. **Two container systems.** `Section` (bordered, no fill, 18dp) is used by Logger, Exercise detail and
   Analytics; `Card` with fills at 12/14/16/18/20dp by Plans, History, Session detail, Settings and Import.
   Six private stat displays (`HeaderStat`, `SummaryStat`, `MetaStat`, `MetricReadout`, `LabeledLine`,
   `StatTile`), two `FilterRow`s, two `StepperRow`s, plus unused `NumberStepper` and `WeeklyBarChart`.
3. **Half the colour roles are Material baseline defaults.** `tertiaryContainer`, `error`, `errorContainer`
   and `inverse*` are unset (`Color.kt`), so warm-up badges, failure badges, the rest-ready chip and every
   destructive button are drawn in baseline purple/red that belongs to no palette.
4. **The logger is a form, not an instrument.** Each set is two `OutlinedTextField`s and a `Checkbox` in a
   bordered box; the session clock and rest clock are 14sp chips; the number you need between sets
   (rest elapsed) is the smallest type on screen.
5. **State identity is decided in the VM, but the UI is decided by the route.** The logger's title comes
   from `planId` in the route; its content from whatever draft exists (`SessionLoggerViewModel.kt:96-109`).
   That is the root of issue 2 (wrong session opens silently) and must be resolved in UI, not papered over.

Also: hard constraints not met — `MaterialExpressiveTheme` wraps everything (`Theme.kt:12`), the nav
transitions are scale-fades (an Expressive register), and `ui-tooling-preview` is a dependency with zero
`@Preview` in the codebase.

### Inventory issues → where each is handled

| # | Issue (short) | Handled in |
|---|---|---|
| 1 | Destinations buried in overflow | §5 bottom bar (decision 1), §4.1 |
| 2 | Wrong session opens silently | §4.6 session guard, §6.1 |
| 3 | No post-finish summary | §4.5 summary (minimal feature) |
| 4 | Leaving logger keeps session, unannounced | §4.3 (↓ minimise icon), §4.1 live strip |
| 5 | Empty-state flash / blank loading | House rule 8, every §4 **Loading** line |
| 6 | Resume banner doesn't tick | §4.1 live strip ticks |
| 7 | Remove exercise immediate, no undo | §6.6 undo snackbar |
| 8 | Drag handle decorative | §4.2 handle-only drag |
| 9 | Plan notes have no UI | §4.2 note line (existing `updateNotes`) |
| 10 | Two target sheets, different caps | §4.7 one `TargetSheet` |
| 11 | Auto-tick; unticked sets look done later | §4.3, §4.5, §4.9 "skipped" rows (decision 8) |
| 12 | Weight column on cardio/mobility | §4.3 per-type columns |
| 13 | "−" removes only last set | §4.3 set menu "Delete set" |
| 14 | Rest timer fixed 90s, no alert | §4.4 rest target (minimal feature), background vibration (decision 5) |
| 15 | Future dates; UTC conversion | §4.3 date picker capped at today, local-date conversion |
| 16 | Hints ignore set type/completion | Non-UI list (§7) |
| 17 | Stale KDoc | Non-UI list |
| 18 | Plan name missing in History | §4.8, §4.9 |
| 19 | Exercise counts disagree | Non-UI list (one definition) |
| 20 | "kg·reps" even in lb | `formatVolume()` §2; definition in non-UI list |
| 21 | No session notes / edit | §4.5, §4.9 notes; editing out of scope (decision 3, §8) |
| 22 | No custom exercise CRUD | `CUSTOM` marker in phases 1–8; CRUD in phase 9 (decision 2), §4.19 |
| 23 | Raw enum names | `displayName` §2 |
| 24 | Synthetic 8–12 target; wrong units | Non-UI list; §4.11 units |
| 25 | Recent sets ungrouped | §4.11 grouped by session |
| 26 | Chart has no axes | §2 `LineChart` rewrite |
| 27 | "sessions" counts days | §4.12 label "training days" |
| 28 | Streak 0 before today's workout | Streak replaced by a weekly target (decision 7): §4.1, §4.12 |
| 29 | Calendar unlabeled | §2 `TrainingCalendar` |
| 30 | Chip rows unlabeled, write global pref | §4.12 local, labelled |
| 31 | Neglected ignores window | §4.12 follows range |
| 32 | "Session" means today | §4.12 renamed "Today" |
| 33 | Front/back silhouette identical | §2 `BodyDiagram` revise |
| 34 | KDoc 0.5 vs 0.3 | Non-UI list |
| 35 | Two import paths, Toast vs Snackbar | §4.15 Data page; snackbars only |
| 36 | Import copy wrong; Change resets match | §4.16 |
| 37 | Backup drops set type / pref; copy | Non-UI list |
| 38 | Backup dumbbell icon; RPE | §4.14 (no icons); RPE §8 |
| 39 | Dark-only with baseline roles | §1 every role set |
| 40 | Tokens adopted inconsistently | §2, house rule 5 |
| 41 | Unused components | §2 deletes, §7 phase 2 |
| 42 | Notification permission at launch | §6.1 step 3 (contextual) |

---

## 1. Design direction

### Thesis

**A black scoreboard.** Same family as food-tracker — `#000000` canvas, warm near-black ramp, warm
off-white text, hairlines not boxes, uppercase eyebrows over oversized tabular numerals, zero
elevation — but gym-tracker is used *during* effort: glanced at from a bench, typed into between
sets, with one sweaty thumb. So the numbers are bigger than food's, the touch targets are bigger
than food's, and the two clocks (session, rest) become the loudest things on the screen they live
on. The accent is a volt lime that means **work done**: a ticked set, a record, a filled day.
Targets you set yourself are amber, the family's goal colour. Nothing else is coloured.

Structure comes from the one real logger in the reference set (S2/S3): clock and Finish on top, a
set table in the middle, rest at the thumb. Voice comes from S4: eyebrow, numeral, rule.

What changes visually versus today, in one list: no filled cards anywhere on a page; no chips as
tags or as selectors; no icon discs; no orange; set rows become 28sp numerals on rules instead of
outlined text boxes, and a done set lights its whole row; every section gets a standing head and a
hairline; every screen gets one number that is 40–64sp.

### Colour roles — one scheme

Neutrals are food-tracker's, verbatim (family resemblance). Accent and its derivatives are new.

| Role | Value | Note |
|---|---|---|
| `background` / `surface` / `surfaceDim` / `surfaceContainerLowest` / `scrim` | `#000000` | OLED true black |
| `surfaceContainerLow` | `#070707` | |
| `surfaceContainer` | `#0C0C0B` | sheets, menus, dialogs only |
| `surfaceContainerHigh` / `surfaceVariant` | `#141312` | snackbar, pressed rows |
| `surfaceContainerHighest` / `surfaceBright` | `#1C1A18` | focused input underlay |
| `onSurface` / `onBackground` | `#EFE9E0` | warm off-white, not `#FFF` |
| `onSurfaceVariant` | `#A79E92` | labels, units, pending hints |
| `outline` | `#36312B` | ticks, stronger rules |
| `outlineVariant` | `#201D1A` | hairlines, meter tracks |
| `primary` (**Volt**) | `#C5EA6A` | work done, records, primary action |
| `onPrimary` | `#1B2600` | |
| `primaryContainer` | `#26330C` | badge fill only |
| `onPrimaryContainer` | `#E2F7B0` | |
| `inversePrimary` (deep volt) | `#5E7A1C` | heat-ramp midpoint |
| `tertiary` (**Target**, shared with food) | `#E0A961` | rep ranges, rest target, goal lines |
| `tertiaryContainer` / `onTertiaryContainer` | `#2D2310` / `#F3DBA5` | |
| `error` / `onError` | `#FF6B5E` / `#3A0A06` | destructive only |
| `errorContainer` / `onErrorContainer` | `#4A120C` / `#FFD9D4` | |
| `Effort` (custom token, = food's `StatusOver`) | `#F08A4B` | failure sets, negative deltas |
| `DoneRow` (custom token) | `#101309` | completed set row tint = primary at 8% on black (S2-b) |

Why volt: it is the one hue with high luminance on black that neither food (mint) nor the
family's warning hues (amber, orange, red) occupy, and it reads as "go" in a gym register without
being a traffic-light green. The orange launcher/brand (`#8E5226`) is retired (decision 6).

**Rule governing the containers:** the `surfaceContainer*` ramp exists only for modal surfaces and
pressed/focused states. Nothing on a page fills.

**Data palette.** Gym has one quantity family (training volume), so one ramp, not a hue per concept:

```
heat 0     outlineVariant  #201D1A   (no volume)
heat 0.25  #3A4520
heat 0.5   inversePrimary  #5E7A1C
heat 0.75  #91B23F
heat 1     primary         #C5EA6A
set types  warm-up = onSurfaceVariant · working = onSurface · failure = Effort #F08A4B
```

### Type scale

Food's `AppText` scale verbatim (system font, `tnum` on every numeric style), plus one gym token.

| Token | Size/line | Weight | Use |
|---|---|---|---|
| `numberXXL` | 64/60, −2sp, tnum | SemiBold | a screen's one hero (rest clock, summary duration, est. 1RM) |
| `numberXL` | 40/44, tnum | SemiBold | section heroes (week count) |
| `numberL` | 28/32, tnum | SemiBold | **set-row values**, stat rows |
| `numberM` | 18/24, tnum | SemiBold | dense grids, stat columns |
| `titleL` | 20/28 | SemiBold | screen/sheet titles |
| `titleM` | 16/22 | SemiBold | exercise and plan names |
| `body` | 15/22 | Normal | |
| `bodyS` | 13/18, tnum | Normal | units, captions, axes |
| `label` | 13/18 | Medium | buttons, selectors |
| `eyebrow` | 11/16, +1.2sp | Medium, UPPERCASE (by the component) | section heads, column heads |

Set-row values at `numberL` 28 are the deliberate break from food (which uses 28 only for row
values in stat rows): a weight read from 60cm away must be legible at a glance.

### Spacing scale

`4 · 8 · 12 · 16 · 20 · 24 · 32` via `Spacing.*`. Gutter 16, 24 between sections. Raw `.dp` only for
sizes (icon, chart, row min-heights), never for gaps. Today's literals 6, 10, 14, 18, 64, 96 go.

### Radii

```
Radius.sm   = 8dp    badges, status cells, set badge
Radius.md   = 14dp   input underlays, snackbar, menus
Radius.lg   = 20dp   genuine cards only (expected: none on pages)
Radius.sheet= 24dp   sheet top corners
Radius.pill = 999    FAB and primary buttons only
```
Today: 4, 8, 9, 10, 12, 14, 16, 18, 20, circle.

### Elevation & border

Zero elevation in every state (default/pressed/focused/hovered) for FAB, buttons, cards, top bar,
logger dock. Structure is a 1dp `outlineVariant` rule. Fills allowed: sheets/dialogs/menus
(`surfaceContainer`), primary button + FAB (`primary`), the rest transport's Skip button
(`primary`), snackbar (`surfaceContainerHigh`, `Radius.md` — overriding Material's cream inverse
slab), the `PR` badge and tooltip tags (`primaryContainer`), and **completed set rows** (`DoneRow`
`#101309` = primary at 8% on black, full-bleed; S2-b).

### Motion

| Event | Spec |
|---|---|
| generic state | 200ms `FastOutSlowIn` |
| numbers, meters | 450ms value-interpolated (tnum prevents jitter) |
| set completion | row tint fades in 150ms; numerals tween `onSurfaceVariant`→`onSurface` 200ms; `toggle` haptic |
| exercise complete | 600ms after its last set is ticked, the block collapses (`animateContentSize`, 200ms); the next block scrolls to the top of the list |
| new PR | `PR` badge fades+scales 0.9→1 over 200ms (no overshoot); `confirm` haptic (existing) |
| rest reaches target | meter fill switches tertiary→primary, one `confirm` haptic, no sound |
| nav push/pop | food's 300ms slide + `predictivePop*` pair, replacing today's 0.85 scale-fade (family consistency; scale-in is the Expressive feel) |
| bottom-bar switch | fade-through: 90ms fadeOut → 210ms fadeIn; peers don't slide |
| period pager | content crossfades 200ms; no horizontal slide |
| sheets | Material default; `skipPartiallyExpanded = true` on any sheet taller than half |
| banned | spring overshoot, shape-morph, shared-element, `MaterialExpressiveTheme` motion scheme |

### House rules (adopted up front — food learned these after shipping)

1. **A standing head with a rule under it — not a box, not a gap.** `Section(title, trailing)`; trailing is the section's figure, never an action. Actions go in a foot.
2. **The number is the loudest thing.** Label left, number right; eyebrow labels; units `bodyS` trailing.
3. **The accent is for work done, not chrome.** Selection is a 24×2dp `onSurface` rule under a full-strength label. No filled chips. **One gym exception, from S2-b:** a completed set row carries a full-bleed `DoneRow` tint. Food bans tinted "now" rows because they mark *time*; this marks *work done*, which is exactly what the accent means, and it is the only signal legible from a bench at arm's length. Nothing else is tinted.
4. **No emoji, no decorative icons.** Icons only where they are the control (back, overflow, drag handle, check, filter).
5. **Spacing from the scale only.**
6. **Geometry:** sm/md for corners, pill for FAB and primary button, zero elevation.
7. **Size tokens are minimums** (`heightIn(min=)`); test every screen at font scale 0.85 / 1.0 / 1.15; equal-width columns measure their widest label and wrap.
8. **No empty state before data.** VM flows start at `null` (Loading); render top bar + nothing until the first Room emission; the empty state only when the emission is empty.
9. **Names, not enums; your unit, always.** Every enum goes through `displayName`; every weight and volume through `formatWeight`/`formatVolume` in the preferred unit.
10. **Single-item removal is undoable; whole-object deletion is confirmed.** Set/exercise remove → undo snackbar. Plan/session delete/discard → `ConfirmSheet`.
11. **Thumb zone for the logger.** The actions used *every set* (tick, rest) sit in the bottom 40% of the screen; once-a-session actions (Finish, reorder) may sit at the top.
12. **No sheet raises a sheet.** A drill-down from a sheet becomes a page or an in-sheet state.

**Reference implementation:** the Session logger (§4.3). It is built first after the atoms (§7 phase 3) and every later screen is checked against it.

---

## 2. Atoms

New package `ui/components/atoms/`.

| Atom | Verdict | Reason |
|---|---|---|
| `Section` | **rewrite** | Today a bordered box (18dp, 14/12 padding). Becomes the standing head: eyebrow + trailing figure · 8 · rule · 16 · content. No border, no fill. |
| `AppDivider` | **new** | 1dp `outlineVariant`, full width. |
| `AppBottomBar` | **new** (decision 1) | Food's bar verbatim: hairline top, background container, eyebrow labels, 3 items. Shown only on bar routes. |
| `AppTopBar` | **new** | `TopAppBar`, container = background (also when scrolled), `titleL`, optional `bodyS` subtitle. Replaces 10 inline `TopAppBar`s. |
| `AppButton` / `AppOutlinedButton` / `AppTextButton` | **new** | Pill, 0 elevation all states, `heightIn(min = 48dp)`, `label`. Replaces `FilledTonalButton`s (no tonal fills in the family). |
| `AppFab` | **new** | `ExtendedFloatingActionButton`, primary/onPrimary, pill, elevation 0 ×4 states. |
| `AppSnackbarHost` | **new** | `surfaceContainerHigh`, `Radius.md`, action in primary. Replaces the Toast in plan export. |
| `Spinner` | **new** | 24dp, 2dp stroke, primary on outlineVariant. |
| `ValueText` + `Format.kt` | **new** | Number + NBSP + unit (`bodyS`, onSurfaceVariant). `grouped()` (`12 400`), `formatWeight(kg, unit)`, `formatVolume(kg, unit)` → `12 400 kg` / `27 340 lb` (fixes "kg·reps"), `formatDuration`, em dash for missing. |
| `displayName` (Muscle, Equipment, MovementType, SetType) | **new** | `FrontDelts` → "Front delts", `UpperBack` → "Upper back". Fixes issue 23 everywhere at once. |
| `StatRow` | **new** | Eyebrow label + caption left, `numberL` + unit right; optional chevron; clickable. Replaces `StatTile`, `LabeledLine`, `MetricReadout`. |
| `StatColumns` | **new** | 2–3 equal columns of eyebrow over `numberM`/`numberL`; measures widest label, wraps at large font scale. Replaces `HeaderStat`, `SummaryStat`, `MetaStat`. |
| `Meter` | **new** | Food's: 2–3dp, rounded, track outlineVariant, optional target tick in `tertiary`. |
| `StatusCell` | **new** | 10×10dp, 3dp corner; heat-ramp colour. Unit of the calendar and the week strip. |
| `UnderlineSelector` | **rewrite** of `ExclusiveButtonGroup` | Labels in a row; selected = onSurface + 24×2dp rule; unselected onSurfaceVariant; haptic tick kept; re-tap no-op. Kills `FilterChip` selection. |
| `NumberField` | **new** | Borderless numeral input: `numberL` text, 1dp rule beneath (focused: 2dp primary), hint in `onSurfaceVariant` at same size, `imeAction = Next`. Replaces the `OutlinedTextField`s in `SetRow`. |
| `Stepper` | **new** | `[−] value [+]`, 48dp outlined circular buttons, value `numberL`. Replaces both private `StepperRow`s. |
| `NumberStepper` | **delete** | Unused (inventory §4). |
| `WeeklyBarChart` | **delete** | Unused. A new `BarChart` is built for §4.12 from food's stacked-bar styling, not revived from this file. |
| `TagChip` | **delete** | Tags become text: `CHEST · BACK · ARMS` in eyebrow, onSurfaceVariant. |
| `StatTile` | **delete** | → `StatRow`. |
| `EmptyState` | **revise** | Drop the 40dp icon; `titleM` line + `bodyS` line + optional `AppTextButton`. Shown only after a real empty emission (rule 8). |
| `LineChart` | **rewrite** | Food's look: 180dp plot + 22dp axis, 4 grid lines outline@40%, 3dp primary stroke, 12%→0 wash, 3dp dots; y labels (min/max value in unit), x labels (first/mid/last date) in `bodyS`; target band in `tertiary`@12% with caption above the band if room, else below, clamped inside the plot (food lesson 4). Last point ringed with its value label (S2-a). |
| `BarChart` | **new** | 150dp + 18dp axis, bar corner `min(w/2, 4dp)`. Bars `inversePrimary` with a 2dp `primary` top cap (S4-b); dashed `tertiary` reference line with caption (same collision rule as `LineChart`); selected bar gets a `TooltipTag`; current week outline-only until it ends. |
| `TrainingCalendar` | **rewrite** of `ConsistencyCalendar` | 7 columns M–S (Sunday included), `StatusCell`s, eyebrow day initials on top, month initials on the left at month starts, future days not drawn. |
| `BodyDiagram` | **revise** | Keep hit-testing and regions. Fill = heat ramp; empty = outlineVariant; outline = outline; selected = onSurface 1.5dp. Back silhouette gets its own path (issue 33). |
| `ConfirmSheet` | **new** | Title, one-line consequence, destructive `AppButton` in `error`, `AppTextButton` cancel. Replaces 5 hand-built confirm sheets. |
| `Haptics` | **keep** | Untouched; gating by pref stays. |
| `Sparkline` | **new** (S2-a) | 96×28dp, 2dp primary stroke, last point a 3dp dot, no axes, no wash. Plan rows only. |
| `TooltipTag` | **new** (S4-b, S6-b) | `primaryContainer` fill, `Radius.sm`, `label` in onPrimaryContainer, 4dp pointer; sits above a selected bar/point, clamped inside the plot. |
| `PeriodPager` | **new** (S6-b) | `‹  4 – 10 Nov  ›`: 48dp chevron targets, `label` centred, `›` disabled at the current period. |
| `RestTransport` | **new** (S5-g) | `−15 s` text button · 56dp `Radius.md` primary-filled `Skip` · `+15 s` text button. |

## 3. Molecules

| Molecule | Verdict | Reason |
|---|---|---|
| `LiveStrip` (was Resume banner) | **rewrite** | No `primaryContainer` card. `LIVE · PUSH A` eyebrow in primary, ticking elapsed (1s), sets done and rest state, then a full-width `Resume` `AppButton` (S5-d). |
| `WeekHero` | **new** | Train hero: sessions this week vs the weekly target (`numberXL` + `of 4`), 7 `StatusCell`s Mon–Sun, 2dp `Meter` to target, three `StatColumns` (weeks on target of last 12, volume, sets). Decision 7. |
| `PlanRow` | **rewrite** | No card, tile, chips or trash. Name `titleM` + trailing `Start` text button in primary; `Last · 3 days ago` + `Sparkline` (S2-a); `6 exercises · Chest, Shoulders` `bodyS`. |
| `SetRow` | **rewrite** | See §4.3. Badge (set number / W / F) · weight `NumberField` · reps `NumberField` · 48dp check. Columns per movement type. PR → numeral in primary + `PR` badge. |
| `ExerciseBlock` | **rewrite** (was a `Section` card) | Head: name `titleM` + `2/3` trailing; target eyebrow in tertiary; ⋮ menu (Edit target, Progress, Skip / Unskip exercise, Remove). Column eyebrows once; set rows divided by hairlines, done rows tinted; foot `+ Add set`. Collapsed state (S2-b): one 48dp line `✓ name  3/3  ⌄` (`Skipped` in muted text instead of `✓` for a skipped exercise). |
| `LoggerDock` | **new** | Bottom bar: `REST` / `TARGET` eyebrows, `numberXL` clock, 2dp meter, `RestTransport` (S5-g). One-line compact state while the IME is open. Replaces the chips row and the Add/Finish bar. |
| `LoggerTopBar` | **new** (S2-b) | ↓ · centred `numberL` elapsed over two rows — the workout title (one line, ellipsised) and a muted meta line `3 of 12 sets · Today` (a past date in tertiary) · outlined pill: `Discard` (error) until a set is ticked, then `Finish` · ⋮. |
| `PlannedRow` | **rewrite** | Index `numberM`, name, `Chest · Barbell`, target in tertiary, real drag handle. |
| `SessionRow` | **rewrite** | Date column (eyebrow weekday over `numberM` day) · plan name + meta · trailing sets `numberL`. |
| `RecordRow` | **new** | Exercise · record kind · value · delta in primary. Summary and Exercise detail. |
| `MuscleRankRow` | **new** | Muscle name · 2dp meter (heat) · value. Progress screen. |
| `ExerciseListRow` | **rewrite** | No card, no chip. Name `titleM`, `Barbell · Chest, Triceps` `bodyS`, trailing eyebrow `COMPOUND`; `CUSTOM` eyebrow in tertiary for custom exercises. |
| `FilterSheet` + `ActiveFilterLine` | **new** (S3-b) | Search field's trailing filter button (with count) opens a sheet of three text-option sections; the active filters show as one `bodyS` line + `Clear` under the search. Replaces both `FilterRow`s (3 scrolling chip rows, ~150dp → one optional 24dp line). Only ever opened from a page. |
| `SettingsRow` | **new** | Label / value / chevron or trailing control. Replaces `SettingsCard` (icon + card). |
| `ImportEntryRow` | **rewrite** | Source name + `3 × 8–12`; resolution as a text line (`→ Bench press` primary, `New: Name` tertiary, `Skipped` onSurfaceVariant, `Needs review` error); actions as text buttons. |
| `ExercisePicker` (was `ExercisePickerSheet`) | **rewrite** | Full page, multi-select with sticky CTA (S3-b); single-select mode for Import. §4.13. |
| `HeaderStat`, `SummaryStat`, `MetaStat`, `MetricReadout`, `LabeledLine`, `SettingsCard`, both `FilterRow`, both `StepperRow`, `ChipRow` | **delete** | Folded into the atoms above. |

---

## 4. Organisms & screen layouts

Viewport assumed: 412×915dp, ~690dp between top bar and nav-bar inset. Wireframes are ~46 columns.

### 4.1 Train (bottom destination 1, start; was "Plans") — **rewrite**

```
┌────────────────────────────────────────────┐
│ Train                                   ⚙  │  56dp AppTopBar; ⚙ → Settings
├────────────────────────────────────────────┤
│ LIVE · PUSH A                     23:41    │  eyebrow primary; numberM ticks 1s
│ 7 of 18 sets · resting 1:12                │  bodyS
│ ╭──────────────────────────────────────╮   │  S5-d: full-width AppButton
│ │             Resume  →                │   │  only with a draft
│ ╰──────────────────────────────────────╯   │
├────────────────────────────────────────────┤
│ THIS WEEK                     TARGET 4     │  WeekHero (decision 7)
│ 3 of 4                   ▪ ▪ ▫ ▪ ▫ ▫ ▫     │  numberXL "3" + bodyS "of 4"; cells
│ sessions                 M T W T F S S     │  today's label in primary
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━──────────   │  2dp Meter to target; primary at 4/4
│                                            │
│ ON TARGET   VOLUME          SETS           │  StatColumns, eyebrow / numberM
│ 9 of 12 wk  12 400 kg       54             │  weeks that met target, last 12
│                                            │
│ PLANS                            4 plans   │  Section head + trailing
│ ────────────────────────────────────────── │
│ Push A                          Start      │  titleM; Start = primary text btn
│ Last · 3 days ago            ▁▂▂▄▃▅▆       │  S2-a: Sparkline, volume, last 8
│ 6 exercises · ~55 min                      │  bodyS; typical duration (decision 4)
│ ────────────────────────────────────────── │
│ Pull B                          Start      │  row tap → Plan editor
│ Last · yesterday             ▃▃▄▄▅▅▆       │
│ 5 exercises · ~48 min                      │
│ ────────────────────────────────────────── │
│ + New plan                                 │  foot → Create sheet   ← fold
│                         ╭─────────────╮    │
│                         │ + Quick log │    │  AppFab, hidden while LIVE
│                         ╰─────────────╯    │
├────────────────────────────────────────────┤
│   ●Train        ○History      ○Progress    │  80dp bottom bar (decision 1)
└────────────────────────────────────────────┘
```

**Scroll:** one `LazyColumn`; top bar and bottom bar fixed; FAB bottom-end above the bar, not hiding on scroll. Bottom content padding 96dp (FAB) — the bar's own height is handled by the NavHost inset rule (§5), not added here.
**Above the fold:** live session (if any), this week against its target, and the first two or three plans with a one-tap Start.
**Empty (no plans):** WeekHero still renders (`0 of 4`, empty cells, empty meter, `ON TARGET —` until a full week has passed); PLANS shows `EmptyState` "No plans yet" / "Build one, or start a quick log." + `New plan`. A plan with fewer than 2 sessions shows no sparkline; a plan with no timed sessions omits `~min`.
**Loading:** top bar and bar only until the first emission (rule 8) — fixes the "No plans yet" flash.

Changes, each justified:
1. **Overflow menu and the `+`/bar-chart icons go.** History and Progress become bottom-bar destinations (decision 1); Exercises is reached from Progress; Settings is the ⚙ icon here; Import moves to the Plans & backup page (§4.15).
2. **Delete leaves the home card.** A trash icon next to Start is one mis-tap from losing a plan. Delete moves to the Plan editor's ⋮ (still confirmed). Flow change; one extra tap for a rare, destructive action.
3. **A week hero against a weekly target replaces the streak** (decision 7) — "3 of 4 sessions" answers "am I training enough" without punishing rest days the way a daily streak did (issue 28).
4. **Each plan shows its own trend** (S2-a) and its typical duration (decision 4) — "is this plan still moving me forward, and how long will it take" without opening it. One per-plan volume query, no extra schema beyond decision 4's column.

**vs today:** today is an orange-tinted card stack (20dp `surfaceContainer` cards, 44dp dumbbell tiles, chip rows, tonal Log buttons) under a top bar of three unlabeled icons, with a `primaryContainer` banner whose minutes never tick and no navigation bar at all. New: no cards, no tiles, no chips; a 40sp "3 of 4" with a volt meter; a live clock and a full-width Resume; plans are text on hairlines, each with a volt sparkline; a three-item bottom bar.

---

### 4.2 Plan editor (full page, from a plan row / after create or import) — **rewrite**

```
┌────────────────────────────────────────────┐
│ ←                                    ⋮     │  ⋮: Rename, Export, Delete plan
│ Push A                                     │  titleL; tap → Rename sheet
│ 6 exercises · 18 sets · ~55 min            │  median of timed sessions (decision 4)
│ Heavy day. 2 min rests on compounds.       │  plan note, tap → edit (new UI)
├────────────────────────────────────────────┤
│ EXERCISES                        18 sets   │  Section
│ ────────────────────────────────────────── │
│ 1   Bench press         3 × 8–12     ≡     │  index numberM; target tertiary
│     Chest · Barbell                        │  ≡ = drag handle (only drag zone)
│ ────────────────────────────────────────── │
│ 2   Overhead press      3 × 6–8      ≡     │  tap row → Target sheet §4.7
│     Front delts · Barbell                  │
│ ────────────────────────────────────────── │
│ 3   Incline DB press    3 × 10–12    ≡     │
│     Chest · Dumbbell                       │
│ ────────────────────────────────────────── │
│ + Add exercise                             │  foot → Exercise picker
│                                            │
│ ╭──────────────────────────────────────╮   │  pinned bottom, 48dp min
│ │           ▶  Start workout           │   │  AppButton (primary)
│ ╰──────────────────────────────────────╯   │
└────────────────────────────────────────────┘
```

**Scroll:** list scrolls; Start pinned above the nav inset. **Above the fold:** name, note, first ~6 exercises.
**Empty:** "No exercises yet" / "Add the first one." + `Add exercise`; Start disabled with caption "Add an exercise to start".
**Loading:** top bar with no title until data (no "Plan" placeholder, no empty-state flash).

Changes: drag only from the handle (issue 8; long-press anywhere competed with tap); remove moves into the Target sheet with undo (issue 7); per-row ↗ and trash icons gone (↗ is "View progress" in the sheet); Start becomes a full-width button instead of a 40dp circle in the top bar; plan note becomes visible/editable (issue 9; `PlanEditorViewModel.updateNotes` already exists). Export Toast → snackbar.

**vs today:** today each exercise is a filled 16dp card with a tinted index disc, raw enum muscles in orange, and three trailing icons. New: one text line + one caption per exercise, the target in amber, a single handle, and a pinned primary Start.

---

### 4.3 Session logger (full page) — **rewrite; reference implementation**

Laid out after S2-b, built in the family voice.

```
┌────────────────────────────────────────────┐
│ ↓            23:41        ╭────────╮  ⋮    │  ↓ minimise · elapsed numberL centred
│              Push A       │ Finish │       │  title, one line · outlined pill (S2-b)
│     7 of 18 sets · Today  ╰────────╯       │  muted meta; ⋮: Change date, Reorder, Discard
│ ━━━━━━━━━━━━━━━━────────────────────────   │  2dp Meter: sets done / total
├────────────────────────────────────────────┤
│ ✓ Warm-up rower              1/1     ⌄     │  collapsed: done exercise, 48dp
├────────────────────────────────────────────┤
│ Bench press                    2/3    ⋮    │  ExerciseBlock head, titleM
│ TARGET 3 × 8–12 REPS                       │  eyebrow tertiary; tap → Target
│ ────────────────────────────────────────── │
│ SET      KG            REPS        ✓       │  eyebrow column heads, once
│▓1        60.0          10          ✓      ▓│  DoneRow tint full-bleed, ✓ primary
│▓2        62.5          9  PR       ✓      ▓│  PR: value primary + PR badge
│ 3        62.5          8           ○       │  pending: hint numerals, muted
│ ────────────────────────────────────────── │
│ + Add set                                  │  foot
├────────────────────────────────────────────┤
│ Treadmill                      0/1    ⋮    │  cardio block
│ TARGET 20–30 MIN                           │
│ ────────────────────────────────────────── │
│ SET      MIN                       ✓       │  no weight column (issue 12)
│ 1        25                        ○       │
│ ────────────────────────────────────────── │
│ + Add exercises                            │  end-of-list foot → picker page
├────────────────────────────────────────────┤
│ REST                        TARGET 1:30    │  LoggerDock, above IME/nav
│ 1:12                                       │  numberXL, tap → Rest sheet
│ ━━━━━━━━━━━━━━━━━━━━━━━──────────────      │  Meter → target (S5-g thin bar)
│  − 15 s       ╭────────╮       + 15 s      │  RestTransport
│               │  Skip  │                   │  56dp primary
│               ╰────────╯                   │
└────────────────────────────────────────────┘
```

Set row anatomy: 64dp min height. Badge 40dp (number, `W` muted, `F` in Effort) → tap opens a menu
**Warm-up / Working / Failure / — / Delete set** (issue 13). `NumberField`s for weight and reps (weight
first: "62.5 × 9" is how lifters say it). Check is a 48dp target; tapping it on an empty row commits the
hint values (as today's save already does). Typing still auto-completes the set (kept: it is the fast
path), and the row's tint and brightened numerals make the auto-tick visible instead of silent —
except the set that would complete the exercise (the only one left unticked): typing never ticks it,
its check pulses in primary (~2s) on the first input instead, and the block folds only once the user
ticks it.

The dock collapses to a single 56dp line (`REST 1:12 / 1:30` + meter) while the IME is open, so the
focused row is never hidden; it expands again when the keyboard closes.

**Scroll:** one `LazyColumn`; top bar and dock fixed. Collapsed (finished) exercises stay in place, so
order is preserved and a tap re-expands one. `imePadding` on the dock only — one inset consumer.
**Above the fold:** clock, Finish, progress meter, the current exercise's set table, and the rest
transport — every per-set action in the bottom 40% (rule 11).
**Empty (quick log, no exercises):** "Add your first exercise" + `AppButton Add exercises` centred; dock shows REST `—` and transport disabled; the top-bar pill reads Discard.
**Loading:** while the draft hydrates, top bar + dock only (today shows "Tap Add exercise" briefly).

Changes:
1. **The chip row goes.** Date → top-bar ⋮ **Change date** (picker capped at today, converted with the
   local zone — issue 15), beside **Reorder exercises** and **Discard workout** (in ⋮ once a set is ticked; before that Discard
   is the top-bar pill). Elapsed → the centred clock. `x/y done` → the meta row and a 2dp meter. Rest → the dock.
2. **Finish moves to the top bar** (S2-b). It is used once per session; at the top it is out of the
   way of the thumb that is ticking sets, and the dock is freed for rest.
3. **Rest becomes the dock** with a target and an S5-g transport (minimal feature, §4.4, §6.3).
4. **Done rows light up and done exercises collapse** (S2-b) — "where am I" is answered by the first
   un-tinted row under the first expanded block.
5. **Add exercise moves to the end of the list** and opens the multi-select picker (§4.13).
6. **Back is a ↓ minimise icon**, not ←, because leaving does not end the session (issue 4). First minimise of a session shows a snackbar on Train: "Push A is still running · Resume".
7. **Remove exercise** goes into the block's ⋮ with an undo snackbar above the dock (issue 7).
8. **Title is the draft's, not the route's** (fault 5): the title row always reads the draft's plan name. The guard (§4.6) stops the mismatch arising at all.

Reorder mode keeps its behaviour (entered from the top-bar ⋮); compact rows adopt `PlannedRow` styling with the handle.

**vs today:** today the top third is a row of tonal chips with 14sp clocks, exercises are bordered 18dp boxes, each set is two outlined text boxes and a Material checkbox, and Finish shares a bottom bar with Add exercise. New: a centred 28sp clock with Finish beside it, hairline blocks of 28sp numerals on rules, done rows lit volt from edge to edge, finished exercises folded to one line, an amber target eyebrow, and a bottom dock whose 40sp rest clock and Skip button are the biggest things under the thumb.

---

### 4.4 Rest sheet (modal, from the dock's clock) — **new**

The dock already carries the S5-g transport; the sheet is for changing the *target*.

```
╭────────────────────────────────────────────╮
│                 ───                        │
│ REST                                       │  eyebrow
│ 1:12                         of 1:30       │  numberXXL, ticks; target tertiary
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━────────────   │  Meter; turns primary at target
│                                            │
│ TARGET                                     │
│   ( − 15 s )        1:30      ( + 15 s )   │  Stepper, 15 s steps, 0:15–10:00
│   1:00     1:30     2:00     3:00          │  UnderlineSelector presets
│                                            │
│ ☐ Use as default for new workouts          │  writes pref
├ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─┤
│ Restart now                       Done     │  text btn · AppButton
╰────────────────────────────────────────────╯
```

**Empty/Loading:** none — opens with live values. Before the first completed set the hero reads `—` and "Starts when you tick a set".
With the app in the background the target is marked by one vibration from the Rest timer notification channel (decision 5, §4.18).
**vs today:** today rest is a 14sp chip that turns tertiary at a hard-coded 90s and has no other UI.

---

### 4.5 Finish sheet + Session summary — **revise + new**

Finish sheet (modal, from the top bar's `Finish`) — merges today's two sheets ("Save changes to plan?" and "Finish this session?") into one:

```
╭────────────────────────────────────────────╮
│                 ───                        │
│ Finish Push A?                             │  titleL
│ 41:20 · 16 of 18 sets · 2 PRs              │  bodyS
│ 2 unticked sets will be saved as skipped   │  only if any (decision 8)
│ ────────────────────────────────────────── │
│ ☐ Update Push A to match this session      │  only if structure changed
│   Bench 3→4 sets · added Dips              │  bodyS diff
├ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─┤
│ Keep logging              ( Finish )       │  text · AppButton
╰────────────────────────────────────────────╯
```

One decision instead of two sequential sheets; the plan-sync choice becomes a checkbox with a stated diff (today: three buttons with no diff). Default unchecked (same as "Just log").

Session summary (full page, pushed after commit with `popUpTo(SessionLoggerRoute){inclusive=true}`):

```
┌────────────────────────────────────────────┐
│                                       ✕    │  ✕ / back = Done
│ WORKOUT COMPLETE                           │  eyebrow primary
│ Push A                                     │  titleL
│ Tue 23 Nov                                 │  bodyS
│                                            │
│ 41:20                                      │  numberXXL duration
│ DURATION                                   │
│                                            │
│ SETS        VOLUME          EXERCISES      │  StatColumns numberL
│ 16          8 420 kg        5              │
├────────────────────────────────────────────┤
│ RECORDS                            2 PRs   │  Section
│ ────────────────────────────────────────── │
│ Bench press                    82.5 kg     │  RecordRow; value primary
│ Est. 1RM                          +2.5     │
│ ────────────────────────────────────────── │
│ Dips                             14 reps   │
│ Max reps                            +2     │
├────────────────────────────────────────────┤
│ EXERCISES                                  │
│ ────────────────────────────────────────── │
│ Bench press            3 sets · 62.5×9     │  best set
│ Overhead press         3 sets · 40×8       │
├────────────────────────────────────────────┤
│ NOTE                                       │  writes SessionEntity.notes
│ Add a note…                                │  inline field
│                                            │
│ ╭──────────────────────────────────────╮   │
│ │                 Done                 │   │  → back to where logger started
│ ╰──────────────────────────────────────╯   │
└────────────────────────────────────────────┘
```

Duration is `loggedAtMillis − startedAtMillis` from the saved `SessionEntity` (decision 4: commit writes the draft's start time), so the route is just `SessionSummaryRoute(sessionId)`. Skipped sets are listed under each exercise as "1 skipped" (decision 8). PRs come from the logger's existing baseline snapshot. **Empty records:** section reads "No records this time" in `bodyS`. **Loading:** none (data in hand).
**vs today:** today Finish pops straight back with a haptic; there is no summary at all.

---

### 4.6 Session guard (modal, when starting while a draft exists) — **new**

```
╭────────────────────────────────────────────╮
│                 ───                        │
│ A workout is already running               │  titleL
│ Push A · 23 min · 7 sets                   │  bodyS
├ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─┤
│ ╭──────────────────────────────────────╮   │
│ │           Resume Push A              │   │  AppButton
│ ╰──────────────────────────────────────╯   │
│ Discard it and start Pull B                │  text btn, error; 2nd state:
│                                            │  "Discard 7 sets?" [Discard & start]
╰────────────────────────────────────────────╯
```

Triggered by Start on any plan, Start workout in the editor, or Quick log while a draft exists. The
sheet's second state replaces a sheet-over-sheet confirm (no back convention for stacked sheets).
**vs today:** today the existing draft opens silently under the new route's title (issue 2).

---

### 4.7 Target sheet (modal; Plan editor row tap, logger ⋮ Edit target) — **rewrite, one component**

```
╭────────────────────────────────────────────╮
│                 ───                        │
│ Bench press                                │  titleL
│ 3 × 8–12 reps                              │  numberL tertiary, live preview
│ ────────────────────────────────────────── │
│ SETS                 ( − )  3  ( + )       │  Stepper; hidden in logger
│ FROM                 ( − )  8  ( + )       │  unit-aware label (reps/min/sec)
│ TO                   ( − ) 12  ( + )       │  min ≤ max kept consistent
├ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─┤
│ View progress →      Remove from plan      │  text btns; remove = error, undo
│ ╭──────────────────────────────────────╮   │
│ │                Apply                 │   │
│ ╰──────────────────────────────────────╯   │
╰────────────────────────────────────────────╯
```

Same caps in both callers (sets 1–10; 50 reps / 120 min / 180 sec) — issue 10. The live preview the logger KDoc promised now exists.
**vs today:** two differently-styled sheets with private steppers and a tonal Apply.

---

### 4.8 History (bottom destination 2) — **rewrite**

```
┌────────────────────────────────────────────┐
│ History                                    │  top-level: no back arrow
├────────────────────────────────────────────┤
│ THIS WEEK                     3 sessions   │  Section per ISO week, trailing count
│ ────────────────────────────────────────── │
│ TODAY                                      │  S4-b day eyebrow
│ Push A                             16      │  titleM · trailing numberL
│ 5 exercises · 41 min · 8 420 kg   sets     │  bodyS; duration (decision 4)
│ ────────────────────────────────────────── │
│ SUNDAY 21                                  │
│ Quick log                           9      │  no plan → "Quick log"
│ 3 exercises · 22 min · 2 100 kg   sets     │
├────────────────────────────────────────────┤
│ LAST WEEK                     4 sessions   │
│ ────────────────────────────────────────── │
│ FRIDAY 19                                  │
│ Pull B                             18      │
│ 5 exercises · 52 min · 9 860 kg   sets     │
└────────────────────────────────────────────┘
```

Grouped by ISO week (was by day, which on a one-session-a-day app is one header per card). Title = plan name (issue 18), "Quick log" for ad hoc (the "Freestyle" chip goes). Volume in the user's unit. Muscle-group tags dropped from the row (they are on the detail page) — at larger font scales they wrapped to a fourth line. Sessions logged before the v5 migration have no start time and omit the duration.
**Empty:** "Nothing logged yet" / "Finished workouts land here." **Loading:** top bar only.
**vs today:** reached only through a three-dot menu; filled 18dp cards titled by "top exercise +N more", icon-led meta and chip rows, a header per day, no durations.

---

### 4.9 Session detail (full page, from History) — **rewrite**

```
┌────────────────────────────────────────────┐
│ ←  Push A                            ⋮     │  ⋮: Delete session (ConfirmSheet)
│    Tue 23 Nov 2026                         │
├────────────────────────────────────────────┤
│ DURATION  SETS  VOLUME      PRS            │  S4-c stat row, numberM
│ 41:20     16    8 420 kg    2              │  PRS primary when > 0; — if untimed
│ Volume = weight × reps, warm-ups out.      │  bodyS
│ NOTE  Felt strong. Shoulder fine.          │  tap → edit note
├────────────────────────────────────────────┤
│ Bench press                       3 sets   │  head → Exercise detail
│ 3 × 8–12 REPS                              │  eyebrow, the target used
│ ────────────────────────────────────────── │
│ W              40 kg            12 reps    │  S2-c table; warm-up muted
│ 1              60 kg            10 reps    │  numberM, right-aligned columns
│ 2            62.5 kg       PR    9 reps    │  PR badge (recomputed)
│ 3                   —           skipped    │  unticked: muted
├────────────────────────────────────────────┤
│ Treadmill                          1 set   │
│ ────────────────────────────────────────── │
│ 1                               25 min     │  no weight for cardio
└────────────────────────────────────────────┘
```

Exercise count moves to the bodyS caption line. **Empty:** n/a. **Loading:** top bar only (today: blank body).
**vs today:** filled summary card plus filled per-exercise cards; set lines "Set 1 … 8 reps · 60.0 kg" as one right-hand string; unticked sets indistinguishable; no plan name; "kg·reps".

---

### 4.10 Exercises (full page, from Progress's top-bar action, Plan editor and Session detail; was "Exercise library") — **revise**

```
┌────────────────────────────────────────────┐
│ ←  Exercises                         176   │  count trailing, bodyS
│ ⌕ Search exercises                  ⚲ 2    │  S3-b: filter button + active count
│ Barbell · Chest                  Clear     │  active filters, bodyS; only if any
├────────────────────────────────────────────┤
│ Bench press                    COMPOUND    │  ExerciseListRow
│ Barbell · Chest, Triceps                   │
│ ────────────────────────────────────────── │
│ Close-grip bench press         COMPOUND    │
│ Barbell · Triceps, Chest                   │
│ ────────────────────────────────────────── │
│ Incline bench press            COMPOUND    │
│ Barbell · Chest, Front delts               │
│ ────────────────────────────────────────── │
│ My landmine press      CUSTOM COMPOUND     │  custom marker tertiary
└────────────────────────────────────────────┘
```

Filter sheet (from ⚲): three `Section`s — TYPE, EQUIPMENT, MUSCLE — each a wrapped row of text options (selected = onSurface + 2dp rule, multi-select within a section), `Clear` and `Show 14` in a pinned foot. Opening a sheet from a page is fine (rule 12).
**Empty (filters exclude all):** "No exercises match" + `Clear filters` (uses the unused `clearFilters`). **Loading:** none drawn (rule 8).
Phase 9 (decision 2) adds a `+ New exercise` FAB (bottom-end, flat pill) opening §4.19; nothing else on this page changes.
**vs today:** three scrolling chip rows (~150dp) of raw enum names, then filled cards with a disabled AssistChip badge. New: one search line, one filter line, then text rows.

---

### 4.11 Exercise detail (full page) — **rewrite**

```
┌────────────────────────────────────────────┐
│ ←  Bench press                             │
│    Compound · Barbell                      │
├────────────────────────────────────────────┤
│ READY TO PROGRESS                          │  only when true; eyebrow primary
│ Top of the range two sessions running.     │  body; no bordered box
│ Add weight next time.                      │
│ ────────────────────────────────────────── │
│ EST. 1RM                  BEST 85.0 kg     │  S4-b: hero left, reference right
│ 82.5 kg                   +5.0 IN 3M       │  numberXXL; delta primary
│ ────────────────────────────────────────── │
│ 1RM   HEAVIEST   VOLUME   REPS             │  UnderlineSelector (metric)
│ ╭────────────────────────────────────╮     │  LineChart 180dp + axis
│ │ 85 ┤                    ◉ 82.5     │     │  last point ringed + value (S2-a)
│ │    ┤             ●──●───●╱         │     │
│ │ 70 ┤ ●──●──●╱                      │     │
│ ╰────────────────────────────────────╯     │
│  12 Sep          18 Oct         23 Nov     │  bodyS axis
│ 1M    3M    1Y    ALL                      │  UnderlineSelector (range), S6-b
├────────────────────────────────────────────┤
│ RECORDS                     24 sessions    │
│ ────────────────────────────────────────── │
│ Est. 1RM                82.5 kg  23 Nov    │  StatRow + date
│ Heaviest                  70 kg  19 Nov    │
│ Best set volume          620 kg  23 Nov    │
│ Max reps                     12  02 Oct    │
├────────────────────────────────────────────┤
│ RECENT                                     │  grouped by session (issue 25)
│ 23 Nov   60×10 · 62.5×9 · 62.5×8           │
│ 19 Nov   60×10 · 60×10 · 60×9              │
├────────────────────────────────────────────┤
│ MUSCLES                                    │  BodyDiagram pair, full width,
│   FRONT          BACK                      │  ≤360dp; primary = primary, secondary =
│ Chest · Triceps, Front delts               │  inversePrimary; names under
├────────────────────────────────────────────┤
│ CUES                                       │
│ Retract shoulder blades…                   │
└────────────────────────────────────────────┘
```

The two selectors are separated by the chart so neither reads as the other's label (issue 30's failure). Reps metric shows the target band in tertiary using the exercise's plan target when it is in exactly one plan, else no band (the hard-coded 8–12 goes; domain part in the non-UI list). Cardio/mobility label in min/sec (issue 24). Bodyweight exercises: hero = max reps, metric selector = REPS only.
Phase 9 (decision 2) adds a top-bar ⋮ on custom exercises only: Edit → §4.19, Delete → `ConfirmSheet` (soft delete; history keeps the name; the exercise leaves every plan it was in).
**Empty (no history):** hero `—`, chart replaced by "Log this exercise to see progress"; Records hidden; Muscles and Cues still shown. **Loading:** top bar only.
**vs today:** bordered boxes, a 2×2 tile grid, a 120dp axis-less line, raw-enum TagChips, and an undated "Set 1, Set 2, Set 1" list.

---

### 4.12 Progress (bottom destination 3; was "Analytics") — **rewrite**

```
┌────────────────────────────────────────────┐
│ Progress                     Exercises     │  text action → §4.10 (decision 1)
│ TODAY  WEEK  MONTH  3M  YEAR               │  UnderlineSelector, pinned
│ ‹          17 – 23 Nov 2026          ›     │  PeriodPager (S6-b), pinned
├────────────────────────────────────────────┤
│ TRAINING DAYS        ON TARGET 9/12 WK     │  S4-b: hero left, reference right
│ 14                   BEST RUN 6 WK         │  numberXL; decision 7
│ ────────────────────────────────────────── │
│     M  T  W  T  F  S  S                    │  TrainingCalendar, 12 weeks
│ S   ▪  ▪  ▫  ▪  ▫  ▫  ▫  ✓                 │  ✓ primary = week met target
│     ▪  ▫  ▪  ▪  ▫  ▪  ▫  ✓                 │  month initial at month start
│ O   ▪  ▪  ▪  ▫  ▪  ▫  ·                    │  current week: no mark yet
├────────────────────────────────────────────┤
│ VOLUME                    SETS  TONNAGE    │  local selector (not the pref)
│ ────────────────────────────────────────── │
│ 54 working sets          +12% VS 4-WK      │  numberL; delta primary/Effort
│                  ╭─────╮                   │  TooltipTag over selected bar
│                  │ +12%│                   │
│  ▂▂  ▄▄  ▃▃  ▅▅  ▆▆  ┊┊                    │  BarChart: deep-volt bars, 2dp
│ - - - - - - - - - - - - - - avg 4 wk       │  primary cap; dashed tertiary ref
│  27  3   10  17  24  1                     │  week starts, bodyS; current outlined
├────────────────────────────────────────────┤
│ MUSCLES                                    │
│ ────────────────────────────────────────── │
│   FRONT          BACK                      │  BodyDiagram pair, heat ramp
│   (diagram)      (diagram)                 │  tap → breakdown sheet; tapped muscle outlined onSurface while open
│ Less ▪▪▪▪▪ More                            │
│ Chest           ━━━━━━┊━━━━━━┊━   22 sets  │  MuscleRankRow, top 6; ┊ = 10 and 20
│ Quads           ━━━━━━┊━━━━      14 sets   │  band tertiary, WEEK only (dec. 11)
│ Show all 18 →                              │
│ NOT TRAINED THIS PERIOD                    │  follows period (issue 31), max 5
│ Hamstrings · Calves · Obliques             │
└────────────────────────────────────────────┘
```

Range and period are pinned and drive every section (issues 30–32: "Session" renamed "Today"; metric choice local; neglect follows the period). The pager steps whole periods back (a minimal addition: the windows in `domain/Window.kt` gain an offset); `›` is disabled at the current period. Bars: tap selects, tag shows that week's change vs the four weeks before it. In WEEK range each muscle row draws the 10–20 working-sets band as two tertiary ticks on its meter and the value turns primary inside the band, `Effort` above it, onSurfaceVariant below (decision 11); other ranges show no band. Muscle breakdown sheet: restyled with `StatRow`s, names via `displayName`, volume via `formatVolume`.
**Empty (period has nothing):** sections keep their heads; each body reads "Nothing in this period"; the pager still works. **Loading:** top bar + selectors only.
**vs today:** an unlabeled bar-chart icon on Plans opens a bordered box with a 🔥 streak header and an unlabeled dot grid, two identical unlabeled chip rows, no history before "now", no volume chart, and a heatmap with raw enum names.

---

### 4.13 Exercise picker (full page; was a sheet — Plan editor, Logger, Import Map) — **rewrite**

S3-b's layout. The flow change: you can add several exercises in one visit (today: one sheet per exercise — building a 6-exercise plan is six round trips).

```
┌────────────────────────────────────────────┐
│ ←  Add exercises                           │  titleL
│ ⌕ Search                            ⚲      │  autofocus only from logger
├────────────────────────────────────────────┤
│ RECENT                                     │  minimal addition: last 8 used
│ ────────────────────────────────────────── │
│ Bench press                    COMPOUND    │
│ Barbell · Chest, Triceps             ☑     │  checkbox 48dp, ☑ primary
│ ────────────────────────────────────────── │
│ Lat pulldown                   COMPOUND    │
│ Cable · Lats, Biceps                 ☐     │
├────────────────────────────────────────────┤
│ ALL                                  176   │
│ ────────────────────────────────────────── │
│ Ab wheel rollout               COMPOUND    │
│ Other · Abs                          ☐     │
│ ╭──────────────────────────────────────╮   │  sticky, appears at ≥1 selected
│ │            Add 2 exercises           │   │  AppButton
│ ╰──────────────────────────────────────╯   │
└────────────────────────────────────────────┘
```

Selection order is insertion order (numbered `1`, `2` in place of ☑ once two or more are picked). Import's **Map** uses single-select mode: no checkboxes, a tap returns. RECENT hides while searching or filtering. Already-in-plan exercises show `IN PLAN` in the trailing slot and stay selectable (supersets of the same lift are legitimate).
**Empty (no match):** "No exercises match" + `Clear filters`. **Loading:** list not drawn until the first emission.
**vs today:** a full-height sheet titled "Pick an exercise" over three chip rows and filled card rows; one exercise per opening; no recents.

---

### 4.14 Settings (full page, from ⚙) — **rewrite**

```
┌────────────────────────────────────────────┐
│ ←  Settings                                │
├────────────────────────────────────────────┤
│ UNITS                                      │
│ ────────────────────────────────────────── │
│ Weight                         KG   LB     │  UnderlineSelector inline
│ Volume                   SETS  TONNAGE     │  + bodyS explanation below
├────────────────────────────────────────────┤
│ WORKOUT                                    │
│ ────────────────────────────────────────── │
│ Weekly target              4 sessions  ›   │  decision 7; 1–7, default 3
│ Default rest                     1:30  ›   │  new pref (rest target)
│ Haptics                           [on]     │  Switch, volt track
├────────────────────────────────────────────┤
│ DATA                                       │
│ ────────────────────────────────────────── │
│ Plans & backup                         ›   │  → §4.15
├────────────────────────────────────────────┤
│ ABOUT                                      │
│ ────────────────────────────────────────── │
│ Version                         1.0 (1)    │
└────────────────────────────────────────────┘
```
**vs today:** five filled cards each with a primary-tinted icon (Backup uses a dumbbell), segmented buttons, and four buttons with spinners on the main settings page.

### 4.15 Plans & backup (full page) — **new, merges Settings' Plans + Backup cards and the Import-plan entry**

Three sections, each with a one-line description and text-button actions: **IMPORT A PLAN** (`Choose file…` → §4.16, reviewed, always creates a new plan) · **ALL PLANS** (`Export`, `Import` — upserts by name; confirm dialog kept) · **FULL BACKUP** (`Export`, `Restore` — confirm dialog kept). Running operation shows `Spinner` in its row; results by snackbar. **vs today:** split across a Settings card pair and the Plans overflow, with different feedback (Toast vs Snackbar).

### 4.16 Import plan (full page) — **revise**

Pick phase: `bodyS` helper, `AppButton Choose JSON file`, error line in `error`, "Format" as a `Section` with the monospace example (fix: list includes Adductors; copy "Paste a plan JSON" → "Choose a plan JSON file"). Review phase: hero `numberXL` "6 / 8 matched" with `2 need review · 0 skipped` `bodyS`; `ImportEntryRow`s on hairlines; pinned `AppButton Import plan (6 exercises)`. Skipped and Needs-review get different glyphs (`–` vs `!`). **vs today:** a filled `primaryContainer` summary card and filled entry cards with tonal buttons.

### 4.17 Small sheets — **restyle**

Create plan, Rename plan, Delete plan, Discard session, Delete session → `ConfirmSheet` / one-field sheet (`NumberField`-style underline text field, `AppButton`). Copy unchanged except Discard: "Discard this session?" / "7 logged sets will be lost."

### 4.18 Notification & splash — **revise**

Notification: title "Push A · 7 of 18 sets"; when resting, text "Rest · target 1:30" with the chronometer based on the last completed set (count-up; switches back to session time on the next tick). A second channel, **Rest timer** (decision 5), posts once when rest reaches its target while the logger is not in the foreground: high importance so it vibrates, `setSound(null)`, no actions, auto-cancelled by the next completed set or on finish/discard. While that post is pending the service holds a `PARTIAL_WAKE_LOCK` (acquired with a timeout of remaining rest + 10 s, released when the alert posts, is re-keyed or cancelled, or the service is destroyed) so the vibration is on time with the phone in a pocket and the screen off. Splash and launcher background `#8E5226` → `#000000` with the existing foreground.

### 4.19 Custom exercise form (full page; from Exercises FAB, Exercise detail ⋮ Edit, Import Create) — **new, phase 9 (decision 2)**

```
┌────────────────────────────────────────────┐
│ ←  New exercise                    Save    │  Save = text btn primary; disabled until valid
├────────────────────────────────────────────┤
│ NAME                                       │
│ Landmine press                             │  underline field, titleM
│ ────────────────────────────────────────── │
│ TYPE                                       │
│ COMPOUND  ISOLATION  CARDIO  MOBILITY      │  UnderlineSelector, wraps if it runs out of width
│ ────────────────────────────────────────── │
│ EQUIPMENT                                  │
│ Barbell  Dumbbell  Machine  Cable          │  wrapped text options, single
│ Bodyweight  Kettlebell  Band  Other        │
│ ────────────────────────────────────────── │
│ PRIMARY MUSCLES                    2       │  multi; count trailing
│ Chest ✓  Front delts ✓  Triceps  …         │  18 names via displayName
│ ────────────────────────────────────────── │
│ SECONDARY MUSCLES                  1       │
│ Triceps ✓  Side delts  …                   │
├────────────────────────────────────────────┤
│ Delete exercise                            │  edit mode, custom only; error
└────────────────────────────────────────────┘
```

**Empty:** n/a (a form). **Loading:** edit mode shows top bar only until the exercise loads. Duplicate names are **blocked**: a name matching (ignoring case) any exercise that isn't deleted shows an inline error under the field ("You already have Landmine press") and Save stays disabled; a deleted exercise's name is free to reuse. Built-in exercises never reach this page.
**Delete** (custom only, from Exercise detail ⋮) soft-deletes the exercise and, in the same transaction, removes it from every plan; the `ConfirmSheet` says so when it is planned: "Its logged sets stay in History. It will be removed from 2 plans." A workout in progress keeps it (its sets are saved), but Finish never syncs it back into a plan. Backup restore, plan import and plan transfer match exercises by name among live exercises only; restore still matches by `clientUuid` first, deleted rows included, so history of a deleted exercise comes back attached to it.
**vs today:** the only way to make an exercise is the Import screen's "Create" sheet with chip rows; there is no edit or delete.

---

## 5. Navigation & surface model

### Destinations: 3 bar destinations + leaf pages (was one flat stack under a three-dot menu)

**Bottom bar — 3 items (decision 1), `NavigationBar`, labels always shown, no badge.** Food's bar: 80dp, `AppDivider` on top, `background` container, `tonalElevation = 0`, eyebrow labels, selected = primary label + onPrimaryContainer icon on a `primaryContainer` indicator, unselected onSurfaceVariant.

| | Destination | Holds |
|---|---|---|
| ◉ | **Train** | live session, week vs target, plans; ⚙ → Settings → Plans & backup → Import plan |
| ◻ | **History** | sessions by week → Session detail |
| ◻ | **Progress** | consistency, volume, muscles; top-bar `Exercises` → library → Exercise detail |

Why three and not four: logging is an *action* (Start / Quick log / Resume), not a place; Exercises is reference material most often reached from a context (a plan row, a session block, a picker), so it is a leaf of Progress rather than a fourth slot. Settings is rare and lives behind ⚙.

**The bar is hidden** on the Logger, Session summary, Exercise picker, Plan editor, Import, Plans & backup, Settings, Exercises, Exercise detail and Session detail — every leaf page. The logger owns the bottom of the screen (rule 11).

**Inset rule (food lesson 3, 90bcb48):** the NavHost gets `.padding(bottom = barPadding).consumeWindowInsets(PaddingValues(bottom = barPadding))` — bottom only, so a destination's own `Scaffold` cannot re-apply the navigation-bar inset and float the FAB or snackbar an inset too high. Status-bar insets stay each screen's job. When the bar is hidden, `barPadding` is 0 and leaf Scaffolds consume the system inset themselves.

**FAB:** `+ Quick log` on Train only, bottom-end above the bar, elevation 0 in all four states.


### Every secondary surface

| Surface | Decision | Why |
|---|---|---|
| Logger | full page (keep) | the place you spend 60 minutes |
| Session summary | **full page (new)** | replaces the logger in the stack; a moment, not a transient |
| Exercise picker | **full page (was a sheet)** | multi-select + its own filter sheet (rule 12); S3-b |
| Filter | bottom sheet (new) | over a page, never over a sheet |
| Session guard | bottom sheet (new) | a choice in front of a navigation |
| Rest | bottom sheet (new) | adjusts a running value without leaving the log |
| Finish | bottom sheet (merged) | confirmation |
| Target | bottom sheet (one component) | edit of one row |
| Plans & backup | **full page (new)** | file pickers and destructive restores |
| Custom exercise form | **full page (new, phase 9)** | eight fields and multi-selects; also opened from Import's Create, which used to be a sheet |
| Import plan | full page (keep) | |
| Confirms | `ConfirmSheet`; backup restore stays an `AlertDialog` | hard modal stop for data-wide operations |

### Back-stack behaviour

- **Bar destinations are peers.** Switching uses `popUpTo(graph.startDestinationId) { saveState = true }` + `launchSingleTop` + `restoreState`; fade-through 90ms out / 210ms in, no slide. Each tab keeps its scroll position. System back on History or Progress returns to Train; on Train it exits.
- **Leaf pages push onto the current tab's stack** with food's 300ms slide + `predictivePop*` pair (replacing today's 0.85 scale-fade).
- Logger ↓/back → pops; draft persists (unchanged). Train shows the live strip and, once per session, the snackbar "Push A is still running · Resume".
- Finish → summary with `popUpTo<SessionLoggerRoute>{ inclusive = true }`; Done/back → pops to the logger's origin (Train or Plan editor).
- Import commit → pop Import, push Plan editor (keep). External JSON intent → Import page on top of Train (keep).
- Notification tap → logger, gated as today (`AppNav.kt:40-50`, keep verbatim), pushed on Train's stack.
- Sheets are not back entries.

---

## 6. Behaviour specs

### 6.1 Starting a workout

| Step | Action | Feedback |
|---|---|---|
| 1 | Tap `Start` on a plan row (or editor's Start, or Quick log) | light haptic |
| 2 | Draft exists for a different start → guard sheet (§4.6) | — |
| 3 | First session ever on API 33+ → notification permission requested here, with a one-line rationale, not at launch (issue 42) | — |
| 4 | Logger pushes; plan's exercises with hint rows | progress meter at 0; REST `—` |

### 6.2 Logging a set

| Step | Action | Feedback |
|---|---|---|
| 1 | Tap weight field | 2dp primary underline; numeric keyboard, `Next` → reps; dock shrinks to one line |
| 2 | Type reps | set auto-completes: row tint fades in 150ms, numerals → onSurface, `toggle` haptic, rest restarts, meter advances 450ms — unless it is the only unticked set of its exercise: then no tick, and its check pulses in primary (~2s) on the first input |
| 3 | Or tap ✓ on an empty row | hint values committed and shown |
| 4 | Value beats baseline | `PR` badge + value in primary; `confirm` haptic |
| 5 | Last set of an exercise ticked | 600ms later the block collapses to one line; the next block scrolls up |
| 6 | Tap set badge | menu: set types / Delete set (undo) |

### 6.3 Rest timer — **target is the one addition**

| Step | Action | Feedback |
|---|---|---|
| 1 | Set completes | dock REST restarts at `0:00`, `TARGET 1:30`, meter tertiary |
| 2 | Reaches target | meter turns primary, value primary, one `confirm` haptic in-app; one vibration from the Rest timer notification if backgrounded (decision 5) |
| 3 | Over target | keeps counting; value stays primary |
| 4 | `− 15 s` / `+ 15 s` | changes *this session's* target; light haptic; meter re-scales 450ms |
| 5 | `Skip` | rest cleared (dock shows `—`) until the next completed set |
| 6 | Tap the clock → sheet | presets, "use as default", Restart now |

Target lives in the draft payload (`restTargetSeconds`, JSON — no schema change) seeded from a new DataStore pref (default 90s, today's constant).

### 6.4 Finishing

| Step | Action | Feedback |
|---|---|---|
| 1 | Tap Finish | finish sheet with stats, skipped count, optional plan-sync checkbox |
| 2 | Tap Finish in sheet | spinner in button; commit as today |
| 3 | Commit done | `confirm` haptic; summary replaces logger; service stops |
| 4 | Type a note, tap Done | note saved (`SessionDao.updateNotes`), pop |

### 6.5 Records

Baselines snapshotted at start (keep). A set beating Est. 1RM, heaviest, best set volume or max reps flags PR live; summary lists the best record per exercise with delta vs baseline. Session detail recomputes PR flags from history before that session.

### 6.6 Removing things

Exercise (logger/editor) and set → removed immediately, snackbar "Removed Bench press · Undo" 5s above the dock (Scaffold `snackbarHost`, so it never overlaps the dock or FAB). Plan, session, draft → `ConfirmSheet`. No swipe-to-delete anywhere (decision 10, §8).

### 6.7 Adding exercises — **multi-select is the one addition**

| Step | Action | Feedback |
|---|---|---|
| 1 | `+ Add exercises` (logger foot / editor foot) | picker page pushes; logger route autofocuses search |
| 2 | Tap rows | ☑ / order number; sticky `Add n exercises` appears |
| 3 | Tap Add | pop; new blocks/rows appended in pick order with type defaults (unchanged); the first scrolls into view and its weight field focuses (logger only) |

---

## 7. Migration plan

Eight phases plus one follow-up (phase 9, decision 2), each shippable and each **its own commit on `main`** (food landed eight phases in one 130-file commit; don't). Every phase ends with the font-scale check at 0.85 / 1.0 / 1.15 and a grep for raw `.dp` gaps.

### Phase 1 — Theme foundation *(breadth, shallow)*
- `ui/theme/Color.kt` — one dark scheme, every role in §1 set (no baseline fall-through); `Effort` and heat-ramp tokens.
- `ui/theme/Theme.kt` — `MaterialExpressiveTheme` → `MaterialTheme`; `GymTrackerTheme()` no-arg; delete `withAmoledSurfaces()`; keep the opaque `Surface` backdrop.
- `ui/theme/Type.kt` — `AppText` scale; map Material slots; remove the `8.sp` literal in `SetRow`.
- `ui/theme/Dimens.kt` → `Tokens.kt` (`Spacing`, `Radius`, `Sizes`, `Elevation`).
- `res/values/colors.xml`, `themes.xml` — launcher/splash bg `#000000`.
- `MainActivity.kt` — `isAppearanceLightStatusBars = false`.
- `app/build.gradle.kts` / `libs.versions.toml` — drop `ui-tooling-preview` (and debug `ui-tooling` if present): no `@Preview` exists.
- `README.md` — stack line.

### Phase 2 — Atom layer *(pure refactor)*
- New `ui/components/atoms/*` per §2; `ui/format/Format.kt`; `displayName` extensions in `data/`.
- Delete `NumberStepper.kt`, `WeeklyBarChart.kt`, `StatTile.kt`, `TagChip.kt`; rewrite `Section.kt`, `ExclusiveButtonGroup.kt` → `UnderlineSelector.kt`, `EmptyState.kt`.
- Snackbar host + zero-elevation FAB/buttons in all four states.

### Phase 3 — Logger (reference implementation)
- `SessionLoggerScreen.kt` split into `LoggerScreen.kt`, `components/LoggerTopBar.kt`, `ExerciseBlock.kt` (incl. collapsed state), `SetRow.kt` (incl. `DoneRow` tint), `LoggerDock.kt` + `RestTransport`, `RestSheet.kt`, `FinishSheet.kt`, `TargetSheet.kt`.
- `ui/components/ExercisePickerSheet.kt` → `ui/screens/picker/ExercisePickerScreen.kt` + `ExercisePickerRoute(mode, planId?)`; result returned via `savedStateHandle` (list of ids). Logger/editor VMs gain `addExercises(ids)`.
- Collapse rule lives in the screen (derived from `sets.all { it.isCompleted }` or the group's `skipped` flag + user override set), not in the draft payload.
- `SessionLoggerViewModel.kt` — `restTargetSeconds`; undo buffer for removed group/set; merged finish state; title from draft; local-date conversion.
- `SessionDraftPayload` — `restTargetSeconds: Int = 90` (ignoreUnknownKeys; no migration). `PreferencesStore` — `defaultRestSeconds`.
- `SessionSummaryScreen.kt` + route; `SessionDao.updateNotes(id, notes)`.
- **Room v4→v5** (decision 4): `SessionEntity.startedAtMillis: Long?` (`started_at_millis`), `Migration(4, 5)` in `AppDatabase.kt`, migration test; `commitSave` writes the draft's `startedAtMillis`. `BackupPayload` v3 carries it (optional).
- IME: `imePadding()` on the dock only; list uses `contentPadding` from the Scaffold — never both.

### Phase 4 — Navigation & surface model *(the most disruptive phase)*
- `Routes.kt` — add `SessionSummaryRoute(sessionId)`, `DataRoute`, `ExercisePickerRoute`; `PlansRoute` → `TrainRoute`, `HistoryRoute` and `AnalyticsRoute` → bar destinations (`ProgressRoute`), `LibraryRoute` → `ExercisesRoute`.
- New `navigation/AppBottomBar.kt` — food's bar, 3 items; visible only on the three bar routes.
- `AppNav.kt` — outer `Scaffold` with `AppBottomBar` around the `NavHost`; the inset rule (§5) exactly as written; `saveState`/`restoreState` wiring; fade-through for bar switches, slide + predictive pop for pushes; guard sheet hoisted to Train/Editor callers (needs the `draft` flow).
- `PlanListScreen.kt` — delete the overflow menu and the `+` / bar-chart top-bar icons (their destinations now live in the bar, ⚙, and Plans & backup).
- `HistoryScreen.kt`, `AnalyticsScreen.kt` — drop the back arrow (top-level now).
- `MainActivity.kt` — remove launch-time permission request; request in `SessionLoggerHost` on first start.
- Check: FAB and snackbar sit exactly one bar above the nav inset on gesture *and* 3-button navigation.

### Phase 5 — Train + Plan editor
- `PlanListScreen.kt` → `TrainScreen.kt` (+ `LiveStrip`, `WeekHero`, `PlanRow`, `Sparkline`).
- `PlanListViewModel` — sessions this week vs `weeklyTarget`, weeks on target in the last 12 (from `domain/WeeklyTarget.kt`, phase 7 ships the function; land it here if phase 7 has not), 7-day volume/sets, per-plan last-8 volume (one `SessionDao` query grouped by `plan_id`, folded with the existing tonnage logic), per-plan median duration from `started_at_millis`, and a `null` initial state.
- `PlanEditorScreen.kt` — handle-only drag, note line, typical duration, pinned Start, ⋮ menu, Toast → snackbar.

### Phase 6 — History + Session detail
Week grouping, plan names, durations (`—` for sessions logged before decision 4's migration), DURATION stat column, skipped rows (decision 8), PR recompute, note edit. `HistoryViewModel` / detail VM `null` initial states.

### Phase 7 — Exercises, Exercise detail, Progress
- `FilterSheet` replaces both `FilterRow`s; `LineChart` rewrite; grouped recent sets (one query with dates).
- `TrainingCalendar` (with the per-week target mark), `BarChart`, `TooltipTag`, `PeriodPager`, `MuscleRankRow` (with the decision-11 band).
- `domain/Window.kt` — `offset: Int` (periods back; pure function change).
- `domain/StreakCalc.kt` — **delete** (decision 7), with its Sunday-skip and unused rest-day logic. New `domain/WeeklyTarget.kt`: `sessionsInWeek`, `weeksOnTarget(last = 12)`, `bestRunOfWeeks`; ISO weeks, Monday start. Unit tests replace `StreakCalc`'s.
- `domain/MuscleVolume.kt` — expose weekly working sets per muscle for the 10–20 band (decision 11); band shown only when range = WEEK.
- `AnalyticsViewModel` → `ProgressViewModel` — local metric, period-scoped neglect, "Today", weekly volume + 4-week reference, weekly-target figures.
- `ExercisesScreen` reached from Progress's top-bar action.

### Phase 8 — Settings, Plans & backup, Import, notification
- `SettingsScreen.kt` rows, including **Weekly target** (1–7 sessions, default 3; DataStore `weeklyTargetSessions`) and **Default rest**.
- New `DataScreen.kt` (moves code from Settings); `ImportPlanScreen.kt` restyle + copy.
- `SessionNotification.kt` — rest text; new **"Rest timer" channel** (decision 5): `IMPORTANCE_HIGH`, `setSound(null)`, vibration pattern on, no actions. `SessionTrackingService` schedules one post at `lastSetCompletedAt + restTarget` (coroutine in the service scope, re-keyed whenever the draft's rest base or target changes, cancelled on Skip/finish/discard) and posts only when the logger is not resumed. Auto-cancels on the next completed set.

### Phase 9 — Custom exercises *(follow-up; decision 2; ships after phase 8 as its own commit)*

Scope, and nothing more:
- **Create:** `+ New exercise` FAB on Exercises (§4.10) and a `New exercise` foot row in the picker's empty-search state → §4.19 form page.
- **Edit:** Exercise detail ⋮ → Edit, for custom exercises only. Built-in (seeded) exercises are never editable — the seed's deterministic UUIDs and `seedVersion` reseeding depend on that.
- **Delete:** Exercise detail ⋮ → Delete (custom only) → `ConfirmSheet` "Its logged sets stay in History." (+ "It will be removed from n plans." when planned) → `softDeleteExercise` (already in the DAO; FK on `logged_sets` is `RESTRICT`, so hard delete is not an option), which also deletes its `planned_exercises` rows in the same transaction. Soft-deleted exercises vanish from Exercises, the picker and every plan; history and records still resolve their names. Name matching (import, plan transfer, backup restore) ignores deleted exercises.
- **Duplicate names:** blocked in the form with an inline error, not warned (§4.19).
- **Form:** reuses Import's "Create custom exercise" fields (name, type, equipment, primary, secondary) restyled as a page; Import's Create then opens the same page prefilled.
- **Data:** `ExerciseDao.update` + `ExerciseRepository.updateCustom`. No schema change (`ExerciseEntity` already has every field and the custom flag).
- Out of this phase: cues editing, images, merging duplicates.

### Data & schema summary

| Change | Type | Migration? |
|---|---|---|
| `sessions.started_at_millis` (decision 4) | nullable `INTEGER` column | **Yes — Room v4→v5**, `Migration(4, 5)`: `ALTER TABLE sessions ADD COLUMN started_at_millis INTEGER` (nullable, no default). Existing sessions stay `NULL` and show `—` for duration. Commit writes the draft's `startedAtMillis`. Add a migration test. |
| Backup payload v3 | optional fields: `startedAtMillis`, set type, volume metric, weekly target | No (additive; v2 files still import) |
| `SessionDraftPayload.restTargetSeconds` | JSON field, defaulted | No |
| `defaultRestSeconds`, `weeklyTargetSessions` | DataStore keys | No |
| `SessionDao.updateNotes` | DAO method on existing column | No |
| Recent exercises query; per-plan session volume and duration | DAO queries | No |
| `ExerciseDao.update` (phase 9) | DAO method | No |
| `Window` offset; `StreakCalc` → `WeeklyTarget` | pure domain functions | No |

This is the plan's only Room schema change.

### Non-UI fixes found while planning (separate, not part of the visual phases)

| # | Fix | Recommendation |
|---|---|---|
| 16 | Hints taken from same set index regardless of type/completion | Use the same-index *working, completed* set; fall back to last working set |
| 17, 34 | Stale KDoc (no-set type, drop sets, secondary 0.5) | Correct the comments |
| 19 | Exercise counts differ between History and Detail | One definition: exercises with ≥1 completed set |
| 20 | Tonnage includes failure sets | Keep (failure sets are work); state it in the caption |
| 24 | Progression uses synthetic 8–12 | Use plan target when unambiguous; otherwise hide the banner |
| 28 | Streak breaks before today's workout | Resolved by removal: `StreakCalc` is replaced by `WeeklyTarget` (decision 7, phase 7) |
| 36 | "Change" on an auto-match resets to Needs review | Return to the matched state with Change opening the picker |
| 37 | Backup drops set type and volume pref; dialog copy says duplicates | Payload v3 (see summary above); fix the copy to describe REPLACE-by-uuid |

---

## 8. Out of scope / deliberately kept

- **Room schema, apart from `started_at_millis`** (decision 4). Seed (176 exercises), `ExerciseProgress`, `MuscleVolume`, `ProgressionRule` — the UI is rebuilt around them.
- **Draft persistence** — 400ms debounce, app-scope flush on clear, singleton row. Correct and hard-won.
- **Foreground service lifecycle** and the resume-from-notification gating in `AppNav.kt`.
- **Haptics** model and pref gating.
- **Plan/backup formats** — only additive optional fields (payload v3); the screens around them change.
- **Editing past sessions** (decision 3). Session detail stays read-only apart from its note and Delete. When it is done later, the logger in an edit mode is the intended route.
- **Merging the two plan-import paths** (decision 9). They are co-located and relabelled on Plans & backup (§4.15) and keep their semantics: one reviewed plan always creates a new plan; the all-plans bundle upserts by name.
- **Swipe-to-delete** on sets, exercises, sessions or plans (decision 10). Removal is a menu item + undo snackbar (§6.6); whole-object deletion is a `ConfirmSheet`.
- **RPE** (column exists, no UI) — not added.
- **Rest alert sound and notification actions** — decision 5 is vibration only.
- No light mode, no dynamic colour, no tablet/landscape/Wear, no bundled font.

---

## 9. Asks — RESOLVED 2026-11-23

All eleven settled, each as recommended. Decisions are binding on the phases named and are cited in the body as "decision N".

| # | Question | Decision | Reasoning |
|---|---|---|---|
| 1 | Bottom navigation? | **Yes — 3 tabs: Train / History / Progress** | History, Progress and the library were behind a three-dot menu and an unlabeled icon. One `Scaffold` in `AppNav` un-buries them; logging stays an action (FAB, Start), not a tab. Phase 4. |
| 2 | Custom exercise create / edit / delete? | **Yes — as a scoped follow-up, phase 9** | Real need (today only Import can create one), but it is a feature, not a redesign; shipping it after the visual phases keeps both reviewable. Custom only; soft delete; built-ins locked. |
| 3 | Edit past sessions? | **No — out of scope (§8)** | Would need an edit mode for the logger, PR recomputation and plan-sync rules. Note edit and delete cover the common fixes. |
| 4 | Store session start time? | **Yes — Room v4→v5, nullable `started_at_millis`** | Duration is the most-asked figure about a workout and the draft already knows it; the cost is one additive migration. Old sessions show `—`. Phases 3, 5, 6. |
| 5 | Rest alert while backgrounded? | **Yes — vibration only, no sound, no actions** | The phone is in a pocket between sets; a vibration at target is the whole point of a target. Sound in a gym is noise; actions add a second control surface. Phase 8. |
| 6 | Accent colour | **Volt `#C5EA6A`** | Orange (today's identity, and S2's) collides with the failure-set hue and destructive red; aqua and mint are food-tracker's. The launcher/splash move to black. |
| 7 | Daily streak or weekly target? | **Weekly target, replacing the streak** | A daily streak punishes the rest days training needs and read 0 every morning (issue 28). "3 of 4 this week" plus "weeks on target, 9 of 12" says the same thing honestly. Default 3, set in Settings. `StreakCalc` is deleted. Phases 5, 7, 8. |
| 8 | Unticked sets at Finish | **Save as skipped** | Keeps today's data (rows with `is_completed = false`) and makes it visible: the Finish sheet states the count, Summary and Session detail render them as "skipped". |
| 9 | Merge the two plan-import paths? | **No — out of scope (§8)** | Co-location and labelling on Plans & backup removes the confusion at a fraction of the cost. |
| 10 | Swipe-to-delete? | **No — out of scope (§8)** | Conflicts with focusing a set's fields, and food-tracker's swipe shipped a data-loss bug. Menu + undo instead. |
| 11 | Weekly-volume guideline band on muscle rows? | **Yes — 10–20 working sets/week, WEEK range only, fixed** | Turns the muscle list from a ranking into a verdict; meaningless for other ranges, so hidden there. Phase 7. |

### Consequent edits to the sections above

- **Banner, §0** — status line, issue map (rows 1, 11, 14, 21, 22, 28) and the refused list now cite decisions instead of open asks; the hatched-goal bar refusal stands (the target is sessions, not volume).
- **§1** — accent hedge removed (decision 6); motion table gains the bar fade-through.
- **§2, §3** — `WeekHero` is "sessions vs target + weeks on target"; no streak figure anywhere.
- **§4.1** — bottom bar added, link row deleted, streak replaced by the weekly target (decisions 1, 7); typical plan duration (decision 4).
- **§4.2** — typical duration shown (decision 4).
- **§4.3, §4.4, §6.3** — rest target vibrates in the background (decision 5).
- **§4.5** — summary reads duration from `SessionEntity`; route is `SessionSummaryRoute(sessionId)`; skipped sets stated (decision 8).
- **§4.8, §4.9** — History is bar destination 2 with durations; Session detail leads with DURATION (decision 4).
- **§4.10, §4.19** — Exercises is a leaf of Progress; the custom-exercise FAB and form arrive in phase 9 (decision 2).
- **§4.12** — Progress is bar destination 3 with an `Exercises` top-bar action; consistency hero is training days vs weeks on target; weeks that met target are marked on the calendar; muscle rows carry the 10–20 band in WEEK (decisions 1, 7, 11).
- **§4.14, §4.18** — Settings gains Weekly target; notification gains the Rest timer channel (decisions 5, 7).
- **§5** — rewritten as the 3-tab model with food's bar and inset rule; the no-tabs default is gone.
- **§6.6** — swipe exclusion now cites decision 10.
- **§7** — phase 4 builds the bar; phases 5–8 carry the target, duration and alert work; **phase 9** added; schema summary records the v4→v5 migration as the plan's only schema change.
- **§8** — editing past sessions, merging import paths and swipe-to-delete moved here (decisions 3, 9, 10).
