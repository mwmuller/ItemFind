
package net.runelite.client.plugins.itemfind;

import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.components.IconTextField;
import net.runelite.client.util.ImageUtil;
import okhttp3.OkHttpClient;

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

    private itemObtainedSelection itemObtainedSelection;
    private ItemFindPanel panel;
    private ItemFindPluginService service;
    private NavigationButton navButton;

    @Provides
    ItemFindConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(ItemFindConfig.class);
    }

    @Override
    protected void startUp() throws Exception
    {
        service = new ItemFindPluginService();
        panel = new ItemFindPanel();

        panel.getSearchButton().addActionListener(e -> {
            String itemName = panel.getSearchText();
            int itemId = itemManager.search(itemName).get(0).getId();
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

        WikiScraper.getItemLocations(okHttpClient, itemName, itemId).whenCompleteAsync((itemObtainedSelection, ex) -> {
            this.itemObtainedSelection = itemObtainedSelection;
            if (tablePanel != null) {
                tablePanel.resetSelectedIndex();
            }
            monsterSearchField.setIcon(dropTableSections.length == 0 ? IconTextField.Icon.ERROR : IconTextField.Icon.SEARCH);
            monsterSearchField.setEditable(true);
            refreshMainPanel();
    });
    }
}


