[Mob Journal on CurseForge](https://www.curseforge.com/minecraft/mc-mods/mob-journal)

# 📘 Mob Journal – Custom Mob Descriptions

**Mob Journal** supports fully customizable descriptions for mobs using simple, readable **Markdown** in JSON. Great for modpack creators, lore builders, or anyone wanting to add flavor and detail to mobs.

---

## 🛠️ How to Add Custom Mob Descriptions

1. Open your resource pack (or mod) folder.
2. Navigate or create the path:
   ```
   assets/journal/mobs_desc/<mod_namespace>/<mob_name>.json
   ```
   ✅ **Example:**
   ```
   assets/journal/mobs_desc/minecraft/zombie.json
   ```

3. Create a JSON file with the following content:

```json
{
  "description": [
    "## {mobName}",
    "*The undead menace that groans in the night...*",
    "",
    "{red}Health:{reset} {getHealth}",
    "{gray}Armor:{reset} {getArmor}",
    "",
    "[Hello!](hover:This is a tooltip shown when hovered!)",
    "",
    "**Combat Stats**",
    "You have slain this mob {getTimesKilled} times.",
    "It has slain you {getTimesDiedTo} times.",
    "",
    "**Drops:**",
    "{getLootDrops}",
    "",
    "{gold}Tip:{reset} You can customize this description using markdown!"
  ]
}
```

---

## 🧩 Supported Variables

| Variable | Description |
|---|---|
| `{mobName}` | The display name of the mob |
| `{getHealth}` | Max health of the mob |
| `{getArmor}` | Armor value |
| `{getLootDrops}` | Automatically shows known drops |
| `{getTimesKilled}` | Times the player has slain the mob |
| `{getTimesDiedTo}` | Times the mob has slain the player |
| `{getTameable}` | Yes / No |
| `{getCategory}` | Monster / Creature / Ambient / ... |
| `{getTags}` | Comma-separated entity tags, or `None` |
| `{namespace}` | The namespace of the mob's ID |
| `{path}` | The path part of the mob's ID |
| `{entityType}` | The full entity type ID |
| `{attribute.<id>}` | Any attribute, e.g. `{attribute.minecraft:generic.attack_damage}` |

Rows whose value resolves to `N/A` are hidden automatically, so a passive mob will not show an empty attack row.

---

## ✨ Markdown & Text Features

| Feature | Syntax | Example |
|---|---|---|
| Bold | `**text**` | `**bold**` |
| Italic | `*text*` | `*italic*` |
| Heading | `## Title` | `## Mob Info` |
| Tooltip | `[word](hover:tip)` | `[hover me](hover:Hi!)` |

Headings are **centered by default**.

---

## 📐 Line Alignment

Prefix any line with an alignment marker.

| Marker | Result |
|---|---|
| `{left}` | Left aligned (default for normal text) |
| `{center}` | Centered |
| `{right}` | Right aligned |

```json
"## {mobName}",
"{center}A creature of the night.",
"{right}Day {getTimesKilled}",
"{left}## Drops"
```

Use `{left}` on a heading to override its default centering.

---

## 🖼️ Inline Items & Textures

Embed **items** and **textures** directly in a line.

**Item:**
```
[label](item:namespace:item_id scale=1.0 "Optional Tooltip")
```
Items without a tooltip use Minecraft's own item tooltip.

**Texture:**
```
[label](texture:namespace:path/to/image.png scale=1.0 "Optional Tooltip")
```

**Texture sizing:**

| Option | Meaning |
|---|---|
| `width=` / `height=` | Size the image is drawn at |
| `srcwidth=` / `srcheight=` | Actual pixel size of the source file |

Set both pairs when the source is not 16×16, otherwise the image is cropped rather than scaled:

```
[Heart](texture:minecraft:textures/mob_effect/regeneration.png width=16 height=16 srcwidth=18 srcheight=18)
```

---

## 🔍 Hover Previews

Hovering a word can show an image instead of only text — useful for pointing at an item without cluttering the page.

| Syntax | Shows |
|---|---|
| `[x](hover:some text)` | Plain tooltip text |
| `[x](hover:item:minecraft:bone)` | A single item icon |
| `[x](hover:tag:minecraft:meat)` | One icon, cycling through the tag |
| `[x](hover:tag_grid:minecraft:meat)` | A 4×4 grid, paging through the tag |
| `[x](hover:texture:namespace:path.png)` | A texture |

```json
"Tame with [bones](hover:item:minecraft:bone), breed with [meat](hover:tag_grid:minecraft:wolf_food)."
```

- `tag:` cycles one icon per second with an `(n/total)` counter.
- `tag_grid:` shows up to 16 at a time and pages every two seconds; tags of 16 or fewer never page.
- Nested tags are resolved, so a tag containing another tag works.
- An optional quoted string adds a caption: `[x](hover:item:minecraft:bone "Found in ruins")`

Hovered words are underlined so they read as interactive.

---

## 🎨 Color & Style Codes

| Code | Color |
|---|---|
| `{black}` | §0 |
| `{dark_blue}` | §1 |
| `{dark_green}` | §2 |
| `{dark_aqua}` | §3 |
| `{dark_red}` | §4 |
| `{dark_purple}` | §5 |
| `{gold}` | §6 |
| `{gray}` | §7 |
| `{dark_gray}` | §8 |
| `{blue}` | §9 |
| `{green}` | §a |
| `{aqua}` | §b |
| `{red}` | §c |
| `{light_purple}` | §d |
| `{yellow}` | §e |
| `{white}` | §f |

| Style | Effect |
|---|---|
| `{bold}` | Bold |
| `{italic}` | Italic |
| `{underline}` | Underline |
| `{strikethrough}` | Strikethrough |
| `{obfuscated}` / `{ofus}` | Obfuscated |
| `{reset}` | Clears formatting |

❌ Hex colors (e.g. `#ffaa00`) are **not supported**.

---

## 🧠 Description Fallback System

If a specific mob has no custom description, Mob Journal checks in order:

1. `assets/journal/mobs_desc/<namespace>/<mob>.json`
2. `assets/journal/mobs_desc/<namespace>/default.json`
3. `assets/journal/mobs_desc/journal/default.json`
4. A hardcoded fallback description

Only **one** file is used — the first match wins. A per-mob file does not inherit anything from `default.json`, so include every stat line you want in it.

---

## 📄 Bundled Descriptions

The mod ships with descriptions for wolves, cats, parrots, horses, cows, sheep, chickens, pigs, rabbits and llamas. Any of them can be overridden by a resource pack using the same path.

Vanilla's per-mob food tags (`#minecraft:wolf_food` and friends) only exist in 1.21+, so the 1.20.1 build's bundled descriptions reference items directly instead.

---

## 🧪 Testing Tips

- Press **F3 + T** in-game to reload resource packs and apply description changes.
- Descriptions are **client-side** and can ship in resource packs, mods, or modpacks.
- An unknown **item** ID renders as a gray `❓` marker so mistakes are visible. An unknown **texture** path draws nothing, so double-check texture paths.

---

📦 **Perfect for modpacks**
📖 **Great for storytelling**
💾 **Lightweight, readable, and moddable**
