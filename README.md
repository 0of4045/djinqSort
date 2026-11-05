# djinqSort

A small and lightweight inventory sorting plugin for Paper servers.

Sort the 27 main inventory slots with commands, or sort an open supported container with middle-click. You can also sort supported containers with Shift + right-click. Sorting keeps item metadata intact and never touches the 9 hotbar slots, armor, or offhand.

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

Built for Paper 1.21.11 through 26.3. Use Java 21 for Paper 1.21.11, or Java 25 for Paper 26.1+.
