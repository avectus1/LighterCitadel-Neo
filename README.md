# LighterCitadel Neo

A NeoForge 1.21.1 port of [LighterCitadel](https://github.com/shuttheirprayer/LighterCitadel) by Misanthropy.

All credit for the original mod goes to Misanthropy. This is an unofficial port and is not affiliated with Misanthropy or the Citadel authors.

LighterCitadel is an optimization and bug fix mod for Citadel. Every patch keeps Citadel's behaviour the same.

- skips Citadel's tick rate check for every entity every tick when no tick rate modifier is active
- caches Citadel's saved data and client tracker lookups
- faster master tick, modifier queries and sound pitch when nothing is scheduled
- lighter tooltip hover animations, and fixes Citadel's hover map growing forever
- skips the patreon follower render work for players that aren't patreons
- removes unloaded entities from Citadel's tick list so they can't keep a world in memory
- optional: skip Citadel's renderer events when nothing listens to them (off by default)
- optional: turn off patreon followers (off by default)

## Changes from the original

- ported to NeoForge 1.21.1 and Citadel 2.7
- fixes the hover animation in Citadel's 1.21.1 version jumping instead of animating smoothly
- renderer event skipping rewritten for NeoForge's event bus

## Requirements

- NeoForge 1.21.1
- Citadel 2.7.0+

## Config

`config/citadel-optimizer.toml`, every patch can be turned off. Needs a restart.

## Building

Put `citadel-2.7.0-1.21.1.jar` in `libs/`, then run `gradle build`.
