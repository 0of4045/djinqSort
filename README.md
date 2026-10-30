# djinqSort

A small and lightweight inventory sorting plugin for Paper servers.

Sort your main inventory with commands, or supported containers with Shift + right-click or middle-click. Sorting keeps item metadata intact and never touches your hotbar, armor, or offhand.

Made by djinq ~ [djinq.dev](https://djinq.dev)

## Installation

1. Drop `djinqSort.jar` into your server's `plugins` folder.
2. Start the server once.
3. Edit `plugins/djinqSort/config.yml` and `messages.yml` however you want.
4. Run `/sort reload` or restart the server.

## Commands

| Command | Description |
| --- | --- |
| `/sort` | Sorts the player's main inventory. |
| `/sort mode <mode>` | Changes the player's sorting mode. |
| `/sort reload` | Reloads the configuration and messages. |

## Permissions

| Permission | Description |
| --- | --- |
| `djinqsort.command` | Allows `/sort`. |
| `djinqsort.mode` | Allows `/sort mode`. |
| `djinqsort.middleclick` | Allows middle-click sorting. |
| `djinqsort.containerclick` | Allows Shift + right-click container sorting. |
| `djinqsort.reload` | Allows `/sort reload`. |

## Configuration

In `config.yml`, you can edit:

- `default-mode` ~ the default sorting mode for new players.
- `reverse-order` ~ whether sorting starts in reverse order.
- `player-inventory.enabled` ~ enables or disables main inventory sorting.
- `containers` ~ which standard storage containers can be sorted.
- `triggers` ~ enables each container shortcut separately.
- `join-hint` ~ controls the delayed container sorting hint.

`messages.yml` contains one `prefix` setting. It is automatically added to command messages, while click sorting stays quiet.

Targets Paper 1.21.11 through 26.3. Use Java 21 for Paper 1.21.11, or Java 25 for Paper 26.1+.

Compiled against Paper 1.21.11. Test newer Paper versions on a real server before using them in production.
