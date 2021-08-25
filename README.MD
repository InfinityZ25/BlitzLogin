# BlitzLogin
BlitzLogin is an authentication plugin for Velocity which automatically logs in premium users.

At the moment this is one of the few mixed auth plugins that exist, the others being:
- LockLogin (mixed auth only works on bungeecord)
- JPremium (not free)
- Nyx ULTIMATE (not free)
- PremiumConnector (bungeecord only, abandoned)
- FastLogin (planned Velocity support)

### ❗ This plugin is still in early development ❗
Please report any bugs you encounter.
Any contributions or suggestions would be appreciated

## Quick setup
Just drag and drop the JAR file into the `plugins` folder of Velocity **and** of the Spigot-based servers you wish unauthenticated players to connect to in order to login or register

You also need to specify in the Velocity plugin config the `main` and `limbo` servers. Note that these servers can coincide, but it is recommended to use different ones.

Unauthenticated players will be connected to a `limbo` and authenticated players to a `main` one.

## Features
- Force online-mode for premium players
- Allow cracked players with premium nicknames to join (second attempt)
- Disable movement and interactions for unauthenticated players

## Planned additions
- Custom messages
- Support for premium nickname change
- Support for newly created premium accounts that use the nickname of a cracked player which already registered on the server
- Allow premium players to join via a reserved domain name
- 2FA
- Email password recovery
- Support for Floodgate (this is not a priority ATM)