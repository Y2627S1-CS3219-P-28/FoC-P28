# Shared Frontend UI Style Baseline

Status: workflow baseline for future Order Service frontend work.

This guide records the visual language visible in the user-provided staging
"Friend on Campus" dashboard screenshot and reconciles it with the live shared
frontend implementation. It is a consistency rule, not a substitute for a feature
requirement or an approved detailed UI design.

## Authority and evidence

1. The current shared frontend source and its design tokens are authoritative for
   exact implementation values.
2. The supplied staging screenshot is the visual reference for composition, density,
   hierarchy, and overall tone.
3. Screenshot-only measurements are approximate; do not infer a new product behavior,
   route, navigation item, role, or interaction from the screenshot.
4. Before implementing a UI, re-inspect `../frontend/` and reuse its existing layout,
   primitives, tokens, and patterns.

Reference source inspected for this baseline:

- `../frontend/src/app/layout.tsx`
- `../frontend/src/app/globals.css`
- `../frontend/src/components/app-shell.tsx`
- `../frontend/src/components/ui/`
- User-provided staging dashboard screenshot dated 2026-09-29

## Visual character

- Clean, calm, spacious, and utility-focused.
- Predominantly white and near-black monochrome surfaces.
- Strong hierarchy through weight, spacing, and alignment rather than decoration.
- Thin neutral borders and very restrained shadows.
- Rounded controls and panels, but no excessive pill-shaped or ornamental treatment.
- Consistent line-icon language; avoid mixing filled icon families with the existing
  outlined navigation icons.
- Keep the interface legible and quiet at both desktop and mobile-web widths.

## Typography

The live frontend uses `Geist` from `next/font/google` as `--font-sans`; use that
existing font rather than adding a new font family.

| Purpose | Existing baseline |
|---|---|
| Body and navigation | Geist Sans, normally `text-sm` (14px), regular/medium |
| Page heading | `text-2xl` (24px), semibold, tight tracking |
| Card/dialog heading | `text-xl` (20px) or `text-base` (16px), medium/semibold |
| Supporting/helper text | `text-sm` (14px) or `text-xs` (12px), muted foreground |
| Buttons and controls | `text-sm` (14px), medium |
| Code or technical values | Existing Geist Mono token only when appropriate |

Do not hard-code a different web font or introduce a second heading family without an
approved design-system decision.

## Color and surface baseline

Use the semantic tokens in `frontend/src/app/globals.css` and existing shadcn
components instead of scattered literal colors:

- `background` and `card`: white/light surfaces.
- `foreground`: near-black primary text.
- `primary`: near-black action surface with light text; use for the strongest CTA.
- `muted`/`secondary`: very light neutral surfaces for active navigation and quiet
  controls.
- `muted-foreground`: gray supporting text.
- `border`: light neutral one-pixel separators and card outlines.
- `destructive`: reserved for actual destructive/error states.
- Existing status colors (for example, the supplier open badge) must remain semantic
  and restrained.

Avoid introducing bright brand colors, gradients, heavy drop shadows, or a new dark
theme treatment merely to decorate an Order Service screen.

## Layout and component patterns

- Reuse the shared `AppShell` and responsive sidebar rather than creating another
  navigation shell.
- Desktop uses a persistent left sidebar with a subtle right border; mobile uses the
  existing responsive navigation pattern.
- Sidebar navigation uses compact icon-plus-label rows, `rounded-lg` active states,
  modest horizontal padding, and clear selected contrast.
- Brand/header treatment is compact: icon plus product name, medium/semibold weight,
  and a thin divider.
- Main content should use generous whitespace and a clear content boundary or card
  when the feature calls for one.
- Reuse `Button`, `Card`, `Badge`, `Dialog`, `Sheet`, `Skeleton`, and other existing
  UI primitives before creating new variants.
- Default panel/card treatment is a light border, rounded corners (usually the
  existing `rounded-xl` family), and minimal shadow/ring.
- Use Lucide icons already used by the frontend; keep icon size and stroke weight
  consistent with neighboring components.

## Interaction and responsive expectations

- Preserve the existing hover, focus-visible, disabled, loading, and error patterns.
- Use short, subtle transitions; do not add large motion or layout-shifting effects.
- Design one responsive web experience for desktop and mobile browsers. Do not create a
  separate native mobile UI without explicit approval.
- Validate the layout from 320px through 1920px and check touch-target sizing,
  overflow, text wrapping, keyboard focus, and reduced-motion behavior where relevant.
- Keep UI state and messages consistent with the approved Order Service contract and
  application role/mode. Visual consistency never replaces backend authorization.

## Implementation gate for future Order Service UI

Before frontend implementation:

1. Read this guide and `docs/frontend-integration-workflow.md`.
2. Re-inspect the live shared frontend and identify reusable components/tokens.
3. Propose the affected page, route, role/mode, states, responsive behavior, and API
   contracts; do not invent them from the screenshot.
4. Obtain the required feature-level architecture/UI approval.
5. Implement with the existing Geist/shadcn/Lucide conventions and verify lint,
   typecheck, role/mode behavior, accessibility, and responsive behavior.

When a future approved design intentionally departs from this baseline, record the
reason and approval in the existing change/architecture records rather than silently
overwriting this guide.
