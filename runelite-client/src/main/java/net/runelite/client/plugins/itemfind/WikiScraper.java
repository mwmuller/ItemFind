package net.runelite.client.plugins.itemfind;

import net.runelite.client.RuneLite;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import okhttp3.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class WikiScraper {
    private final static String baseUrl = "https://oldschool.runescape.wiki";
    private final static String baseWikiUrl = baseUrl + "/w/";
    private final static String baseWikiLookupUrl = baseWikiUrl + "Special:Lookup";

    private static Document doc;

    public static CompletableFuture<itemObtainedSelection[]> getItemLocations(OkHttpClient okHttpClient, String itemName, int itemId) {
        CompletableFuture<itemObtainedSelection[]> future = new CompletableFuture<>();

        String url;
        if (itemId > -1) {
            url = getWikiUrlWithId(itemName, itemId);
        } else {
            url = getWikiUrl(itemName);
        }

        requestAsync(okHttpClient, url).whenCompleteAsync((responseHTML, ex) -> {
            List<itemObtainedSelection> itemObtainedSelections = new ArrayList<>();

            if (ex != null) {
                itemObtainedSelection[] result = new itemObtainedSelection[0];
                future.complete(result);
            }

            doc = Jsoup.parse(responseHTML);
            Elements tableHeaders = doc.select("h2 span.mw-headline, h3 span.mw-headline");

            Boolean parseitemObtainedSelection = false;
            itemObtainedSelection curritemObtainedSelection = new itemObtainedSelection();
            Map<String, WikiItem[]> currDropTable = new LinkedHashMap<>();
            int tableIndexH3 = 0;
            int tableIndexH2 = 0;

            for (Element tableHeader : tableHeaders) {
                String tableHeaderText = tableHeader.text();
                // String itemNameLC = itemName.toLowerCase();

                // // --- Handle edge cases for specific pages ---
                // if (itemNameLC.equals("hespori") && tableHeaderText.equals("Main table")) continue;
                // if (itemNameLC.equals("chaos elemental") && tableHeaderText.equals("Major drops")) continue;
                // if (itemNameLC.equals("cyclops") && tableHeaderText.equals("Drops")) continue;
                // if (itemNameLC.equals("gorak") && tableHeaderText.equals("Drops")) continue;
                // if (itemNameLC.equals("undead druid") && tableHeaderText.equals("Seeds")) {
                //     incrementH3Index = true;
                //     continue;
                // }
                // ;
                // ---
                
                String tableHeaderTextLower = tableHeaderText.toLowerCase();
                Boolean isItemObtainHeader = tableHeaderTextLower.toLowerCase().contains("item_sources") || tableHeaderTextLower.toLowerCase().contains("shop_locations") || tableHeaderTextLower.toLowerCase().contains("spawns");

                Elements parentH2 = tableHeader.parent().select("h2"); // Drops
                Boolean isParentH2 = !parentH2.isEmpty();

                Elements parentH3 = tableHeader.parent().select("h3"); // spawns and store locations
                Boolean isParentH3 = !parentH3.isEmpty();

                if (isParentH2 || isParentH3) {
                    if (!currDropTable.isEmpty()) {
                        // reset section
                        curritemObtainedSelection.setTable(currDropTable);
                        itemObtainedSelections.add(curritemObtainedSelection);

                        currDropTable = new LinkedHashMap<>();
                        curritemObtainedSelection = new itemObtainedSelection();
                    }

                    if (isItemObtainHeader) {
                        // new section
                        parseitemObtainedSelection = true;
                        curritemObtainedSelection.setHeader(tableHeaderText);
                    } else {
                        parseitemObtainedSelection = false;
                    }
                } else if (parseitemObtainedSelection && (isParentH2 || isParentH3)) {
                    String element = isParentH2 ? "h2" : "h3";
                    int tableIndex = isParentH2 ? tableIndexH2 : tableIndexH3;
                    // parse table
                    // h2 is item-drops, h3 is store-locations-list || align-center-2

                    String selector = element == "h2" ? " ~ table.item-drops" : " ~ table.store-locations-list";
                    int rowSize = element == "h2" ? 5 : 6; // 5 for item-drops, 4 for store-locations-list
                    Boolean isDrop = element.equals("h2");
                    WikiItem[] tableRows = getTableItems(tableIndex, element + selector, rowSize, isDrop);

                    if (tableRows.length > 0 && !currDropTable.containsKey(tableHeaderText)) {
                        currDropTable.put(tableHeaderText, tableRows);
                        if (isParentH2) {
                            tableIndexH2++;
                            if (isParentH3) {
                                tableIndexH3++;
                            }
                        } else {
                            tableIndexH3++;
                        }
                    }
                }
            }

            if (!currDropTable.isEmpty()) {
                curritemObtainedSelection.setTable(currDropTable);
                itemObtainedSelections.add(curritemObtainedSelection);
            }

            if (itemObtainedSelections.isEmpty()) {
                tableHeaders = doc.select("h2 span.mw-headline");

                if (!tableHeaders.isEmpty()) {
                    WikiItem[] tableRows = getTableItems(0, "h2 ~ table.item-drops", 5, true);
                    if (tableRows.length > 0) {
                        currDropTable = new LinkedHashMap<>();
                        currDropTable.put("Source", tableRows);
                        itemObtainedSelections.add(new itemObtainedSelection("Source", currDropTable));
                    }
                }
            }

            itemObtainedSelection[] result = itemObtainedSelections.toArray(new itemObtainedSelection[itemObtainedSelections.size()]);
            future.complete(result);
        });

        return future;
    }

    private static WikiItem[] getTableItems(int tableIndex, String selector, int rowSize, Boolean isDrop) {
        List<WikiItem> wikiItems = new ArrayList<>();
        Elements dropTables = doc.select(selector);

        if (dropTables.size() > tableIndex) {
            Elements dropTableRows = dropTables.get(tableIndex).select("tbody tr");
            for (Element dropTableRow : dropTableRows) {
                String[] lootRow = new String[rowSize];
                Elements dropTableCells = dropTableRow.select("td");
                int index = 1;

                for (Element dropTableCell : dropTableCells) {
                    String cellContent = dropTableCell.text();
                    Elements images = dropTableCell.select("img");

                    if (images.size() != 0) {
                        String imageSource = images.first().attr("src");
                        if (!imageSource.isEmpty()) {
                            lootRow[0] = baseUrl + imageSource;
                        }
                    }

                    if (cellContent != null && !cellContent.isEmpty() && index < rowSize) {
                        cellContent = filterTableContent(cellContent);
                        lootRow[index] = cellContent;
                        index++;
                    }
                }

                if (lootRow[0] != null) {
                    WikiItem wikiItem = parseRow(lootRow, isDrop);
                    wikiItems.add(wikiItem);
                }
            }
        }


        WikiItem[] result = new WikiItem[wikiItems.size()];
        return wikiItems.toArray(result);
    }

    public static WikiItem parseRow(String[] row, Boolean isDrop) {
        String src_spwn_sell = "";
        String level = "";

        double rarity = -1;
        String rarityStr = "";

        int quantity = 0;
        String quantityStr = "";

        // First element is always a seller/spawn/npc
        src_spwn_sell = row[0];
        
        if (isDrop) { // dropped from an NPC, use item_sources logic
            level = row[1];

            NumberFormat nf = NumberFormat.getNumberInstance();

            // quantity logic
            quantityStr = row[2];
            quantityStr = quantityStr.replaceAll("–", "-").trim();
            try {
                String[] quantityStrs = quantityStr.replaceAll("\\s+", "").split("-");
                String firstQuantityStr = quantityStrs.length > 0 ? quantityStrs[0] : null;
                quantity = nf.parse(firstQuantityStr).intValue();
            } catch (ParseException e) {
            }

            rarityStr = row[3];
            if (rarityStr.startsWith("~")) {
                rarityStr = rarityStr.substring(1);
            } else if (rarityStr.startsWith("2 × ") || rarityStr.startsWith("3 × ")) {
                rarityStr = rarityStr.substring(4);
            }

            try {
                String[] rarityStrs = rarityStr.replaceAll("\\s+", "").split(";");
                String firstRarityStr = rarityStrs.length > 0 ? rarityStrs[0] : null;

                if (firstRarityStr != null) {
                    if (firstRarityStr.equals("Always")) {
                        rarity = 1.0;
                    } else {
                        String[] fraction = firstRarityStr.split("/");
                        if (fraction.length > 1) {
                            double numer = nf.parse(fraction[0]).doubleValue();
                            double denom = nf.parse(fraction[1]).doubleValue();
                            rarity = numer / denom;
                        }

                    }
                }
            } catch (ParseException ex) {
            }

        }
        return new WikiItem(src_spwn_sell, level, quantity, quantityStr, rarityStr, rarity);
    }


    public static String filterTableContent(String cellContent) {
        return cellContent.replaceAll("\\[.*\\]", "");
    }

    public static String getWikiUrl(String itemOritemName) {
        String sanitizedName = sanitizeName(itemOritemName);
        return baseWikiUrl + sanitizedName;
    }

    public static String getWikiUrlWithId(String itemName, int id) {
        String sanitizedName = sanitizeName(itemName);
        return baseWikiLookupUrl + "?type=item&id=" + String.valueOf(id) + "&name=" + sanitizedName;
    }

    public static String getWikiUrlForDrops(String itemName, String anchorText, int itemId) {
        if (itemId > -1) {
            return getWikiUrlWithId(itemName, itemId);
        }
        String sanitizeditemName = sanitizeName(itemName);
        String anchorStr = "Drops";
        if (anchorText != null) {
            anchorStr = anchorText.replaceAll("\\s+", "_");
        }
        return baseWikiUrl + sanitizeditemName + "#" + anchorStr;
    }

    public static String sanitizeName(String name) {
        // ---
        name = name.trim().toLowerCase().replaceAll("\\s+", "_");
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    public static Boolean isItemHeaderForEdgeCases(String itemName, String tableHeaderText) {
        String itemNameLC = itemName.toLowerCase();
        String tableHeaderTextLower = tableHeaderText.toLowerCase();
        return (false); // update
    }

    public static Boolean parseH3PrimaryForEdgeCases(String itemName) {
        String itemNameLC = itemName.toLowerCase();
        return itemNameLC.equals("cyclops");
    }

    private static CompletableFuture<String> requestAsync(OkHttpClient okHttpClient, String url) {
        CompletableFuture<String> future = new CompletableFuture<>();

        Request request = new Request.Builder().url(url).header("User-Agent", RuneLite.USER_AGENT).build();

        okHttpClient
                .newCall(request)
                .enqueue(
                        new Callback() {
                            @Override
                            public void onFailure(Call call, IOException ex) {
                                future.completeExceptionally(ex);
                            }

                            @Override
                            public void onResponse(Call call, Response response) throws IOException {
                                try (ResponseBody responseBody = response.body()) {
                                    if (!response.isSuccessful()) future.complete("");

                                    future.complete(responseBody.string());
                                } finally {
                                    response.close();
                                }
                            }
                        });

        return future;
    }

}