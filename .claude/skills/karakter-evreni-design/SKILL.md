---
name: karakter-evreni-design
description: Use this skill to generate well-branded interfaces and assets for Karakter Evreni, either for production or throwaway prototypes/mocks/etc. Contains essential design guidelines, colors, type, fonts, assets, and UI kit components for prototyping a Turkish-language character gallery PWA with dark glassmorphism and Material Design 3 tokens.
user-invocable: true
---

Read the `README.md` file within this skill, and explore the other available files.

If creating visual artifacts (slides, mocks, throwaway prototypes, etc), copy assets out and create static HTML files for the user to view. If working on production code, you can copy assets and read the rules here to become an expert in designing with this brand.

If the user invokes this skill without any other guidance, ask them what they want to build or design, ask some questions, and act as an expert designer who outputs HTML artifacts _or_ production code, depending on the need.

## Quick orientation

- **README.md** — full brand voice, visual foundations, iconography rules.
- **colors_and_type.css** — every CSS token you need (`--primary`, `--au`, `--surface`, type ramp, radii, easings).
- **assets/** — logo, monochrome fallback, PWA icons, favicons.
- **preview/** — small standalone HTML cards that demonstrate every token & component in isolation. Open any of these to see exact specs.
- **ui_kits/karakter-evreni/** — a working React UI kit recreating the four core surfaces (Hub, Gallery, Character Modal, Tournament). Components are split into `Primitives.jsx` + per-screen `Screen-*.jsx`. Use `index.html` as the entry point.

## Hard rules

1. **Dark only.** Background is `#131318`. No light theme.
2. **Two fonts.** `Metamorphous` (serif, fantasy display) for `h1` & character names. `Manrope` (sans, 400–800) for everything else. Mono only inside prompt blocks.
3. **One icon system.** Material Symbols Outlined, loaded from Google Fonts. No SVG icons. No emoji. No PNG icons. Unicode `✓ ✗ ─` only inside text strings.
4. **Glass over aurora.** Sticky toolbars, dialogs and side panels use `rgba(27,27,32,0.75–0.88)` + `backdrop-filter: blur(14–20px)` + a `rgba(146,143,153,0.18–0.20)` hairline border.
5. **Cards are 2:3 portrait, 18px radius.** Image fills top, info strip below. Hover lifts shadow and tints the border with the active accent.
6. **Language.** UI strings default to Turkish (or matching-the-source English). Tone is fan-site informal, no marketing voice, no emoji.

## When in doubt

Lift values directly from `colors_and_type.css` or from one of the `preview/*.html` cards — they encode the lived-in specs, not idealised ones.
