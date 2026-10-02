// versions
// https://parchmentmc.org/docs/getting-started
val parchmentVersion = "2024.11.17"
// https://fabricmc.net/develop/
val minecraftVersion = "1.21.1"
val loaderVersion = "0.19.5"
val fapiVersion = "0.115.1+1.21.1"

// in-house dependencies
val flywheelVersion = "1.0.1-11"
val ponderVersion = "1.0.44"
val registrateVersion = "1.3.77-MC1.21.1"
val milkLibVersion = "1.1.0-patch+1.21.1"
// https://mvn.devos.one/#/snapshots/io/github/fabricators_of_create/Porting-Lib
val portingLibVersion = "3.1.0-beta.91+1.21.1"
val portingLibExtensionsVersion = "3.1.0-beta.54+1.21.1"
val portingLibModules = listOf(
    "base", "blocks", "brewing", "client_events", "common", "core", "data",
    "entity", "fluids", "items", "level_events", "mixin_extensions", "models", "obj_loader", "resources", "tags",
    "transfer"
)
// "conditions" and "accessors" aren't published at the same version as the rest of porting-lib (confirmed via
// each module's own maven-metadata.xml: beta.91+ was never published for either at 1.21.1); pin them separately.
val portingLibConditionsVersion = "3.1.0-beta.47+1.21.1"
val portingLibAccessorsVersion = "3.1.0-beta.54+1.21.1"

// external dependencies
val configApiVersion = "21.1.3"
val nightConfigVersion =  "3.6.3"
val jsr305Version = "3.0.2"

// compat
// https://modrinth.com/mod/cc-tweaked/versions
val ccVersion = "1.115.1"
// for CC - https://modrinth.com/mod/cloth-config/versions
val clothVersion = "15.0.140+fabric"
// https://modrinth.com/mod/jei/versions
val jeiVersion = "19.21.0.247"
// https://modrinth.com/mod/rei/versions
val reiVersion = "16.0.799"
// https://modrinth.com/mod/emi/versions
val emiVersion = "1.1.20+1.21.1"
// https://modrinth.com/mod/botania
val botaniaVersion = "1.19.2-436-FABRIC"
// https://modrinth.com/mod/modmenu/versions
val modmenuVersion = "11.0.3"
// https://modrinth.com/mod/sandwichable/versions
val sandwichableVersion = "1.3.1+1.20.1"
// https://modrinth.com/mod/farmers-delight-refabricated/versions
// NOTE: versions after 3.2.5 mis-declare their "accessWidener" fabric.mod.json field as a v2 ClassTweaker file,
// which Loom's AccessWidenerJarProcessor can't parse (upstream packaging bug) - pinned to the last known-good build.
val farmersDelightVersion = "1.21.1-3.2.5"
// https://modrinth.com/mod/sodium
val sodiumVersion = "mc1.21.1-0.6.9-fabric"
// https://github.com/emilyploszaj/trinkets/releases/
val trinketsVersion = "3.10.0"
// for Trinkets - https://modrinth.com/mod/cardinal-components-api/versions
val ccaVersion = "6.1.2"
// https://modrinth.com/mod/journeymap
val jmVersion = "1.21.1-6.0.0-beta.39+fabric"
// https://modrinth.com/mod/xaeros-minimap/versions
val xaerosMinimapVersion = "fabric-1.21.1-26.5.0"
// https://modrinth.com/mod/xaeros-world-map/versions
val xaerosWorldMapVersion = "fabric-1.21.1-1.46.0"
// https://chocolateminecraft.com/maven/xaero/lib/xaerolib-fabric-1.21.1/ - required transitively by
// both xaero mods above (xaero.lib.client.gui.ScreenBase etc.), not published on Modrinth for fabric
val xaeroLibVersion = "1.7.3"
// check the jm jar, it's JiJ
val jmApiVersion = "1.20-1.9-SNAPSHOT"

// dev stuff
val ccRuntime = false
val recipeViewer = "emi" // jei, rei, or emi

plugins {
    id("fabric-loom") version "1.13.+"
    id("maven-publish")
}

val buildNum = providers.environmentVariable("GITHUB_RUN_NUMBER")
    .filter(String::isNotEmpty)
    .map { "-build.$it" }
    .orElse("-local")
    .getOrElse("")

version = "6.0.0.0+mc$minecraftVersion$buildNum"

group = "com.simibubi.create"
base.archivesName = "create-fabric"

repositories {
    maven("https://maven.parchmentmc.org") // Parchment
    maven("https://maven.fabricmc.net") // FAPI, Loader
    maven("https://maven.createmod.net") // Ponder, Flywheel
    maven("https://mvn.devos.one/snapshots") // Registrate, Forge Tags, Milk Lib, Porting Lib
    maven("https://mvn.devos.one/releases") // Porting Lib releases
    maven("https://raw.githubusercontent.com/Fuzss/modresources/main/maven") // Forge Config API Port
    maven("https://maven.shedaniel.me") // REI and deps
    maven("https://api.modrinth.com/maven") { // LazyDFU, Sodium, Sandwichable
        content { includeGroupAndSubgroups("maven.modrinth") }
    }
    maven("https://maven.terraformersmc.com") // Mod Menu, Trinkets
    maven("https://maven.squiddev.cc") // CC:T
    maven("https://modmaven.dev") // Botania
    maven("https://maven.ladysnake.org/releases") // CCA, for Trinkets
    maven("https://maven.saps.dev/releases") // FTB
    maven("https://maven.architectury.dev") // Architectury API
    maven("https://jm.gserv.me/repository/maven-public/") // Journey map
    maven("https://chocolateminecraft.com/maven") // XaeroLib, required by Xaero's Minimap/World Map
}

val ponder = file("Ponder")

dependencies {
    // setup
    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings(loom.layered {
        officialMojangMappings { nameSyntheticMembers = false }
        parchment("org.parchmentmc.data:parchment-$minecraftVersion:$parchmentVersion@zip")
    })
    modImplementation("net.fabricmc:fabric-loader:$loaderVersion")

    // dependencies
    modImplementation("net.fabricmc.fabric-api:fabric-api:$fapiVersion")

    modApi(include("com.tterrag.registrate_fabric:Registrate:$registrateVersion")!!)

    modApi(include("com.electronwill.night-config:core:$nightConfigVersion")!!)
    modApi(include("com.electronwill.night-config:toml:$nightConfigVersion")!!)
    modApi(include("fuzs.forgeconfigapiport:forgeconfigapiport-fabric:$configApiVersion")!!)
    modApi(include("dev.engine-room.flywheel:flywheel-fabric-$minecraftVersion:$flywheelVersion")!!)
    modApi(include("maven.modrinth:milk-lib:$milkLibVersion")!!)
    api(include("com.google.code.findbugs:jsr305:$jsr305Version")!!)

    for (module in portingLibModules) {
        modApi(include("io.github.fabricators_of_create.Porting-Lib:$module:$portingLibVersion")!!)
    }
    modApi(include("io.github.fabricators_of_create.Porting-Lib:extensions:$portingLibExtensionsVersion")!!)
    modApi(include("io.github.fabricators_of_create.Porting-Lib:conditions:$portingLibConditionsVersion")!!)
    modApi(include("io.github.fabricators_of_create.Porting-Lib:accessors:$portingLibAccessorsVersion")!!)

    if (ponder.exists()) {
        implementation("net.createmod.ponder:Ponder-Fabric-$minecraftVersion:$ponderVersion") { isTransitive = false }
        implementation("net.createmod.ponder:Ponder-Common-$minecraftVersion:$ponderVersion")
    } else {
        modRuntimeOnly(include("net.createmod.ponder:Ponder-Fabric-$minecraftVersion:$ponderVersion")!!)
        modCompileOnly("net.createmod.ponder:Ponder-Fabric-$minecraftVersion:$ponderVersion") {
            exclude(group = "io.github.fabricators_of_create.Porting-Lib")
        }
    }

    // compat
    modCompileOnly("cc.tweaked:cc-tweaked-$minecraftVersion-fabric-api:$ccVersion")

    modCompileOnly("vazkii.botania:Botania:$botaniaVersion") { isTransitive = false }
    modCompileOnly("com.terraformersmc:modmenu:$modmenuVersion")
    modCompileOnly("maven.modrinth:sandwichable:$sandwichableVersion")
    modCompileOnly("maven.modrinth:farmers-delight-refabricated:$farmersDelightVersion")
    modCompileOnly("maven.modrinth:sodium:$sodiumVersion")
    modCompileOnly("maven.modrinth:xaeros-minimap:$xaerosMinimapVersion")
    modCompileOnly("maven.modrinth:xaeros-world-map:$xaerosWorldMapVersion")
    modCompileOnly("xaero.lib:xaerolib-fabric-1.21.1:$xaeroLibVersion")

    modCompileOnly("dev.emi:trinkets:$trinketsVersion")
    // for Trinkets
    modCompileOnly("dev.onyxstudios.cardinal-components-api:cardinal-components-base:$ccaVersion")
    modCompileOnly("dev.onyxstudios.cardinal-components-api:cardinal-components-entity:$ccaVersion")

    // FIXME - Use gradle.properties for these versions, make change to concealed for this
    modCompileOnly("dev.architectury:architectury-fabric:9.1.12")
    // FIXME fabric-port: these dev.ftb.mods:*-fabric coordinates/versions don't exist on any configured
    // repository (404 on maven.squiddev.cc) - stale pin from before this ever resolved. FTB Chunks/Teams/Library
    // compat (FTBIntegration.java, FTBChunksTrainMap.java) needs the correct current maven + version.
    // modCompileOnly("dev.ftb.mods:ftb-chunks-fabric:2001.3.1")
    // modCompileOnly("dev.ftb.mods:ftb-teams-fabric:2001.3.0")
    // modCompileOnly("dev.ftb.mods:ftb-library-fabric:2001.2.4")

    modCompileOnly("maven.modrinth:journeymap:$jmVersion")
    modCompileOnly("info.journeymap:journeymap-api:$jmApiVersion")

    // EMI
    modCompileOnly("dev.emi:emi-fabric:$emiVersion:api") { isTransitive = false }
    // JEI
    modCompileOnly("mezz.jei:jei-$minecraftVersion-fabric:$jeiVersion") { isTransitive = false }
    // REI
    modCompileOnly("me.shedaniel:RoughlyEnoughItems-api-fabric:$reiVersion")
    modCompileOnly("me.shedaniel:RoughlyEnoughItems-default-plugin-fabric:$reiVersion")

    when (recipeViewer) {
        "jei" -> modLocalRuntime("mezz.jei:jei-$minecraftVersion-fabric:$jeiVersion")
        "rei" -> modLocalRuntime("me.shedaniel:RoughlyEnoughItems-fabric:$reiVersion")
        "emi" -> modLocalRuntime("dev.emi:emi-fabric:$emiVersion")
    }

    // dev env
    modLocalRuntime("com.terraformersmc:modmenu:$modmenuVersion")
    modLocalRuntime("dev.emi:trinkets:$trinketsVersion") { isTransitive = false }
    // for Trinkets
    modLocalRuntime("dev.onyxstudios.cardinal-components-api:cardinal-components-base:$ccaVersion")
    modLocalRuntime("dev.onyxstudios.cardinal-components-api:cardinal-components-entity:$ccaVersion")
    if (ccRuntime) {
        modLocalRuntime("cc.tweaked:cc-tweaked-$minecraftVersion-fabric:$ccVersion")
        modLocalRuntime("maven.modrinth:cloth-config:$clothVersion")
    }
    // have deprecated modules present at runtime only
    modLocalRuntime("net.fabricmc.fabric-api:fabric-api-deprecated:$fapiVersion")
}

sourceSets.named("main") {
    resources {
        srcDir("src/generated/resources")
        exclude(".cache/")
    }
}

loom {
    accessWidenerPath = file("src/main/resources/create.accesswidener")

    runs {
        register("datagen") {
            client()
            name("Data Generation")
            vmArg("-Dfabric-api.datagen")
            vmArg("-Dfabric-api.datagen.output-dir=${file("src/generated/resources")}")
            vmArg("-Dfabric-api.datagen.modid=create")
        }

        register("gametestServer") {
            server()
            name("Headlesss GameTests")
            ideConfigGenerated(false) // this run is for CI
            vmArg("-Dfabric-api.gametest")
            vmArg("-Dfabric-api.gametest.report-file=${layout.buildDirectory}/junit.xml")
            runDir("run/gametest")
        }

        named("server") {
            runDir("run/server")
        }

        configureEach {
            vmArg("-XX:+AllowEnhancedClassRedefinition")
            vmArg("-XX:+IgnoreUnrecognizedVMOptions")
            property("mixin.debug.export", "true")
        }
    }
}

configurations {
    // this avoids remapping ponder when it's local
    named("runtimeClasspath") {
        attributes {
            attribute(Attribute.of("create.marker", String::class.java), "h")
        }
    }
}

tasks.named<ProcessResources>("processResources") {
    exclude("**/*.bbmodel", "**/*.lnk")

    val properties: MutableMap<String, Any> = mutableMapOf(
        "version" to version,
        "minecraft_version" to minecraftVersion,
        "loader_version" to loaderVersion,
        "fabric_version" to fapiVersion,
        "forge_config_version" to configApiVersion,
        "port_lib_base_version" to portingLibVersion,
        "port_lib_client_events_version" to portingLibVersion,
        "port_lib_entity_version" to portingLibVersion,
        "port_lib_models_version" to portingLibVersion,
        "port_lib_obj_loader_version" to portingLibVersion,
        "port_lib_transfer_version" to portingLibVersion
    )

    inputs.properties(properties)

    filesMatching("fabric.mod.json") {
        expand(properties)
    }
}

java {
    withSourcesJar()
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.named<JavaCompile>("compileJava") {
    options.compilerArgs.add("-Xmaxerrs")
    options.compilerArgs.add("10000")
}

publishing {
    publications {
        register<MavenPublication>("mavenJava") {
            artifactId = "create-fabric-$minecraftVersion"
            from(components["java"])
        }
    }

    repositories {
        maven("https://mvn.devos.one/releases") {
            name = "devOsReleases"
            credentials(PasswordCredentials::class)
        }

        maven("https://mvn.devos.one/snapshots") {
            name = "devOsSnapshots"
            credentials(PasswordCredentials::class)
        }
    }
}
