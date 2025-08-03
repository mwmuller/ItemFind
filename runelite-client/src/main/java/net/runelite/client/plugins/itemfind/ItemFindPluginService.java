package net.runelite.client.plugins.itemfind;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ItemFindPluginService {
    // Use the parse API to get the page HTML
    private static final String WIKI_API_URL = "https://oldschool.runescape.wiki/api.php?action=parse&format=json&page=";

    // Simple in-memory cache for item lookups
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public String searchItem(String itemName) {
        String key = itemName.trim().toLowerCase();
        if (cache.containsKey(key)) {
            return cache.get(key);
        }
        try {
            String urlStr = WIKI_API_URL + itemName.replace(" ", "%20");
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.connect();
            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                return "Error: Unable to fetch data from Wiki.";
            }
            Scanner sc = new Scanner(url.openStream());
            StringBuilder inline = new StringBuilder();
            while (sc.hasNext()) {
                inline.append(sc.nextLine());
            }
            sc.close();
            JsonObject json = new JsonParser().parse(inline.toString()).getAsJsonObject();
            // Extract the HTML text from the "parse" -> "text" -> "*"
            JsonObject parse = json.getAsJsonObject("parse");
            if (parse == null) {
                return "Error: Item not found on Wiki.";
            }
            JsonObject text = parse.getAsJsonObject("text");
            String html = text.get("*").getAsString();

            // Parse the HTML for relevant tables
            Document doc = Jsoup.parse(html);
            StringBuilder tablesText = new StringBuilder();

            // 1) Tables with class containing 'item-drops'
            for (Element table : doc.select("table.item-drops, table[class*='item-drops']")) {
                tablesText.append("[Item Drops Table]\n");
                tablesText.append(parseTableToText(table)).append("\n\n");
            }

            // 2) Tables that directly precede an 'mw-header' with id 'Spawns'
            for (Element header : doc.select(".mw-headline#Spawns")) {
                Element prev = header.parent().previousElementSibling();
                if (prev != null && prev.tagName().equals("table")) {
                    tablesText.append("[Table before Spawns Section]\n");
                    tablesText.append(parseTableToText(prev)).append("\n\n");
                }
            }

            // 3) Tables with class containing 'store-locations-list'
            for (Element table : doc.select("table.store-locations-list, table[class*='store-locations-list']")) {
                tablesText.append("[Store Locations Table]\n");
                tablesText.append(parseTableToText(table)).append("\n\n");
            }

            String result = tablesText.length() > 0 ? tablesText.toString() : "No relevant tables found.";
            cache.put(key, result);
            return result;
        } catch (IOException e) {
            return "Error: " + e.getMessage();
        }
    }

    // Helper to convert a Jsoup table element to a simple text table
    private String parseTableToText(Element table) {
        StringBuilder sb = new StringBuilder();
        Elements rows = table.select("tr");
        for (Element row : rows) {
            Elements cells = row.select("th, td");
            for (Element cell : cells) {
                sb.append(cell.text()).append(" | ");
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}
