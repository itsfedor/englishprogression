# EnglishProgression

Turns lifetime earnings into English level-ups. The plugin tracks how much
money a player has earned in total (not spent, earned), compares it against
per-level thresholds, and promotes the player along a LuckPerms track when
they cross a threshold. Each level also raises the player's earnings
multiplier, so better English earns faster.

Part of a gamified ESL teaching setup: chat is the lesson, money is the motivation. The other plugins live under the [ESL Automation Suite](https://github.com/itsfedor/esl-automation-suite).

## Requirements

- Vault (any Vault economy provider works, VaultUnlocked included)
- LuckPerms (for the track and promotion)
- Optional but natural: Chat2Earn, which feeds the economy

## Level thresholds

| Level | Lifetime earnings | Multiplier |
|---|---|---|
| A0 | start | ×1 |
| A1 | $100 | ×2 |
| A2 | $1,000 | ×3 |
| B1 | $10,000 | ×4 |
| B2 | $50,000 | ×5 |
| C1 | $200,000 | ×5 |
| C2 | $500,000 | ×5 |
| D1 | $1,000,000 | ×5 |

The multiplier applies to whatever the economy pays the player. Reaching
D1 takes a million dollars of lifetime earnings, which on a real server is
months of daily English.

## Setup

1. Create the LuckPerms track named `english` with groups in order:
   `a0 a1 a2 b1 b2 c1 c2 d1`.
2. Copy `config.example.yml` from the [repo](https://github.com/itsfedor/englishprogression) to `config.yml`.
3. Restart the server.

On level-up the plugin promotes the player and sends a level-up message
with the new multiplier. Messages are configurable with color codes.

## License

MIT. Source code is in the [repository](https://github.com/itsfedor/englishprogression).
