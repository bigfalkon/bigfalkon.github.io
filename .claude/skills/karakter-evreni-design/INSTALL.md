# Installing `karakter-evreni-design` in Claude Code

This folder is an [Agent Skill](https://docs.claude.com/claude-code/skills). Drop it into your skills directory and Claude Code will pick it up.

## 1. Copy into your skills folder

Pick one:

**Per-project** (recommended — versioned with the repo):
```bash
mkdir -p .claude/skills
cp -R karakter-evreni-design .claude/skills/
```

**Personal / global** (available in every project on your machine):
```bash
mkdir -p ~/.claude/skills
cp -R karakter-evreni-design ~/.claude/skills/
```

> Rename the unzipped folder to `karakter-evreni-design` if it isn't already — the folder name should match the `name:` field in `SKILL.md`.

## 2. Use it

In any Claude Code session, ask:

- "Use the karakter-evreni-design skill to build a settings page."
- "Mock a new tournament results screen in the Karakter Evreni style."
- "Invoke karakter-evreni-design and recreate the Gallery as a React component."

Claude Code will read `SKILL.md`, then `README.md`, then pull tokens from `colors_and_type.css` and components from `ui_kits/karakter-evreni/` as needed.

## 3. What's inside

| Path | Purpose |
| --- | --- |
| `SKILL.md` | Agent Skill manifest (frontmatter + quick orientation). |
| `README.md` | Full brand voice, visual foundations, iconography. |
| `colors_and_type.css` | All design tokens as CSS custom properties. |
| `assets/` | Logo, monochrome mark, PWA icons, favicons. |
| `preview/` | Standalone HTML specimen cards — one concept per file. |
| `ui_kits/karakter-evreni/` | Working React UI kit recreating four core screens. |

## 4. Source of truth

The design system was extracted from the live codebase at
<https://github.com/bigfalkon/bigfalkon.github.io>. If something looks off,
that repo wins — open an issue or update the tokens to match.
