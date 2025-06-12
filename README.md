# EnderaLib

![JitPack](https://jitpack.io/v/org.endera.enderalib/enderalib.svg)
![License](https://img.shields.io/badge/license-MIT-green.svg)

A Kotlin **utility library & Bukkit/Folia plugin** that bundles common functionality my projects depend on:

* 🎨 Kyori Adventure MiniMessage helpers
* ⚙️ Type–safe configuration loader powered by Kotlinx‐serialization
* 🌐 Ktor HTTP client pre-configured for plugins
* 🗄️ Exposed ORM & HikariCP helpers for database access
* 📊 bStats integration
* 🧩 Assorted utilities (permissions, pagination, async tasks, etc.)

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
    implementation("org.endera.enderalib:enderalib:x.y.z")
}
```

A shaded JAR (`enderalib-x.y.z-shaded.jar`) is also created by the build if you need to relocate the library into your
own plugin.

---

## Using as a standalone plugin

Just drop the compiled `enderalib-x.y.z.jar` in your server’s `plugins` folder. The plugin adds a single command:

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
