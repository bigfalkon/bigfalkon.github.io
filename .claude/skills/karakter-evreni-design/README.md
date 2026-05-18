# Karakter Evreni — Design System

> Dark, glassy, gold-and-violet. A Material Design 3 system for a Turkish-language character gallery PWA where users browse, fuse and tournament-pick anime/RPG-style characters.

---

## Product overview

**Karakter Evreni** ("Character Universe") is a single-developer, public progressive web app hosted on GitHub Pages. Behind it sits a tiny Firebase backend (Firestore + Auth) holding characters, evolutions, fusions and "Alternative Universes" (AUs). The public-facing surfaces are:

| Page | Purpose |
| ---- | ------- |
| `index.html` — **Gallery** | The flagship view. A responsive grid of character cards, each filterable by star rating, sortable, zoomable, with a search modal and a detail modal showing a character's three evolutions plus its fusions. |
| `oyun.html` — **Tournament** | A bracketed picker. User chooses a mode (Random / All / 1★ / 2★ / 3★ / Fusions / ABSOLUTE WAR / per-AU) and votes 1-vs-1 until one card stands. Wins are logged per category to a leaderboard. |
| `rastgelekarakter.html` — **Random Picker** | A coin-flip / matchmaker for fusion ideas. Filter the pool by fusion frequency, then run "Random Pair", "I Pick One, You Pick One", or "Custom Pool". |
| `au-viewer.html` — **AU Viewer** | Auth-gated browser of an AU's roster: which characters have art in this universe, their AU prompts, and a bulk prompt-export tool. |
| `index2.html` — **Admin Panel** | (Private) CRUD for characters, evolutions, AUs and prompts. |
| `links.html` — **Hub** | A small launcher page linking the four user-facing tools. |

All pages share a single visual language: deep purple-on-near-black, glass surfaces over an animated aurora gradient, Metamorphous for fantasy headings, Manrope for the rest, Material Symbols for icons, and a Material Design 3 token palette.

---

## Sources

- **GitHub:** [bigfalkon/bigfalkon.github.io](https://github.com/bigfalkon/bigfalkon.github.io) — the entire codebase is the source of truth. Each page is a self-contained HTML file with inline `<style>` and `<script>`. Tailwind, Firebase, Material Symbols and Google Fonts are pulled from CDN.
- **Live site:** `bigfalkon.github.io` (linked from the PWA manifest as Karakter Evreni)
- **Uploaded references:** `uploads/icon-source.png` (color reference), `uploads/monochrome.png` (mono mark), `uploads/favicon.ico`

> If you're extending this design system, **read the original `index.html` and `oyun.html` from the repo above** — they encode hover, focus, animation and theme-switching details (AU theme overrides, secret-unlock click counter, aurora keyframes) that the README can only summarise.

---

## CONTENT FUNDAMENTALS

The product is **Turkish-language**, but headings and dev-facing strings are pragmatically mixed with English where the developer felt the English was clearer. **When writing new copy, default to Turkish.** Pick the English fallback only when matching an existing English string in the source.

### Voice & tone

- **Conversational, second-person informal ("sen" / direct verbs)** — like a fan-site, not a product UI. Compare: "Pick your favourite" (oyun.html), "Tap characters below to add them…" (rastgele).
- **Sparse, descriptive labels.** Buttons and chips are nouns or short verbs: `Gallery`, `Tournament`, `Random Picker`, `Pick Random Pair`, `Find Partner`, `Kopyala` ("Copy"), `İndir` ("Download").
- **Earnest, gently dramatic for headings** — "Discover the Heroes of the Universe", "ABSOLUTE WAR", "Tournament Champion!". Headings are allowed to use ALL CAPS for force; body text never is.
- **Plain instructions, no marketing fluff.** Helper text is one short sentence. Example: "Filter by how many fusions a character has been part of. Select none to include everyone."

### Casing

- **Title Case** for page titles and section headings ("Tournament Setup", "Filter Pool", "Manage Categories", "İsim Listesi").
- **Sentence case** for body, helper text and most chips ("All Races", "Pick your favourite", "No entries found.").
- **UPPERCASE** is rare and reserved for emphasis labels: `MODE`, `UNIVERSES`, `MAIN PROMPTS (3)`, the ABSOLUTE WAR chip. Always paired with `letter-spacing: 0.05em` or wider.

### Pronouns & framing

- The product addresses the user directly ("Your pool", "Pick your favourite", "Sign in to save wins").
- It never says "we" or refers to itself as a brand. There is no marketing voice.
- Empty states are matter-of-fact, not whimsical ("No entries found.", "No categories yet.", "No wins recorded for this category yet.").

### Numbers, stars & badges

- Star ratings are written as **`1★ 2★ 3★ 4★`** in copy, or rendered as filled Material `star` icons in colours (orange / slate / amber / fuchsia).
- AU labels in copy use the AU's own name (e.g. "Solaris Universe", "Viewing: <strong>X</strong> Universe").
- Counts use a leading icon + space: `✓ 12`, `✗ 38`, `16 characters`.

### Emoji & symbols

- **No emoji.** None in the source. All glyphs are Material Symbols (Outlined, filled via `font-variation-settings`) or unicode arrows/dividers (`←`, `→`, `✓`, `✗`, `─`).
- Repeated `─` ASCII line is used as a divider inside prompt exports.

### Example specimens

> **Hero heading:** "Character Gallery" / "Karakter Evreni"
> **Subhead:** "Discover the Heroes of the Universe" / "Browse Alternative Universe entries"
> **Section title:** "Tournament Setup"  
> **Helper:** "Filter by how many fusions a character has been part of. Select none to include everyone."  
> **CTA button:** "Start Tournament", "Pick Random Pair", "Sign In"  
> **Empty state:** "No entries found.", "Type a name or ID above to search."  
> **Modal subtitle:** "Solaris Universe — AU'da var — Her İkisi (47)"  
> **Confirm:** "Permanently delete ALL win records? This cannot be undone!"

---

## VISUAL FOUNDATIONS

### Vibe

Late-night fantasy compendium. Dark glass panes floating over a slow purple-to-pink aurora. Headings carved into a serif with sword-and-sorcery serifs. Everything else clean, modern sans. Pure black backgrounds are avoided; the canvas is a near-black `#131318` so the violet aurora can show through.

### Colors (Material Design 3 tokens)

The full token sheet lives in [`colors_and_type.css`](./colors_and_type.css). Headline values:

| Role | Hex | Use |
| ---- | --- | --- |
| `--primary` | `#BDB3FF` | All primary accents — gallery title, selected chips, focus borders, primary buttons |
| `--on-primary` | `#2D235C` | Foreground on `--primary` (deep violet, never white) |
| `--primary-container` | `#443A74` | Filled containers, app theme color in manifest |
| `--on-primary-container` | `#E6DEFF` | Text inside primary containers |
| `--fusion` | `#E0B3FF` | 4★ fusion accents — fusion badges, fusion modal radial |
| `--au` | `#64DCB4` | Alternative Universe accent — borders, badges, AU-mode pills |
| `--bg` | `#131318` | Page canvas |
| `--surface` | `#201F25` | Default card / panel fill |
| `--surface-high` | `#2A2830` | Raised surfaces, dropdowns, chips |
| `--on-surface` | `#E5E1E6` | Body text |
| `--on-surface-var` | `#C8C4D0` | Secondary text |
| `--outline-var` | `#48454E` | Hairlines, dividers, default borders |
| `--dismissed` | `#796677` | Retired/dismissed character chrome |
| `--gold` | `#FFD700` | Tournament champion glow only |
| `--danger` | `#EF5350` | Destructive actions (Reset, Delete) |

Star colors (Tailwind defaults, used as icon fills):
- 1★ `text-orange-400` · 2★ `text-slate-400` · 3★ `text-amber-400` · 4★/Fusion `text-fuchsia-400`

> AU themes can **override `--primary-*`** on `<body class="au-theme-active">` — each AU carries its own `color`, `icon`, and on-color so swapping universes recolors the whole UI.

### Typography

- **Display / fantasy:** `Metamorphous`, serif — used for `h1` page titles, character names on fusion/dismissed cards (`.font-fantastical`), and large modal titles.
- **UI / body:** `Manrope`, weights 400 / 500 / 600 / 700 / 800 — everything else.
- **Mono:** `ui-monospace, monospace` — only in prompt-export textareas and `prompt-text` blocks.

Headings are `font-extrabold` (800). Buttons and chips are `font-weight: 700`. Selected chips bump to `font-weight: 700` from their base 600. There is no italic body text.

### Backgrounds

- **The aurora.** A `body::before` fixed full-screen layer with two soft radial gradients (one primary at top-left, one fusion-tinted at bottom-right). It animates with `@keyframes aurora` — a 20-second alternating drift + 10° rotate + 1.2 scale. AU mode tints the radials with the AU's color instead of fusion.
- No imagery, no patterns, no textures outside of this. The cards do the visual work; the page is intentionally quiet.
- A fusion modal adds an additional fusion-pink radial behind its content (`fusion-modal-bg::before`) that animates `@keyframes prism` — scale 2 → 1.5 alternating.

### Glassmorphism

The signature material is **frosted glass over the aurora**:
- Sticky control panel: `rgba(27,27,32,0.75)` + `backdrop-filter: blur(14px)` + 1px `rgba(146,143,153,0.2)` border.
- Dialogs (`.m3-dialog`): `rgba(32,31,37,0.88)` + `backdrop-filter: blur(20px)` + the same hairline border.
- Side panels in `au-viewer`: `rgba(26,25,31,0.75)` + `backdrop-filter: blur(12px)`.

Always pair blur with: a translucent dark fill (75–88% alpha), a 1px `rgba(146,143,153,0.18–0.20)` border, and either `border-radius: 18px` (cards) or `28px` (dialogs).

### Corner radii

- `4px` — micro pills (paired/compare cards inner edge).
- `8px` — form inputs, menu items, prompt blocks.
- `12px` — small chips' background, list rows, alerts.
- `14–16px` — link buttons, entry cards.
- `18px` — character cards, dropdowns, control panel.
- `20px` — setup card, login box, modal sub-boxes.
- `28px` — `.m3-dialog` (the canonical M3 dialog radius).
- `99px` — pills, chips, icon buttons, all "round" buttons.

### Borders

- Default: `1px solid var(--outline-var)` on dark surfaces.
- Selected state: switch border to `var(--primary)` and bump weight to `2px` for emphasis on cards / mode chips. On hover, switch border to `rgba(var(--primary-rgb), 0.45–0.5)`.
- Fusion partner cards: `2px solid var(--fusion)`. AU cards: `2px solid rgba(var(--au-rgb), 0.5)`.

### Shadows

Three tiers, all neutral black, no colored ambient shadow except on hover where it tints toward the active accent:

| Tier | Value | Use |
| ---- | ----- | --- |
| Rest | `0 4px 12px rgba(0,0,0,0.3)` | Cards |
| Hover | `0 12px 28px rgba(0,0,0,0.45)` | Cards lifted |
| Modal | `0 8px 32px rgba(0,0,0,0.5)` | Dialogs |
| Heavy | `0 24px 48px rgba(0,0,0,0.35)` | Setup card |

Fusion hover adds a tint: `0 12px 28px rgba(var(--fusion-rgb), 0.15)`.

### Animation & motion

Soft, springy, never bouncy-cute. Easings, in order of frequency:

- `cubic-bezier(0.165, 0.84, 0.44, 1)` — card-in animation, card image hover scale. The house ease.
- `cubic-bezier(0.25, 0.46, 0.45, 0.94)` — ripples.
- `cubic-bezier(0.175,0.885,0.32,1.275)` — `pop-in` for result cards (slightly overshoots).
- `ease` / `ease-in-out` — generic transitions.

Durations: `0.18–0.35s` for hover & state, `0.5s` for card entry, `0.55s` for ripple, `5s + 20s` for the aurora loops.

Named keyframes:
- `card-in` — translateY 30px + scale 0.98 → 1, 0 → 1 opacity, with a 35ms-per-index stagger.
- `aurora` — drift + rotate, 20s alternating infinite.
- `prism` — scale 2 → 1.5, 5s alternating infinite.
- `pop-in` — scale 0.85 → 1 + translateY 12 → 0.
- `winner-pulse` — gold radial shockwave 1s for tournament results.
- `ripple-anim` — scale 0 → 4 fade-out 0.55s.
- `spin` — 360° in 0.7–0.8s linear for loading indicators.

### Hover states

- **Buttons:** `opacity: 0.88–0.9` + `translateY(-2px)` + a soft shadow. Never color shift.
- **Chips:** background lightens to `rgba(255,255,255,0.08)`.
- **Cards (gallery):** border shifts to `rgba(var(--primary-rgb), 0.45)` + shadow deepens + inner image scales `1.06`.
- **Cards (tournament contestant):** border becomes `var(--primary)` + `translateY(-8px)` + larger shadow.
- **Icon buttons:** background goes to `rgba(var(--primary-rgb), 0.1)` (a primary-tinted hover).
- **Menu items:** background `rgba(255,255,255, 0.08)`.

### Press / active

- A **Material ripple** is used on chips, buttons and modal nav. `0.28` alpha white circle scales 0 → 4 then fades. Implemented via `.ripple-effect` + `.ripple` JS handler.
- No `transform: scale(0.9x)` press shrink. Press feedback is the ripple plus button `:active` state inherited from hover.

### Transparency & blur

- Use blur **only** on surfaces that float above the aurora: sticky toolbar, dialogs, side panels, modal backdrops.
- All blurred surfaces sit at 70–90% opacity. Never solid; never fully transparent.
- Backdrop blur range: `4px` (small badge backdrop) · `6–8px` (overlays) · `12–14px` (panels & toolbar) · `20px` (dialogs).
- Image overlays (full-screen enlarge) use a non-blurred `rgba(0,0,0,0.88)` instead — content stays sharp.

### Imagery

User-uploaded character art is the entire imagery system. Treatment rules:

- **Aspect ratio:** `2/3` portrait for every character / fusion card, every contestant card, every result card.
- **Object fit:** `cover`. Images never shrink-to-fit a card.
- **Top corners only:** `border-radius: 18px 18px 0 0` on the image wrapper; card-info strip below has flat top corners.
- **Hover zoom:** `transform: scale(1.06)` over 0.5s with the house ease.
- **Color vibe:** original art ranges from saturated anime palettes (orange eyes, blue armor, gold trim) to monochrome line art. The frame around it is always neutral-dark so the art's color reads cleanly.
- **GIF lazy swap:** card images hold a `data-preview-src` static fallback and lazy-swap to a `data-gif-src` GIF when they enter the viewport (75% threshold).

### Layout & sticky chrome

- **Fixed control panel.** The toolbar at the top of `index.html` is `position: sticky; top: max(1rem, env(safe-area-inset-top) + 2.5rem)` with `z-index: 50`. It always floats over the grid.
- **Safe-area aware.** Body has `env(safe-area-inset-*)` padding on left/right/bottom. The header adds top safe-area to its padding-top.
- **Centered max-widths:** `2560px` for the gallery, `1280px` (xl) / `1536px` (2xl) for the AU viewer, `lg` (`1024px`) for the tournament, `5xl` (`64rem`) for random picker, `4xl` for the links hub.
- **Grid:** the gallery grid uses `display: grid` with a JS-computed `grid-template-columns` (auto-fit or fit-to-screen heuristic), a `gap: 1.5rem`, and stretching cells.
- **Mobile first.** Single column on mobile, breakpoints at `sm (640) → md (768) → lg (1024) → xl (1280) → 2xl (1536)`. Two-column desktop layouts (AU viewer) use `xl:flex` with a sticky sidebar.

### Spacing

Tailwind defaults plus a handful of ad-hoc values. The lived-in scale is:
- `2 / 3 / 4 / 6 / 8 / 12 / 14 / 16 / 20 / 24 / 32` (px) for paddings and gaps inside components.
- `0.5rem · 0.75rem · 1rem · 1.25rem · 1.5rem · 1.75rem · 2rem` for section padding.
- Body padding: `p-4 sm:p-6 md:p-8 xl:px-12 2xl:px-20`.

### Cards — the canonical specimen

```
border-radius: 18px;
background-color: var(--surface);                  /* #201F25 */
border: 1px solid var(--outline-var);              /* #48454E */
box-shadow: 0 4px 12px rgba(0,0,0,0.3);
overflow: hidden;
```

Hover bumps the shadow to `0 12px 28px rgba(0,0,0,0.45)` and tints the border with the active accent.

---

## ICONOGRAPHY

**Single-source icon system: Material Symbols Outlined.** Loaded from Google Fonts:

```html
<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20..48,100..700,0..1,-50..200" />
```

Used as `<span class="material-symbols-outlined">icon_name</span>`. Filling is controlled with `style="font-variation-settings:'FILL' 1;"` (filled stars, badges). Sizing is direct `font-size` in px (`14`, `15`, `16`, `18`, `20`, `26`, `36`).

### Common glyphs in use

| Glyph | Where |
| ----- | ----- |
| `search` `close` `apps` `check` | Control panel, modals |
| `filter_1` `filter_2` `filter_3` `filter_4` | Star-rating filters |
| `star` `badge` | Star row on cards, retired indicator |
| `auto_fix_high` | AU mode trigger & default AU icon |
| `arrow_back` `arrow_forward` `chevron_right` `expand_more` | Navigation |
| `zoom_in` `zoom_out` `auto_awesome_mosaic` | Gallery zoom + fit-to-screen |
| `sports_esports` `casino` `shuffle` `whatshot` | Tournament & random picker |
| `replay` `leaderboard` `category` | Tournament results |
| `admin_panel_settings` `login` `logout` `lock` | Auth & admin |
| `description` `article` `list_alt` `content_copy` `download` | Prompt-export tooling |
| `sync` (with `.spin`) | Loading spinners |
| `check_circle` `error` `delete_forever` `delete` | Status & destructive actions |
| `sort_by_alpha` `arrow_upward` `arrow_downward` | Sort menu |
| `groups` `person` `merge` | Type filters in AU viewer |

### No SVG icons, no PNG icons, no emoji

The entire app uses the icon font exclusively. The only non-Material glyphs are unicode (`✓`, `✗`, `─`) used inside textual labels and count badges. There are no inline SVGs, no PNG icon sprites, and **no emoji anywhere in the codebase or copy**. Don't introduce them.

### Brand mark / logo

The brand mark is `assets/icon-source.png` — an illustrated character bust (the "Karakter Evreni mascot") used as the PWA icon and apple-touch-icon. There is no wordmark; the title `Character Gallery` / `Karakter Evreni` is set in **Metamorphous** and acts as the lockup. Mono fallback: `assets/monochrome.png`.

### Available assets

| File | Purpose |
| ---- | ------- |
| `assets/icon-source.png` | Full-color source mark (anime-style bust) |
| `assets/monochrome.png` | Mono fallback |
| `assets/icon-512.png` · `icon-192.png` | PWA icons (any) |
| `assets/icon-maskable-512.png` · `icon-maskable-192.png` | PWA maskable icons |
| `assets/apple-touch-icon.png` | iOS home-screen |
| `assets/favicon.ico` · `favicon-32x32.png` · `favicon-16x16.png` | Browser favicons |

---

## Index — files in this design system

| File | What's in it |
| ---- | ------------ |
| `README.md` | This document — product, voice, visual foundations, iconography. |
| `colors_and_type.css` | Every color token + type variable as CSS custom properties, plus utility classes (`.h1`, `.body`, `.chip`, `.glass`, …). |
| `assets/` | Brand mark, favicons, PWA icons. |
| `preview/` | Standalone HTML "specimen cards" that populate the Design System tab. |
| `ui_kits/karakter-evreni/` | The Karakter Evreni PWA UI kit: `index.html` plus modular JSX components (`Header.jsx`, `CharacterCard.jsx`, `ControlPanel.jsx`, `CharacterModal.jsx`, `Tournament.jsx`, …) that recreate the live product at high fidelity. |
| `SKILL.md` | Skill manifest — describes this folder to an agent invoking it. |

---

## Font substitution flag

**No font files were shipped with the source codebase** — both `Manrope` and `Metamorphous` are loaded directly from Google Fonts CDN. The design system loads them the same way (`colors_and_type.css` imports them at the top). If you need offline / self-hosted fonts, drop `.woff2` files into a `fonts/` folder and rewrite the `@import` at the top of `colors_and_type.css` to use `@font-face` declarations instead. **No substitution was needed.**
