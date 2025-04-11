# 📘 Mob Journal – Custom Mob Descriptions

Mob Journal supports **custom descriptions** for any mob using simple, readable **Markdown** files. This makes it easy for modpack creators or players to personalize lore, stats, or fun facts about their favorite mobs.

---

## 🛠️ How to Add Custom Mob Descriptions

To customize or add descriptions:

1. Go to your Minecraft instance's resource pack or data pack folder.
2. Navigate (or create) the path:

   assets/journal/mobs_desc/<mod_namespace>/<mob_name>.json

   Example:
   assets/journal/mobs_desc/minecraft/zombie.json

3. Inside the `.json` file, use the following example format:

```json
{
  "description": [
    "## {mobName}",
    "*The undead menace that groans in the night...*",
    "",
    "{§c}Health:{§r} {getHealth}",
    "{§7}Armor:{§r} {getArmor}",
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
    "[Diamond](item:minecraft:diamond scale=1.0 \"A precious gem\")",
    "[Rotten Flesh](item:minecraft:rotten_flesh scale=1.0 \"Smells bad\")",
    "[item](item:minecraft:diamond_sword scale=2)",
    "Some text1 [Sword](texture:journal:icon.png) [Shield](texture:journal:icon.png)\n",
    "Some text2 [Rotten Flesh](item:minecraft:rotten_flesh) [Rotten Flesh](item:minecraft:rotten_flesh)\n",
    "",
    "**Texture Example:**",
    "[Logo](texture:journal:icon.png scale=1.0 \"Custom icon from your mod\")",
    "",
    "{§6}Tip:{§r} You can customize this description using markdown!"
  ]
}
```
![image](https://github.com/user-attachments/assets/4d73d478-780d-4f41-9fd8-80ecd206223f)![image](https://github.com/user-attachments/assets/e6b37c5d-4a74-4067-a6f0-b294d88d61f8)
![image](https://github.com/user-attachments/assets/f4c11e9f-de29-4a52-bbd7-6fec00376619)![image](https://github.com/user-attachments/assets/ee8c8026-7bed-484a-b1dd-24a57d51e90d)







---

## 🧩 Variables

You can use the following variables in your text:

| Variable | Description |
|---------|-------------|
| {mobName} | The mob's display name |
| {getHealth} | Max health of the mob |
| {getArmor} | Armor value |
| {getLootDrops} | Automatically filled with known drops |
| {getTimesKilled} | Times you've killed this mob |
| {getTimesDiedTo} | Times this mob killed you |
| {namespace} | Mob's namespace (e.g., minecraft) |

---

## ✨ Markdown Formatting

| Feature | Syntax | Example |
|--------|--------|---------|
| Bold | **text** | **bold** |
| Italic | *text* | *italic* |
| Headers | ## Title | ## Title |
| Tooltip | [word](hover:Tooltip here) | [hover me](hover:Hi!) |

---

## 🎨 Icons & Textures

You can embed items and textures using this inline syntax:

[Label](item:namespace:item_id scale=1.0 "Optional tooltip")  
[Label](texture:namespace:path/to/image.png scale=1.0 "Optional tooltip")

Examples:

[Diamond](item:minecraft:diamond scale=1.0 "A precious gem")  
[Custom Texture](texture:yourmodid:textures/gui/icon.png scale=1.0 "Icon!")

✅ These support tooltips when hovered over in-game.

---

## 🎨 Color Codes

Mob Journal supports **Minecraft-style color codes** in descriptions using § formatting:

| Code | Color       |
|------|-------------|
| §0   | Black       |
| §1   | Dark Blue   |
| §2   | Dark Green  |
| §3   | Dark Aqua   |
| §4   | Dark Red    |
| §5   | Dark Purple |
| §6   | Gold        |
| §7   | Gray        |
| §8   | Dark Gray   |
| §9   | Blue        |
| §a   | Green       |
| §b   | Aqua        |
| §c   | Red         |
| §d   | Light Purple|
| §e   | Yellow      |
| §f   | White       |

You can also use inline codes like `{§a}` and it will be replaced with §a automatically.

Example:
"{§c}Warning:{§r} This mob is dangerous!"

✅ Note: Hex colors (#ffaa00) are not supported.

---

## 🧠 Fallback System

Mob Journal will fall back to:

1. `assets/journal/mobs_desc/<mod_namespace>/default.json`  
   - Shared fallback for that mod
2. `assets/journal/mobs_desc/journal/default.json`  
   - Shared fallback for everything
3. A hardcoded basic description and stat block

---

## 🧪 Testing Tips

- Press F3 + T to reload your resource pack and instantly refresh descriptions.
- Descriptions are stored client-side and can be bundled with a resource pack.

---

📦 Great for modpacks  
📖 Easy for storytellers  
💾 Lightweight and customizable
