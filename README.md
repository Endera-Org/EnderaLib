# EnderaLib

[![JitPack](https://jitpack.io/v/Endera-Org/EnderaLib.svg)](https://jitpack.io/#Endera-Org/EnderaLib)
![License](https://img.shields.io/badge/license-MIT-green.svg)

A Kotlin **utility library & Bukkit/Folia plugin** that bundles common functionality my projects depend on:

* 🎨 Kyori Adventure MiniMessage helpers
* ⚙️ Type–safe configuration loader powered by Kotlinx‐serialization
* 🌐 Ktor HTTP client pre-configured for plugins
* 🗄️ Exposed ORM & HikariCP helpers for database access
* 📊 bStats integration
* 🧩 Assorted utilities (permissions, menu items, async tasks, etc.)

The goal is to remove boiler-plate from Spigot/Paper/Folia plugin development and keep all shared code in a single,
versioned place.

---

## Getting the library

The artefacts are published on **JitPack**. Simply add the repository and dependency to your build file.

### Gradle Kotlin DSL

```kotlin
repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    // Replace x.y.z with the version shown on the badge above
    compileOnly("com.github.Endera-Org:EnderaLib:x.y.z")
}
```

EnderaLib runs as a plugin on the server, so depend on it with `compileOnly` and add it to your `plugin.yml`:

```yaml
depend: [EnderaLib]
```

The plugin JAR stays small because its runtime dependencies are declared through Minecraft's built-in library loader.
Paper, Folia, and current Spigot servers download those dependencies from Maven Central when the plugin is first loaded.

---

## Using as a standalone plugin

Just drop the compiled `EnderaLib-x.y.z.jar` in your server’s `plugins` folder. The plugin adds a single command:

| Command | Description |
|---------|-------------|
| `/enderalib version` | Shows the currently loaded EnderaLib version |

No further configuration is required. A default `config.yml` is generated on first start containing messages that can be
customised.

---

## Quick examples

#### Send coloured MiniMessage text

```kotlin
sender.sendMessage("<green>Hello <yellow>world!".stringToComponent())
```

#### Load or create a typed configuration

```kotlin
val myConfig = ConfigurationManager(
    configFile  = File(dataFolder, "config.yml"),
    dataFolder  = dataFolder,
    defaultConfig = defaultConfig,
    serializer   = ConfigScheme.serializer(),
    logger       = logger,
    clazz        = ConfigScheme::class
).loadOrCreateConfig()
```

#### Report plugin stats to bStats

```kotlin
override fun onEnable() {
    MetricsLite(this, pluginId)
}
```

Create it while the plugin is enabled, for example in `onEnable`. Reporting stops when the plugin is disabled.

#### Switch between IO and Bukkit/Folia threads in suspend code

```kotlin
plugin.coroutines.launch {
    val async = plugin.coroutines

    val profile = async.withIo {
        profileRepository.load(player.uniqueId)
    }

    async.withEntity(player) {
        player.sendMessage("<green>Loaded ${profile.name}".stringToComponent())
    }
}
```

Or, if you already know the starting context:

```kotlin
plugin.coroutines.launchIo {
    val profile = profileRepository.load(player.uniqueId)

    plugin.coroutines.withEntity(player) {
        player.sendMessage("<green>Loaded ${profile.name}".stringToComponent())
    }
}
```

---

## Building from source

Clone the repository and run:

```bash
./gradlew build
```

The final artefacts will be inside `build/libs`.

*Requires JDK 17*.

---

## Contributing

Pull requests and issue reports are welcome!  Please open an issue first if you want to discuss a big change.

1. Fork the project
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a pull request

---

## License

This project is licensed under the MIT License – see the [LICENSE](LICENSE) file for details.
