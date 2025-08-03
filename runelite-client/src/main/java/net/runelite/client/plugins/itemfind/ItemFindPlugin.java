package net.runelite.client.plugins.itemfind;

import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import javax.inject.Inject;

@PluginDescriptor(
    name = "Item Find",
    description = "Helps find items using the OSRS Wiki",
    tags = {"items", "search", "find"}
)
public class ItemFindPlugin extends Plugin
{
    @Inject
    private ClientToolbar clientToolbar;

    private ItemFindPanel panel;
    private ItemFindPluginService service;
    private NavigationButton navButton;

    @Override
    protected void startUp()
    {
        service = new ItemFindPluginService();
        panel = new ItemFindPanel();
        panel.getSearchButton().addActionListener(e -> {
            String item = panel.getSearchText();
            String result = service.searchItem(item);
            panel.setResultText(result);
        });
        navButton = NavigationButton.builder()
            .tooltip("Item Find")
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
}
