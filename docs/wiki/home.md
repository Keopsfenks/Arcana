# Arcana

Arcana is a Minecraft plugin framework maintained by Semih Gürbüz (Keopsfenks). It includes APIs for configuration, persistent data, commands, menus, custom items, crafting, entities, and server integration.

Start with the [project README](../../README.md) for builds, installation, dependencies, and migration information. Report problems in [Arcana Issues](https://github.com/Keopsfenks/Arcana/issues).

## Configuration and lookups

- [Data storage](general-config/data-storage.md)
- [Text formatting](general-config/text-formatting.md)
- [GUI pages](general-config/pages.md)
- [Sounds](general-config/sounds.md)
- [Math](general-config/math.md)
- [Item lookups](lookup-systems/the-item-lookup-system/the-item-lookup-system.md)
- [External item integrations](lookup-systems/the-item-lookup-system/external-item-integrations.md)
- [Recipes](lookup-systems/the-item-lookup-system/recipes.md)
- [Block lookups](lookup-systems/the-block-lookup-system.md)
- [Entity lookups](lookup-systems/the-entity-lookup-system.md)
- [Particle lookups](lookup-systems/the-particle-lookup-system.md)
- [Price lookups](lookup-systems/the-price-lookup-system.md)
- [Entity AI goals](custom-entity-ai/entity_ai_goals.md)
- [Supported integrations](what-plugins-are-supported.md)

Integration pages also describe external products. A documented integration does not make a precompiled plugin using the original framework API compatible with Arcana; consumer plugins must use Arcana's packages and dependency name.

The server API baseline is 1.21.8. Version-specific modules are listed in [settings.gradle.kts](../../settings.gradle.kts). Check a target server with your plugin set before deployment. The upstream source and MIT attribution are recorded in [NOTICE.md](../../NOTICE.md).
