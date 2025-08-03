package net.runelite.client.plugins.itemfind;
import net.runelite.api.ItemComposition;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ItemFindPluginService {
    // Simple in-memory cache for item lookups
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public String searchItem(String itemName, int itemId) {
            WikiScraper.getItemLocations(itemName, itemId);
    }
}
