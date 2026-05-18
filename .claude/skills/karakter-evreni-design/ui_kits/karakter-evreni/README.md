# Karakter Evreni — UI Kit

A click-thru recreation of the Karakter Evreni progressive web app. Modular React (JSX) components, plus a host `index.html` that demonstrates the four core surfaces with working tab navigation and modal interaction.

This kit is a faithful recreation of the live product at [bigfalkon/bigfalkon.github.io](https://github.com/bigfalkon/bigfalkon.github.io). It is **cosmetic** — Firebase, authentication, the secret-unlock click counter, lazy GIF observers and the real character database are **not** wired up. Use these components as a visual specimen library and as a fast scaffold when designing new screens.

## Files

| File | Purpose |
| ---- | ------- |
| `index.html` | Host page. Mounts `<App>`, provides the tab-strip navigation, persists the open character through screen switches. |
| `styles.css` | Global stylesheet (CSS custom properties, aurora `body::before`, glass surfaces, all component classes). Mirrors the inline `<style>` blocks in the original product. |
| `Primitives.jsx` | Shared atoms: `<MI>` (Material icon), `<Chip>`, `<IconBtn>`, `<Stars>`, `<CharacterCard>`, `<GlassPanel>`. |
| `data.jsx` | Mock dataset (`CHARACTERS`, `AUS`, `FILTER_OPTIONS`, `SORT_OPTIONS`). |
| `Screen-Gallery.jsx` | The flagship view — header, sticky glass control panel, filter + AU dropdown menus, AU banner, zoom controls, character grid. |
| `Screen-CharacterModal.jsx` | Character detail dialog — three evolutions + fusion partners + footer nav. |
| `Screen-Tournament.jsx` | Tournament — setup card with mode chips + category chips + count input, plus a live 1-vs-1 match screen with VS bubble and progress bar. |
| `Screen-Hub.jsx` | The launcher page (`links.html`) — link cards + a "Developer Tools" disclosure. |
| `assets/` | Logo + icon assets copied from the design system root. |

## Coverage vs. the live product

| Surface | Covered? | Notes |
| ------- | -------- | ----- |
| `index.html` (Gallery) | ✅ Full | Sticky toolbar, filter menu (incl. sort + race section), AU menu with locked states (locked omitted in this kit), zoom cluster, fit-to-screen, character grid with hover/fusion/dismissed cards, AU banner, AU dots. |
| `index.html` modal | ✅ Full | Three-evolution layout, fusion grid, modal nav footer, glass-dialog 28px radius. |
| `oyun.html` (Tournament) | ✅ Setup + match | Setup card with mode chips (incl. ABSOLUTE WAR + AU chips), category, count. Match screen with progress bar, VS bubble, contestant cards. Leaderboard & results screens are intentionally omitted — would be follow-up work. |
| `rastgelekarakter.html` | ⚠ Not built | Patterns reused from Gallery + Tournament. Would be filter chips + pool grid + 3 mode cards. |
| `au-viewer.html` | ⚠ Not built | Auth-gated. AU pill selector + tabbed entries panel. |
| `links.html` (Hub) | ✅ Full | Link cards with hover lift, dashed developer-tools disclosure. |
| `index2.html` (Admin) | ❌ Out of scope | Private surface, not recreated. |

## Notes on faithfulness

- Card placeholders use Material Symbols inside a gradient rather than fake-illustrated characters. In production these are real character art from the live database.
- The mascot (`assets/icon-source.png`) is used as the 3★ portrait for "Aria of Ember" so at least one card shows real product art.
- All copy is in English to match the codebase's mixed-but-English-leaning UI strings. Switch to Turkish (`Karakter Galerisi`, `Turnuva Kurulumu`, etc.) for production work.

## How to extend

Components export onto `window` so any sibling `<script type="text/babel">` can use them. The host script in `index.html` shows the pattern. To add a new screen:

1. Write `Screen-MyThing.jsx` exporting `window.MyThingScreen = …`.
2. Add a `<script type="text/babel" src="Screen-MyThing.jsx">` tag in `index.html` **after** `Primitives.jsx` and `data.jsx`.
3. Add an entry to the `SCREENS` array in the host script.
