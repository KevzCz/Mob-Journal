




# 📘 Mob Journal – Custom Mob Descriptions

**Mob Journal** supports fully customizable descriptions for mobs using simple, readable **Markdown** in JSON. Great for modpack creators, lore builders, or anyone wanting to add flavor and detail to mobs.

---

## 🛠️ How to Add Custom Mob Descriptions

1. Open your Minecraft instance folder.
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
    "*The undead menace that groans in the night... {ofus}aaaa{reset}*",
    "",
    "{red}Health:{reset} {getHealth}",
    "{gray}Armor:{reset} {getArmor}",
    "",
    "[Hello!](hover:This is a tooltip shown when hovered!)",
    "",
    "**Combat Stats**",
    "- You have slain this mob {getTimesKilled} times.",
    "- It has slain you {getTimesDiedTo} times.",
    "",
    "**Drops:**",
    "{getLootDrops}",
    "",
    "**Treasure Example:**",
    "[Diamond](item:minecraft:diamond scale=1.0 \"{gold}A precious gem{reset}\")",
    "[Rotten Flesh](item:minecraft:rotten_flesh scale=1.0 \"{red}Smells bad{reset}\")",
    "[item](item:minecraft:diamond_sword scale=2.0 \"{light_purple}Epic weapon!{reset}\")",
    "",
    "Some text before the textures: [Sword](texture:journal:icon.png scale=1.0 \"Sword icon\") [Shield](texture:journal:icon.png scale=1.0 \"Shield icon\")",
    "Even more items: [Rotten Flesh](item:minecraft:rotten_flesh scale=1.0) [Rotten Flesh](item:minecraft:rotten_flesh scale=1.0)",
    "",
    "**Texture Example:**",
    "[Logo](texture:journal:icon.png scale=1.0 \"{aqua}Custom icon from your mod{reset}\")",
    "",
    "{gold}Tip:{reset} You can customize this description using markdown!",
    "",
    "{italic}Fun Fact:{reset} You can use *italics*, **bold**, and {dark_purple}colors{reset} too!"
  ]
}
```
![image](https://github.com/user-attachments/assets/188c1ae7-9b67-4a2a-a8e4-8d66498d0d56)
![image](https://github.com/user-attachments/assets/0d1cf9d6-60e8-4b49-8c82-2bb80e62da19)
![image](https://github.com/user-attachments/assets/ef5b8ec4-b026-48de-8cad-fce54e5d2c0f)
![image](https://github.com/user-attachments/assets/41d2b03d-fa16-4ca8-8e2e-f8b4cd74967f)
---

## 🧩 Supported Variables

| Variable           | Description                            |
|--------------------|----------------------------------------|
| `{mobName}`        | The display name of the mob            |
| `{getHealth}`      | Max health of the mob                  |
| `{getArmor}`       | Armor value                            |
| `{getLootDrops}`   | Automatically shows known drops        |
| `{getTimesKilled}` | Times the player has slain the mob     |
| `{getTimesDiedTo}` | Times the mob has slain the player     |
| `{namespace}`      | The namespace of the mob's ID          |

---

## ✨ Markdown & Tooltip Features

| Feature   | Syntax              | Example                    |
|-----------|---------------------|----------------------------|
| Bold      | `**text**`          | `**bold**`                 |
| Italic    | `*text*`            | `*italic*`                 |
| Heading   | `## Title`          | `## Mob Info`              |
| Tooltip   | `[word](hover:tip)` | `[hover me](hover:Hi!)`    |

✅ Tooltips appear in-game on hover.

Items without tooltips will be using Minecraft's tooltip

---

## 🖼️ Items & Textures

You can embed **items** and **textures** inline.

**Item Example:**

```
[item](item:namespace:item_id scale=1.0 "Optional Tooltip")
```

**Texture Example:**

```
[texture](texture:namespace:path/to/image.png scale=1.0 "Optional Tooltip")
```

✅ Both support custom scale and tooltip.

---

## 🎨 Color Codes

Use color codes like `{red}`, `{gold}`, and style codes like `{bold}`, `{italic}`.

| Code             | Minecraft Color |
|------------------|-----------------|
| `{black}`        | §0              |
| `{dark_blue}`    | §1              |
| `{dark_green}`   | §2              |
| `{dark_aqua}`    | §3              |
| `{dark_red}`     | §4              |
| `{dark_purple}`  | §5              |
| `{gold}`         | §6              |
| `{gray}`         | §7              |
| `{dark_gray}`    | §8              |
| `{blue}`         | §9              |
| `{green}`        | §a              |
| `{aqua}`         | §b              |
| `{red}`          | §c              |
| `{light_purple}` | §d              |
| `{yellow}`       | §e              |
| `{white}`        | §f              |

🎨 Use `{reset}` to clear formatting.

❌ Hex colors (e.g. `#ffaa00`) are **not supported**.

---

## 🧠 Description Fallback System

If a specific mob has no custom description, Mob Journal checks:

1. `assets/journal/mobs_desc/<namespace>/default.json`
2. `assets/journal/mobs_desc/journal/default.json`
3. A hardcoded fallback description

---

## 🧪 Testing Tips

- Press **F3 + T** in-game to reload your resource pack and apply description changes.
- Custom descriptions are **client-side** and can be included in:
  - Resource packs
  - Mods
  - Modpacks


📦 **Perfect for modpacks**  
📖 **Great for storytelling**  
💾 **Lightweight, readable, and moddable**
```
