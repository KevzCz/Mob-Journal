# 📘 Mob Journal – Custom Mob Descriptions

Mob Journal supports **custom descriptions** for any mob using simple, readable **Markdown** files. This makes it easy for modpack creators or players to personalize lore, stats, or fun facts about their favorite mobs.

---

## 🛠️ How to Add Custom Mob Descriptions

To customize or add descriptions:

1. Go to your Minecraft instance's resource pack or data pack folder.
2. Navigate (or create) the path:

   ```
   assets/journal/mobs_desc/<namespace>/<mob_id>.json
   ```

   Example:
   ```
   assets/journal/mobs_desc/minecraft/zombie.json
   ```

3. Inside the `.json` file, use the following format:

```json
{
  "description": [
    "## The Zombie",
    "A **hostile** mob that *burns in sunlight*.",
    "",
    "Health: {getHealth}",
    "Armor: {getArmor}",
    "",
    "You have killed this mob {getTimesKilled} times.",
    "It has killed you {getTimesDiedTo} times.",
    "",
    "**Drops:**",
    "{getLootDrops}"
  ]
}
```

---

## 🧩 Variables

You can use the following variables in your text:

| Variable | Description |
|---------|-------------|
| `{mobName}` | The mob's display name |
| `{getHealth}` | Max health of the mob |
| `{getArmor}` | Armor value |
| `{getLootDrops}` | Automatically filled with known drops |
| `{getTimesKilled}` | Times you've killed this mob |
| `{getTimesDiedTo}` | Times this mob killed you |
| `{namespace}` | Mob's namespace (e.g., `minecraft`) |
| `{path}` | Mob's path (e.g., `zombie`) |

---

## ✨ Markdown Formatting

| Feature | Syntax | Example |
|--------|--------|---------|
| Bold | `**text**` | **bold** |
| Italic | `*text*` | *italic* |
| Headers | `## Title` | ## Title |
| Tooltip | `[word](hover:Tooltip here)` | [hover me](hover:Hi!) |

---

## 🎨 Icons & Textures

You can embed items and textures using this inline syntax:

```
[Label](item:namespace:item_id scale=1.0 "Optional tooltip")
[Label](texture:namespace:path/to/image.png scale=1.0 "Optional tooltip")
```

Examples:

```text
[Diamond](item:minecraft:diamond scale=1.0 "A precious gem")
[Custom Texture](texture:yourmodid:textures/gui/icon.png scale=1.0 "Icon!")
```

---

## 🧠 Fallback System

Mob Journal will fall back to:

1. `assets/journal/mobs_desc/<namespace>/default.json`
2. `assets/journal/mobs_desc/journal/default.json`
3. Hardcoded basic stats if nothing is found.

---

## 🧪 Testing Tips

- Press `F3 + T` to reload your resource pack and see changes immediately.
- Descriptions are read from the client resources (meaning you can bundle them with resource packs).
- All Markdown and placeholders work in any fallback/default.json too!

---

📦 Great for modpacks.  
📖 Easy for storytellers.  
💾 Lightweight and customizable.

Happy journaling!
