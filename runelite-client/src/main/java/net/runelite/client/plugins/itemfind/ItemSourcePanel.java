package net.runelite.client.plugins.itemfind;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ItemSourcePanel extends JPanel {
    private static final Dimension IMAGE_SIZE = new Dimension(32, 32);
    private static final Color BACKGROUND_COLOR = ColorScheme.DARKER_GRAY_COLOR;
    private static final Color HEADER_COLOR = ColorScheme.BRAND_ORANGE;
    private static final int PADDING = 4;
    private static final int INNER_PADDING = 2;
    private static final int PANEL_WIDTH = 300;

    public ItemSourcePanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(BACKGROUND_COLOR);
    }

    public void updateDisplay(itemObtainedSelection[] selections) {
        removeAll();

        if (selections == null || selections.length == 0) {
            add(createLabel("No sources found for this item.", HEADER_COLOR));
            revalidate();
            repaint();
            return;
        }

        for (itemObtainedSelection section : selections) {
            // Add section header
            add(createHeader(section.getHeader()));
            
            for (Map.Entry<String, WikiItem[]> table : section.getTable().entrySet()) {
                // Add table header
                add(createSubHeader(table.getKey()));
                
                // Create grid panel for items
                JPanel gridPanel = new JPanel(new GridLayout(0, 1, PADDING, PADDING));
                gridPanel.setBackground(BACKGROUND_COLOR);
                gridPanel.setBorder(new EmptyBorder(INNER_PADDING, PADDING, INNER_PADDING, PADDING));

                for (WikiItem item : table.getValue()) {
                    gridPanel.add(createItemPanel(item, table.getKey()));
                }

                add(gridPanel);
                add(Box.createRigidArea(new Dimension(0, INNER_PADDING)));
            }
        }

        revalidate();
        repaint();
    }

    private JLabel createHeader(String text) {
        JLabel header = new JLabel(text);
        header.setFont(FontManager.getRunescapeBoldFont());
        header.setForeground(HEADER_COLOR);
        header.setBorder(new EmptyBorder(PADDING, PADDING, INNER_PADDING, PADDING));
        return header;
    }

    private JLabel createSubHeader(String text) {
        JLabel subHeader = new JLabel(text);
        subHeader.setFont(FontManager.getRunescapeSmallFont());
        subHeader.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        subHeader.setBorder(new EmptyBorder(INNER_PADDING, PADDING * 2, INNER_PADDING, PADDING));
        return subHeader;
    }

    private JLabel createLabel(String text, Color color) {
        JLabel label = new JLabel(text);
        label.setForeground(color);
        label.setFont(FontManager.getRunescapeSmallFont());
        return label;
    }

    private JPanel createItemPanel(WikiItem item, String tableType) {
        JPanel panel = new JPanel();
        panel.setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ColorScheme.DARKER_GRAY_COLOR.brighter(), 1),
            new EmptyBorder(INNER_PADDING, INNER_PADDING, INNER_PADDING, INNER_PADDING)
        ));
        panel.setLayout(new BorderLayout(INNER_PADDING, 0));
        panel.setPreferredSize(new Dimension(PANEL_WIDTH - (PADDING * 2), panel.getPreferredSize().height));

        // Left side - Image
        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            loadImage(item.getImageUrl()).thenAccept(image -> {
                if (image != null) {
                    JLabel imageLabel = new JLabel(new ImageIcon(image));
                    imageLabel.setPreferredSize(IMAGE_SIZE);
                    panel.add(imageLabel, BorderLayout.WEST);
                    panel.revalidate();
                }
            });
        }

        // Center - Information
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());

        // Location/Source with left alignment
        JLabel sourceLabel = createLabel(item.src_spwn_sell(), ColorScheme.LIGHT_GRAY_COLOR);
        sourceLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoPanel.add(sourceLabel);

        // Additional info based on type
        if (!tableType.toLowerCase().contains("shop") && !tableType.toLowerCase().contains("spawn")) {
            // Create a panel for level and amount on the same line
            JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, INNER_PADDING, 0));
            statsPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());
            statsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            
            if (!item.getLevel().isEmpty()) {
                String levelText = "L:" + item.getLevel();
                statsPanel.add(createLabel(levelText, Color.WHITE));
                statsPanel.add(createLabel(" • ", Color.GRAY)); // Smaller separator
            }
            statsPanel.add(createLabel("Qty:" + item.getQuantityLabelText(), Color.WHITE));
            
            infoPanel.add(statsPanel);
            JLabel rarityLabel = createLabel("Rarity: " + item.getRarityStr(), HEADER_COLOR);
            rarityLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            infoPanel.add(rarityLabel);
        } else if (tableType.toLowerCase().contains("spawn")) {
            infoPanel.add(createLabel("Amount: " + item.getQuantityLabelText(), Color.WHITE));
        } else {
            // nothing
        }

        panel.add(infoPanel, BorderLayout.CENTER);
        return panel;
    }

    private CompletableFuture<Image> loadImage(String imageUrl) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                java.net.URL url = new java.net.URL(imageUrl);
                BufferedImage image = javax.imageio.ImageIO.read(url);
                if (image != null) {
                    return image.getScaledInstance(32, 32, Image.SCALE_SMOOTH);
                }
            } catch (Exception e) {
                // Log the error but continue without the image
                System.err.println("Failed to load image from URL: " + imageUrl);
            }
            return null;
        });
    }
}
