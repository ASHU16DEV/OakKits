# OakKits - Minecraft Kits Plugin

## Overview
OakKits is a lightweight, professional chat-based Kits plugin for Minecraft Spigot/Paper servers (1.16.5-1.21.10). It provides a complete kit management system with permission-based access, cooldowns, economy integration, GeyserMC/Bedrock support, and an admin panel.

**Version:** 1.0  
**Author:** ASHU16  
**Plugin Size:** ~83KB (optimized)

## Platform Support
| Platform | Support | How |
|----------|---------|-----|
| Java Edition | Direct | Spigot/Paper 1.16.5-1.21.10 |
| Bedrock Edition | Via GeyserMC | Install Floodgate plugin |
| Pocket Edition | Via GeyserMC | Same as Bedrock (merged) |

## Features

### Core Kit System
- Unlimited kits with customizable items, armor, enchantments, and lore
- Permission-based visibility (players only see kits they have access to)
- Smart tab completion showing only permitted kits
- Cooldown system with persistent storage
- One-time use kits option
- Optional command execution on kit claim

### Economy Integration
- Vault economy support
- Configurable kit costs
- Balance checking and deduction

### GeyserMC/Bedrock Support
- Automatic Bedrock player detection via Floodgate
- Native Bedrock Forms for mobile-friendly kit selection
- Touch-optimized menus for mobile players
- Mobile-friendly message formatting (simplified symbols)
- Kit preview forms for Bedrock players

### Professional UI
- Chat messages with color codes
- Action bar notifications for all actions
- Secure read-only preview GUI (Java) / Forms (Bedrock)
- Fully customizable messages and GUI design

### Admin Features
- Create kits from player inventory
- Set cooldowns, costs, and permissions
- Give kits to players (bypasses cooldown/cost)
- Reset player cooldowns
- Preview kits for players
- Hot reload all configurations
- **Kit Editor GUI** - Full in-game GUI editor with clickable buttons

### PlaceholderAPI Integration
- `%oakkits_total_kits%` - Total number of kits
- `%oakkits_available_kits%` - Kits player can access
- `%oakkits_kit_<id>_cooldown%` - Kit cooldown remaining
- `%oakkits_kit_<id>_cost%` - Kit cost
- `%oakkits_kit_<id>_permission%` - Kit permission
- `%oakkits_kit_<id>_available%` - If player can claim
- Plus many more placeholders

## Commands

### Player Commands
- `/kit` or `/kits` - List available kits (opens Form on Bedrock)
- `/kit <kitname>` - Claim a kit

### Admin Commands
- `/kit create <name>` - Create kit from inventory
- `/kit delete <name>` - Delete a kit
- `/kit give <player> <kit>` - Give kit to player
- `/kit setcooldown <kit> <time>` - Set kit cooldown (e.g., 1d, 12h, 30m)
- `/kit setcost <kit> <amount>` - Set kit cost
- `/kit setpermission <kit> <perm>` - Set kit permission
- `/kit resetcooldown <player> <kit>` - Reset player cooldown
- `/kit preview <kit> <player>` - Open preview for player
- `/kit edit <kit>` - Open GUI editor for kit
- `/kit reload` - Hot reload all configs

## Permissions
- `oakkits.use` - Use kit commands (default: true)
- `oakkits.kit.<kitname>` - Access to specific kit
- `oakkits.bypass.cooldown` - Bypass all cooldowns (OP players have this by default)
- `oakkits.admin` - All admin permissions
- `oakkits.admin.create` - Create kits
- `oakkits.admin.delete` - Delete kits
- `oakkits.admin.give` - Give kits
- `oakkits.admin.setcooldown` - Set cooldowns
- `oakkits.admin.setcost` - Set costs
- `oakkits.admin.setpermission` - Set permissions
- `oakkits.admin.resetcooldown` - Reset cooldowns
- `oakkits.admin.preview` - Preview kits
- `oakkits.admin.edit` - Edit kits via GUI
- `oakkits.admin.reload` - Reload configs

## Configuration Files

### config.yml
Main plugin settings including:
- Inventory handling (deny-if-full, drop-on-ground, auto-equip)
- Economy settings
- Console output options
- **Bedrock settings** (forms, mobile messages)

### kits.yml
Kit definitions with items, armor, enchantments, cooldowns, and costs

### messages.yml
All plugin messages with chat and action bar variants

### gui.yml
Preview GUI customization (borders, fillers, info items, layout)

### data.db (SQLite)
Player cooldown and one-time use data stored in SQLite database (auto-managed)

## Project Structure
```
src/main/java/com/oakkits/
├── OakKits.java           # Main plugin class
├── commands/
│   ├── KitCommand.java    # All command handling
│   └── KitTabCompleter.java # Smart tab completion
├── listeners/
│   └── GUIListener.java   # GUI protection
├── managers/
│   ├── ConfigManager.java
│   ├── MessagesManager.java
│   ├── GUIManager.java
│   ├── KitsManager.java
│   ├── DataManager.java
│   ├── EconomyManager.java
│   ├── BedrockManager.java # GeyserMC/Floodgate support
│   └── EditorGUIManager.java # Kit Editor GUI system
├── hooks/
│   └── PlaceholderAPIHook.java # PlaceholderAPI integration
├── models/
│   └── Kit.java           # Kit data model
└── utils/
    ├── ColorUtil.java     # Color code handling
    ├── TimeUtil.java      # Time parsing/formatting
    └── ActionBarUtil.java # Cross-version action bar
```

## Dependencies
- **Spigot API 1.16.5** - Core Minecraft API
- **Vault API** - Economy support (optional)
- **Floodgate API** - Bedrock player detection (optional)
- **Cumulus** - Bedrock Forms API (optional)
- **PlaceholderAPI** - Custom placeholders (optional)

## Building
Run `mvn clean package` to build. The JAR will be in `target/OakKits-1.0.jar`

## Installation
1. Build the plugin or download the JAR
2. Place in your server's `plugins` folder
3. (Optional) Install Floodgate for Bedrock support
4. Restart the server
5. Configure files in `plugins/OakKits/`

## Bedrock Setup
1. Install GeyserMC on your server
2. Install Floodgate plugin
3. OakKits will auto-detect and enable Bedrock features
4. Configure in `config.yml` under `bedrock:` section

## Recent Changes
- **Dec 1, 2025**: Editor GUI Overhaul & Auto-Save System
  - **CRITICAL FIX**: Cooldown/Cost buttons now work properly (isFillerOrBorder fixed)
  - **CRITICAL FIX**: All editor changes now auto-save (no need for manual Save button)
  - **CRITICAL FIX**: Color code buttons in display name editor now work and save properly
  - **NEW**: Config-based GUI system - all buttons load from editor-gui.yml
  - **NEW**: Hot reload support - change config and use /kit reload
  - **NEW**: Comprehensive error logging for config issues
  - **NEW**: Material fallback system for cross-version compatibility (1.16.5-1.21.10)
  - **IMPROVED**: Professional editor-gui.yml with full customization
  - **IMPROVED**: Null-safe config parsing with detailed warning logs
  - **IMPROVED**: Universal materials (stained glass panes) for all MC versions
  - **IMPROVED**: All handlers (items, armor, cooldown, cost, permission, commands, display name, one-time toggle) now persist changes instantly
  - Build size: ~90KB
- **Dec 1, 2025**: Major Bug Fixes & Large Server Optimization
  - **FIXED**: Chat input for commands/permission/name works correctly with scheduled close
  - **FIXED**: "No Permission (Public)" toggle - kits with empty permission visible to all
  - **FIXED**: One-time use button shows ON/OFF status in lore
  - **FIXED**: Cooldowns properly expire across server restarts (real-time based)
  - **FIXED**: Expired cooldowns cleaned up on plugin load (async)
  - **OPTIMIZED**: Thread-safe ConcurrentHashMaps for 50k+ player base
  - **OPTIMIZED**: All database operations synchronized with dbLock (init, load, save, cleanup, shutdown)
  - **OPTIMIZED**: prepareForChatInput uses Bukkit.runTask for proper event ordering
  - **OPTIMIZED**: Session lifecycle with clearSession/clearFullSession separation
  - **IMPROVED**: Professional GUI styling with cleaner separators and status indicators
  - Build size: ~86KB
- **Dec 1, 2025**: Bug fixes & SQLite database
  - **FIXED**: Editor GUI buttons no longer draggable - all items stay in place
  - **FIXED**: Menu transition guard prevents session loss when switching sub-menus
  - **FIXED**: One-time toggle no longer breaks GUI protection after refresh
  - **FIXED**: Filler blocks (glass panes) no longer play click sound
  - **FIXED**: Actual buttons now play UI click sound
  - **FIXED**: Cooldown data cleared when kit is deleted (no more ghost cooldowns)
  - **FIXED**: OP players now bypass all cooldowns automatically
  - **NEW**: SQLite database for player data (faster, more reliable)
  - **NEW**: Permission `oakkits.bypass.cooldown` for cooldown bypass
  - Build size: ~84KB
- **Dec 1, 2025**: Major feature update - PlaceholderAPI & Kit Editor GUI
  - Added PlaceholderAPI integration with 15+ custom placeholders
  - Created comprehensive Kit Editor GUI system with 8 sub-menus:
    - Main Editor, Items Editor, Armor Editor, Cooldown Editor
    - Cost Editor, Permission Editor, Display Name Editor, Commands Editor
  - All GUI interactions use clickable buttons only (no item dragging)
  - Added editor-gui.yml for full customization of editor GUI
  - Added async/batched saving for zero-lag performance
  - Fixed color codes in kit display names and creation command
  - Added new config settings for inventory, economy, cooldowns, notifications
  - Professional action bar notifications for all actions
  - Build size: ~83KB
- **Nov 30, 2025**: Added GeyserMC/Bedrock support
  - Floodgate integration for Bedrock player detection
  - Native Bedrock Forms for mobile-friendly kit selection
  - Mobile-optimized message formatting
  - Kit preview forms for Bedrock players
  - Config options for all Bedrock features
- **Nov 30, 2025**: Initial release v1.0
  - Complete kit system with all planned features
  - Smart tab completion with permission filtering
  - Action bar messages for all actions
  - Secure preview GUI with drag protection
  - Hot reload support for all configs
  - Vault economy integration
  - Optimized build (56KB)

## User Preferences
- Professional and clean UI design
- Colored console messages
- Lightweight plugin size
- Full customization of all text and GUI elements
- GeyserMC compatible for Bedrock/Pocket Edition players
