# HolographicDisplays-Reborn

**Version 3.0.6** — Create floating text and item holograms on your Minecraft server.

HolographicDisplays-Reborn is a fork of the original [Holographic Displays](https://github.com/filoghost/HolographicDisplays) plugin by **filoghost** and its contributors.

This fork is maintained by **Rigqx** and focuses on bringing support to newer Minecraft versions without breaking existing features, commands, configurations, or API integrations.

This is an independent fork and is not affiliated with or officially supported by the original author.

## Features

- Create and manage text and item holograms in-game.
- Support for colors, animations, and placeholders.
- PlaceholderAPI integration.
- Support for the original Holographic Displays API.
- Keep your existing holograms and configuration when upgrading.
- Compatibility with both older and newer Minecraft versions.

## Supported Minecraft Versions

The original compatibility adapters are still included, covering supported releases from **1.8.4 to 1.20.4**. Not every version between these releases is supported.

The new compatibility system adds support for:

- **1.20:** 1.20.5, 1.20.6
- **1.21:** 1.21, 1.21.1, 1.21.3–1.21.11
- **26.x:** 26.1, 26.1.1, 26.1.2, 26.2, 26.3

Paper version formats such as `26.2.build.132-stable` are also recognized.

**Note:** Minecraft 1.21.2 is currently not supported.

Compatibility may vary depending on your server software and mapping version. 

The plugin has been checked using unit tests and packet validation, but full in-game testing is still recommended. Make sure to back up your holograms before updating.

## Installation

1. Stop your Minecraft server.
2. Back up your `plugins/HolographicDisplays/` folder.
3. Remove your old Holographic Displays JAR.
4. Download and place `HolographicDisplays-REBORN-3.0.6.jar` in your `plugins/` folder.
5. Install PlaceholderAPI if you want to use its placeholders.
6. Start your server.

**Important:** Don't run the original plugin and Reborn at the same time.

The internal plugin name is still `HolographicDisplays` to maintain compatibility with existing data and plugins. In the console, Reborn uses the prefix `HolographicDisplays-REBORN`.

The plugin is compiled for Java 8, but you'll still need to use whichever Java version your Minecraft server requires.

## Commands

The original commands are still available.

```text
/hd help
/hd create welcome &aWelcome to the server!
/hd reload
```

Use `/hd help` to see the full command list.

For more information about commands, configuration, and API usage, check out the [original Holographic Displays wiki](https://github.com/filoghost/HolographicDisplays/wiki).

## Building from Source

You'll need:

- **JDK 25**
- **Maven 3.6 or newer**

Clone or download the source code, open a terminal in the project folder, and run:

```bash
mvn package
```

Once the build finishes, you'll find the plugin JAR here:

```text
plugin/target/HolographicDisplays-REBORN-3.0.6.jar
```

If you're using IntelliJ IDEA, set your Maven runner to JDK 25 and run **Lifecycle → package** from the root project.

Make sure you keep the `plugin` module, since it contains the main plugin code, commands, resources, and final JAR assembly.

The project uses PlaceholderAPI 2.11.6 as a compile dependency, but it isn't included in the final JAR. Maven will also need to download the required server and NMS dependencies from the repositories configured in `pom.xml`.

## Using the API

Reborn keeps the original Holographic Displays API packages, so existing integrations can continue using them.

To use the API in your own project, first install it to your local Maven repository:

```bash
mvn install
```

Then add the dependency to your project.

**Maven**

```xml
<dependency>
    <groupId>me.filoghost.holographicdisplays</groupId>
    <artifactId>holographicdisplays-api</artifactId>
    <version>3.0.6</version>
    <scope>provided</scope>
</dependency>
```

**Gradle**

Make sure `mavenLocal()` is included in your repositories, then add:

```groovy
compileOnly 'me.filoghost.holographicdisplays:holographicdisplays-api:3.0.6'
```

The API is installed locally using these instructions. Version 3.0.6 is not necessarily available through the original project's Maven repository.

## Credits

A big thanks to **filoghost** and everyone who contributed to the original Holographic Displays project.

Without their work, this fork wouldn't exist.

**Original project:** [Holographic Displays](https://github.com/filoghost/HolographicDisplays)  
**Original author:** filoghost and contributors  
**Reborn maintainer:** Rigqx

Additional credits go to the developers of the libraries used by the project, including:

- bStats Metrics
- EisenNBT
- PacketWrapper

Other original libraries and their respective authors are listed in [THIRD-PARTY.txt](THIRD-PARTY.txt).

## License

HolographicDisplays-Reborn is released under the **GNU General Public License v3.0 or later (GPL-3.0-or-later)**, following the licensing terms of the original project.

The original copyright notices and license information have been kept.

For more information, see:

- [LICENSE.txt](LICENSE.txt)
- [NOTICE.txt](NOTICE.txt)
- [THIRD-PARTY.txt](THIRD-PARTY.txt)
- [licenses/](licenses/)

If you redistribute this plugin, you must follow the applicable license requirements, including keeping the required notices and making the complete corresponding source code and build files available for the binaries you distribute.

This software is provided without warranty, as described in the license.
