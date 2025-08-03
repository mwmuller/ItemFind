package net.runelite.client.plugins.itemfind;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
@ConfigGroup("itemfind")
public interface ItemFindConfig extends Config
{
    @ConfigItem(
        keyName = "includeSpawns",
        name = "Include Spawns",
        description = "Include spawn locations in search results"
    )
    default boolean includeSpawns() { return true; }

    @ConfigItem(
        keyName = "includeStores",
        name = "Include Stores",
        description = "Include store locations in search results"
    )
    default boolean includeStores() { return true; }

    @ConfigItem(
        keyName = "includeDrops",
        name = "Include Drops",
        description = "Include drop locations in search results"
    )
    default boolean includeDrops() { return true; }
}
