package net.runelite.client.plugins.itemfind;

import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.IconTextField;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class ItemFindPanel extends PluginPanel {
    private static final int PANEL_WIDTH = 300;
    private static final int PANEL_HEIGHT = 400;

    private final IconTextField searchBar;
    private final JButton searchButton;
    private final ItemSourcePanel resultPanel;
    private final JScrollPane scrollPane;
    private final JLabel overallIcon;

    public ItemFindPanel() {
        super(false);
        
        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));

        searchBar = new IconTextField();
        searchBar.setIcon(IconTextField.Icon.SEARCH);
        searchBar.setPreferredSize(new Dimension(0, 30));
        searchBar.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        searchBar.setHoverBackgroundColor(ColorScheme.DARK_GRAY_HOVER_COLOR);

        searchButton = new JButton("Search");
        searchButton.setFocusPainted(false);

        // Search bar panel
        JPanel searchPanel = new JPanel(new BorderLayout(5, 0));
        searchPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        searchPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        searchPanel.add(searchBar, BorderLayout.CENTER);
        searchPanel.add(searchButton, BorderLayout.EAST);

        add(searchPanel, BorderLayout.NORTH);

        // Results area
        resultPanel = new ItemSourcePanel();
        scrollPane = new JScrollPane(resultPanel);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(12, 0));
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        add(scrollPane, BorderLayout.CENTER);

        overallIcon = new JLabel();
    }

    public String getSearchText() {
        return searchBar.getText();
    }

    public JButton getSearchButton() {
        return searchButton;
    }

    public void loadHeaderIcon(BufferedImage img) {
        overallIcon.setIcon(new ImageIcon(img));
        searchBar.setIcon(new ImageIcon(img));
    }

    public void updateResults(itemObtainedSelection[] selections) {
        resultPanel.updateDisplay(selections);
        scrollPane.getVerticalScrollBar().setValue(0);
    }
}

