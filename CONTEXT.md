# Git-Guild

A gamified code-hosting + bounty platform: maintainers publish **Quests** from real repository
work, beginners complete them for XP. This file captures domain language as it is resolved during
design sessions; it is not exhaustive yet.

## Language

### Quest Taxonomy

**Category**:
A maintainer-chosen bucket a Quest belongs to (exactly one per Quest). Has a name + description.
_Avoid_: type, group.

**Tag**:
A free-form coloured label attached to a Quest (zero or more per Quest), stored via the
`quest_tag_relations` join table.
_Avoid_: label, keyword.

**Difficulty**:
A fixed, read-only enum (A/B/C/D). Not user-editable — no create/update surface.

**Enabled / Disabled**:
A soft-retirement flag on a Category or Tag. **Disabling only stops it being offered for *new*
Quests** (the publish dropdown shows enabled-only); Quests that already reference it keep that
reference. Therefore disabling an in-use Category/Tag is safe and is *not* blocked.
_Avoid_: delete, archive (there is no hard delete).

**Quest count (引用数)**:
The number of Quests currently referencing a Category/Tag. Informational only — it does **not**
gate disabling.

## Relationships

- A **Quest** has exactly one **Category**, one **Difficulty**, and zero or more **Tags**.
- **Category** and **Tag** lists have two audiences with opposite needs:
  - the **publish page** (maintainer) sees **enabled-only**;
  - the **admin taxonomy page** sees **all** (so disabled items can be re-enabled), via
    `includeDisabled=true`.

## Example dialogue

> **Dev:** "If a Tag is used by 7 Quests and an admin disables it, do those Quests lose the Tag?"
> **Domain expert:** "No. Disabling just removes it from the publish picker. Existing Quests keep
> it. That's why we show the count as context but never block disabling on it."

## Flagged ambiguities

- "标签 (tag)" was used to mean the whole taxonomy page; resolved — the page manages **Category +
  Tag + Difficulty**, and the work covers all three (Difficulty stays read-only).
