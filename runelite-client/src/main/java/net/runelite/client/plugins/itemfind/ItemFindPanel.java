package net.runelite.client.plugins.itemfind;
import net.runelite.client.ui.components.IconTextField;

import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.ColorScheme;
import java.awt.image.BufferedImage;
import net.runelite.client.ui.FontManager;
import javax.swing.*;
import java.awt.*;

public class ItemFindPanel extends PluginPanel {
    private static final int PANEL_WIDTH = 300;
    private static final int PANEL_HEIGHT = 400;

    private final IconTextField searchField = new IconTextField();
    private final JButton searchButton = new JButton("Search");
    private final JTextArea resultArea = new JTextArea();
    private final JLabel overallIcon = new JLabel();
    private itemObtainedSelection[] itemObtainedSelection;

    public ItemFindPanel() {

        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));

        // Search bar panel
        JPanel searchPanel = new JPanel(new BorderLayout(5, 0));
        searchPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        searchPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        searchField.setPreferredSize(new Dimension(200, 30));
        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(searchButton, BorderLayout.EAST);

        add(searchPanel, BorderLayout.NORTH);

        // Results area
        resultArea.setEditable(false);
        resultArea.setFont(FontManager.getRunescapeSmallFont());
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(resultArea);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(scrollPane, BorderLayout.CENTER);
    }

    public String getSearchText() {
        return searchField.getText();
    }

    public JButton getSearchButton() {
        return searchButton;
    }

    void loadHeaderIcon(BufferedImage img)
	{
		overallIcon.setIcon(new ImageIcon(img));
	}

    public void setResultText(String text) {
        resultArea.setText(text);
    }

    // public void refreshMainPanel() {
    //     if (itemObtainedSelection != null && itemObtainedSelection.length > 0) {
    //         SwingUtilities.invokeLater(() -> {
    //             rebuildMainPanel();
    //         });
    //     }
    // }
}

