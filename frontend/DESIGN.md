---
name: GX J-Credit Carbon
description: Operate-mode web app for companies reporting emission reductions under the J-Credit scheme.
colors:
  primary: "#1468D9"
  primary-strong: "#1154B1"
  accent-surface: "#EEF6FF"
  ink: "#17243A"
  ink-muted: "#4B5869"
  placeholder: "#687587"
  canvas: "#F4F7FB"
  surface: "#FFFFFF"
  surface-subtle: "#F6F8FB"
  hairline: "#DFE4EA"
  input-border: "#828FA1"
  success: "#0F9F62"
  success-strong: "#08794A"
  success-surface: "#ECFDF3"
  warning: "#C77700"
  warning-strong: "#975800"
  warning-surface: "#FFF9E8"
  danger: "#D7373F"
  danger-strong: "#AD232B"
  danger-surface: "#FFF0F0"
typography:
  page-title:
    fontFamily: "Noto Sans JP, Hiragino Sans, Yu Gothic UI, Meiryo, system-ui, sans-serif"
    fontSize: "1.75rem"
    fontWeight: 700
    lineHeight: 1.4
    letterSpacing: "0.02em"
  section-title:
    fontFamily: "Noto Sans JP, Hiragino Sans, Yu Gothic UI, Meiryo, system-ui, sans-serif"
    fontSize: "1.25rem"
    fontWeight: 700
    lineHeight: 1.4
    letterSpacing: "0.02em"
  subsection-title:
    fontFamily: "Noto Sans JP, Hiragino Sans, Yu Gothic UI, Meiryo, system-ui, sans-serif"
    fontSize: "1.0625rem"
    fontWeight: 700
    lineHeight: 1.5
  body:
    fontFamily: "Noto Sans JP, Hiragino Sans, Yu Gothic UI, Meiryo, system-ui, sans-serif"
    fontSize: "0.9375rem"
    fontWeight: 400
    lineHeight: 1.7
  table:
    fontFamily: "Noto Sans JP, Hiragino Sans, Yu Gothic UI, Meiryo, system-ui, sans-serif"
    fontSize: "0.875rem"
    fontWeight: 400
    lineHeight: 1.6
  label:
    fontFamily: "Noto Sans JP, Hiragino Sans, Yu Gothic UI, Meiryo, system-ui, sans-serif"
    fontSize: "0.8125rem"
    fontWeight: 400
    lineHeight: 1.5
    letterSpacing: "0.04em"
  metric:
    fontFamily: "Noto Sans JP, Hiragino Sans, Yu Gothic UI, Meiryo, system-ui, sans-serif"
    fontSize: "2rem"
    fontWeight: 700
    lineHeight: 1.2
rounded:
  sm: "4px"
  md: "6px"
  lg: "8px"
  full: "9999px"
spacing:
  1: "4px"
  2: "8px"
  3: "12px"
  4: "16px"
  6: "24px"
  8: "32px"
  12: "48px"
components:
  button-primary:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.surface}"
    rounded: "{rounded.lg}"
    height: "40px"
    padding: "0 20px"
  button-primary-hover:
    backgroundColor: "{colors.primary-strong}"
  button-outline:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.ink}"
    rounded: "{rounded.lg}"
    height: "40px"
  card:
    backgroundColor: "{colors.surface}"
    rounded: "{rounded.lg}"
    padding: "24px"
  input:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.ink}"
    rounded: "{rounded.md}"
    height: "40px"
  status-chip:
    rounded: "{rounded.full}"
    typography: "{typography.label}"
    padding: "2px 10px"
---

# Design System: GX J-Credit Carbon

> **Status:** the tokens, components and layout rules below are live in code. Rules marked **(pending)** change copy, flow or data and are not applied yet; they are tracked in the local note `.impeccable/PENDING_TEXT_AND_FLOW_CHANGES.md`.

## Overview

A working tool for company staff who register sites, join J-Credit projects, and report monitoring data. People use it at a desk, repeatedly, often to file something that will be audited. The interface should feel like a well-kept 申請書: calm, exact, quietly blue. The product's subject is tonnes of CO₂ reduced, so figures are the loudest thing on any screen. Decoration stays out of the way.

Mode: **Operate**. Familiar patterns win over expression. The brand lives in precision: tabular figures, honest units, one primary action per view.

## Colors

### Primary
- **Primary blue** `#1468D9`: the one action per view, active navigation, links, focus ring. Solid fill only, never a gradient.
- **Primary strong** `#1154B1`: hover and pressed state for primary.
- **Accent surface** `#EEF6FF`: selected rows, active sidebar item.

### Neutral
- **Ink** `#17243A`: values, headings, body.
- **Ink muted** `#4B5869`: labels, descriptions, metadata. 7:1 on white.
- **Placeholder** `#687587`: input placeholders only, 4.7:1 on white. Example values should be written as `例：…` **(pending)**.
- **Canvas** `#F4F7FB` page, **Surface** `#FFFFFF` cards, **Hairline** `#DFE4EA` borders.
- **Input border** `#828FA1`: field and outline-button boundaries, 3.3:1 on white (WCAG 1.4.11 needs 3:1). Hairline is for dividers only, never for a field edge.

### Status
Success, warning and danger each have a base, a strong text tone and a pale surface. They mean workflow state only: 承認 / 入会済 / 参加中 = success, 申請中 = warning, 却下 / 取消 = danger, 未参加 / 未入会 = neutral.

### Named Rules
- **Color is a status, not a decoration.** A status hue never appears on an element that carries no status.
- **Neutral means "not yet".** Not-yet-joined is neutral grey, not warning amber. Amber means a person is waiting.
- **One status, one tone, everywhere.** Project statuses map once, in `features/admin/dashboard/projectStatusPresentation.ts`: 作成済み/終了 = neutral, 準備中/変更中 = info (the project side is working), 妥当性確認処理/検証中/クレジット申請中 = warning (waiting on another party's review), 実施中 = success, 却下 = danger. Badges, table selects and charts read from that map; no local colour tables.

## Typography

Noto Sans JP (400 / 500 / 700) with Hiragino Sans, Yu Gothic UI and Meiryo as local fallbacks. `font-feature-settings: "palt"` on headings tightens kana spacing. Figures use `tabular-nums`.

### Hierarchy
- **Page title** 28px / 1.4 / 700: one `h1` per page.
- **Section title** 20px / 1.4 / 700: `h2`.
- **Subsection** 17px / 1.5 / 700: `h3`, card titles.
- **Body** 15px / 1.7 / 400.
- **Table** 14px / 1.6.
- **Label** 13px / 1.5 / 400, +0.04em, ink-muted.
- **Metric** 32px / 1.2 / 700, tabular.

### Named Rules
- **Bold is for names and numbers.** Labels are regular weight in ink-muted, values are medium in ink, and only titles and figures are bold.
- **Three weights.** 400, 500, 700. No 600: the font does not ship it.
- **One unit spelling (pending).** `t-CO₂` everywhere.

## Layout

A 4px spacing grid: 4 / 8 / 12 / 16 / 24 / 32 / 48. Pages sit in a centred container with a 16px gutter on mobile, 24px on tablet and 32px on desktop. The content never touches the sidebar. Lists of records are tables on desktop (≥768px) and stacked rows on mobile. More space above a heading than below it.

## Elevation & Depth

Flat. Cards are defined by a 1px hairline border on the canvas. Shadows are reserved for floating layers (popover, dropdown, dialog, toast). **A card never sits inside another card.** Inner grouping uses a hairline divider or a subtle surface tint.

## Shapes

- 4px: tags, checkboxes, small chips inside tables.
- 6px: inputs and selects.
- 8px: cards, buttons, dialogs.
- Full: avatars, status dots and status chips only.

## Components

### Buttons
40px tall (44px on touch screens via `pointer: coarse`), 8px radius, 15px medium text. Primary is solid blue. Outline is a white surface with an input-border edge. Ghost is for tertiary actions and icon buttons. One primary per view. A reject or decline choice beside the primary uses `destructive-outline`; solid `destructive` is reserved for the confirming step (e.g. inside the dialog). Row actions in tables are outline buttons at 36px (44px on touch).

### Chips
Status chips are full-radius with a pale status surface, strong status text and a 6px dot. Text always accompanies the dot.

### Cards / Containers
White, hairline border, 8px radius, 24px padding, no shadow.

### Inputs / Fields
40px, 6px radius, input-border. On desktop, `.form-label-left` on a `FormCard` puts labels in a 13rem left column. Required fields will carry a `必須` pill and optional fields an `任意` pill **(pending; today: red `*`)**.

### Navigation
A sidebar grouped by job, with the active item on accent surface. Deep pages publish a breadcrumb to the topbar.

## Do's and Don'ts

### Do:
- Use the tokens and components above; no raw hex or palette classes.
- *(pending)* Show tonnes of CO₂ and credits wherever a site or project is summarised.
- *(pending)* One term per concept: **入会** = company joins a project, **参加** = a site participates in a joined project.
- *(pending)* End a submission with a receipt: what was sent, when, and what happens next.

### Don't:
- Don't use gradients, glass, or coloured left borders on cards.
- Don't nest cards.
- *(pending)* Don't clamp progress at 100%. Over-achievement is shown as it is.
- Don't show placeholder company names or sample values as if they were data.
