
package net.runelite.client.plugins.itemfind;

import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import okhttp3.OkHttpClient;
import java.util.Collections;

import java.awt.image.BufferedImage;
import javax.inject.Inject;

import net.runelite.api.gameval.SpriteID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;

import com.google.inject.Provides;

@PluginDescriptor(
    name = "Item Find",
    description = "Helps find items using the OSRS Wiki",
    tags = {"items", "search", "find"}
)

public class ItemFindPlugin extends Plugin
{
    @Inject
    private ClientToolbar clientToolbar;

    @Inject
	private SpriteManager spriteManager;
    
    @Inject
    private ItemManager itemManager;

    @Inject
    public OkHttpClient okHttpClient;

    private ItemFindPanel panel;
    private NavigationButton navButton;
    private String currentItemName;

    @Provides
    ItemFindConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(ItemFindConfig.class);
    }

    @Override
    protected void startUp() throws Exception
    {
        panel = new ItemFindPanel();

        panel.getSearchButton().addActionListener(e -> {
            String itemName = panel.getSearchText();
            var searchResults = itemManager.search(itemName);
            if (searchResults.isEmpty()) {
                // Show simple error message
                itemObtainedSelection[] error = new itemObtainedSelection[1];
                error[0] = new itemObtainedSelection("Item not found",
                    Collections.emptyMap());
                panel.updateResults(error);
                return;
            }
            int itemId = searchResults.get(0).getId();
            searchForItemName(itemName, itemId);
        });
        
        spriteManager.getSpriteAsync(SpriteID.OptionsIcons.TWO_FINGERS_POINTING_AT_MAGNIFYING_GLASS, 0, panel::loadHeaderIcon);
        // Load the RuneLite sprite for the navigation button icon
        // Use a generic icon (e.g., normal.png from the hiscore plugin or another available resource)
        final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "panel_icon.png");

        navButton = NavigationButton.builder()
            .tooltip("Item Find")
            .icon(icon)
            .priority(1)
            .panel(panel)
            .build();
        clientToolbar.addNavigation(navButton);
    }

    @Override
    protected void shutDown()
    {
        if (navButton != null)
        {
            clientToolbar.removeNavigation(navButton);
        }
    }
    
    void searchForItemName(String itemName, int itemId) {
        if (itemName.isEmpty()) return;
        
        // Don't search if it's the same item
        if (itemName.equals(currentItemName)) return;
        
        currentItemName = itemName;
        WikiScraper.getItemLocations(okHttpClient, itemName, itemId).whenCompleteAsync((itemObtainedSelection, ex) -> {
            if (ex != null) {
                // Create a single selection to show the error
                itemObtainedSelection[] error = new itemObtainedSelection[1];
                error[0] = new itemObtainedSelection("Error", 
                    Collections.singletonMap("❗ Item Not Found", new WikiItem[]{
                        new WikiItem("/skill_icons/Construction.png", 
                            "Item '" + itemName + "' could not be found on the Wiki", 
                            "", 0, "", "", 0.0)
                    }));
                panel.updateResults(error);
                return;
            }
            
            if (itemObtainedSelection == null || itemObtainedSelection.length == 0) {
                // Create a single selection to show no results
                itemObtainedSelection[] noResults = new itemObtainedSelection[1];
                noResults[0] = new itemObtainedSelection("No Results", 
                    Collections.singletonMap("❗ No Sources Found", new WikiItem[]{
                        new WikiItem("/skill_icons/Construction.png", 
                            "No sources found for '" + itemName + "'", 
                            "", 0, "", "", 0.0)
                    }));
                panel.updateResults(noResults);
                return;
            }
            
            panel.updateResults(itemObtainedSelection);
        });
    }
}


