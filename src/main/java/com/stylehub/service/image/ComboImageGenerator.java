package com.stylehub.service.image;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

/**
 * Generates composite flat-lay preview images using Java 2D Graphics & ImageIO.
 * Composes constituent product photos into a unified lookbook collage.
 */
@Component
public class ComboImageGenerator {

    private static final Logger log = LoggerFactory.getLogger(ComboImageGenerator.class);
    private final ImageLayoutService layoutService;

    public record GarmentVisualSpec(String name, String imageUrl, String category) {}

    public ComboImageGenerator(ImageLayoutService layoutService) {
        this.layoutService = layoutService;
    }

    public byte[] generateCollage(List<GarmentVisualSpec> garments, String comboTitle) {
        int width = ImageLayoutService.CANVAS_WIDTH;
        int height = ImageLayoutService.CANVAS_HEIGHT;

        BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = canvas.createGraphics();

        try {
            // High Quality Rendering Hints (Bicubic Luxury Interpolation)
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            // Studio Background (Luxury Soft Photography Gradient)
            GradientPaint studioGrad = new GradientPaint(
                    0, 0, new Color(250, 251, 253),
                    0, height, new Color(238, 240, 245)
            );
            g2.setPaint(studioGrad);
            g2.fillRect(0, 0, width, height);

            // Outer Frame Accent
            g2.setColor(new Color(225, 228, 235));
            g2.drawRect(0, 0, width - 1, height - 1);

            int count = (garments != null) ? garments.size() : 0;
            if (count == 0) {
                drawEmptyPlaceholder(g2, width, height);
            } else {
                List<Rectangle> boxes = layoutService.calculateLayout(count);
                for (int i = 0; i < count; i++) {
                    GarmentVisualSpec spec = garments.get(i);
                    Rectangle box = (i < boxes.size()) ? boxes.get(i) : boxes.get(boxes.size() - 1);
                    drawGarmentCard(g2, spec, box);
                }
            }

            // Luxury StyleHub Tag Pill
            drawBrandBadge(g2, comboTitle, width, height);

        } finally {
            g2.dispose();
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(canvas, "jpg", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Failed to encode JPEG collage", e);
            return new byte[0];
        }
    }

    private void drawGarmentCard(Graphics2D g2, GarmentVisualSpec spec, Rectangle box) {
        int cornerRadius = 16;
        Shape cardShape = new RoundRectangle2D.Float(box.x, box.y, box.width, box.height, cornerRadius, cornerRadius);

        // Soft Studio Elevated Drop Shadow
        g2.setColor(new Color(0, 0, 0, 14));
        g2.fillRoundRect(box.x - 2, box.y + 4, box.width + 4, box.height + 6, cornerRadius + 4, cornerRadius + 4);
        g2.setColor(new Color(0, 0, 0, 22));
        g2.fillRoundRect(box.x - 1, box.y + 2, box.width + 2, box.height + 3, cornerRadius + 2, cornerRadius + 2);

        // Crisp White Card Canvas
        g2.setColor(Color.WHITE);
        g2.fill(cardShape);

        // Attempt to load product image
        BufferedImage srcImage = fetchImage(spec.imageUrl());

        if (srcImage != null) {
            // Clip to rounded rectangle
            Shape oldClip = g2.getClip();
            g2.clip(cardShape);

            // Scale to fill maintaining aspect ratio (cover with subtle padding)
            int pad = 4;
            int drawW = box.width - (pad * 2);
            int drawH = box.height - (pad * 2);

            double scale = Math.max((double) drawW / srcImage.getWidth(), (double) drawH / srcImage.getHeight());
            int scaledW = (int) (srcImage.getWidth() * scale);
            int scaledH = (int) (srcImage.getHeight() * scale);

            int drawX = box.x + pad + (drawW - scaledW) / 2;
            int drawY = box.y + pad + (drawH - scaledH) / 2;

            g2.drawImage(srcImage, drawX, drawY, scaledW, scaledH, null);
            g2.setClip(oldClip);
        } else {
            // Clean Placeholder Card
            drawPlaceholderCard(g2, spec, box);
        }

        // Card Border Line
        g2.setColor(new Color(225, 225, 225));
        g2.setStroke(new BasicStroke(1.2f));
        g2.draw(cardShape);

        // Subtle Garment Name Label at Bottom of Card
        drawGarmentLabel(g2, spec.name(), box);
    }

    private void drawGarmentLabel(Graphics2D g2, String title, Rectangle box) {
        if (title == null || title.isBlank() || box.height < 120) return;

        int labelHeight = 28;
        int y = box.y + box.height - labelHeight;

        // Semi-transparent label background
        g2.setColor(new Color(255, 255, 255, 215));
        g2.fillRect(box.x + 2, y, box.width - 4, labelHeight - 2);

        g2.setColor(new Color(40, 40, 40));
        g2.setFont(new Font("SansSerif", Font.BOLD, Math.min(12, box.width / 22)));

        FontMetrics fm = g2.getFontMetrics();
        String display = title;
        if (fm.stringWidth(display) > box.width - 20) {
            while (display.length() > 4 && fm.stringWidth(display + "...") > box.width - 20) {
                display = display.substring(0, display.length() - 1);
            }
            display = display + "...";
        }

        int textX = box.x + (box.width - fm.stringWidth(display)) / 2;
        int textY = y + ((labelHeight + fm.getAscent() - fm.getDescent()) / 2) - 2;
        g2.drawString(display, textX, textY);
    }

    private void drawPlaceholderCard(Graphics2D g2, GarmentVisualSpec spec, Rectangle box) {
        g2.setColor(new Color(236, 238, 241));
        g2.fillRoundRect(box.x, box.y, box.width, box.height, 16, 16);

        g2.setColor(new Color(60, 64, 72));
        g2.setFont(new Font("SansSerif", Font.BOLD, Math.max(11, Math.min(14, box.width / 16))));
        FontMetrics fm = g2.getFontMetrics();

        String label = (spec.name() != null && !spec.name().isBlank()) ? spec.name() : "Garment";
        if (fm.stringWidth(label) > box.width - 24) {
            while (label.length() > 4 && fm.stringWidth(label + "...") > box.width - 24) {
                label = label.substring(0, label.length() - 1);
            }
            label = label + "...";
        }

        int textX = box.x + (box.width - fm.stringWidth(label)) / 2;
        int textY = box.y + (box.height / 2) - 4;
        g2.drawString(label, textX, textY);

        g2.setColor(new Color(120, 125, 135));
        g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
        String sub = spec.category() != null ? spec.category() : "StyleHub Piece";
        int subX = box.x + (box.width - g2.getFontMetrics().stringWidth(sub)) / 2;
        g2.drawString(sub, subX, textY + 18);
    }

    private void drawBrandBadge(Graphics2D g2, String comboTitle, int canvasW, int canvasH) {
        String badgeText = "STYLEHUB • CURATED COMBO";
        if (comboTitle != null && !comboTitle.isBlank()) {
            badgeText = "STYLEHUB • " + comboTitle.toUpperCase();
            if (badgeText.length() > 38) {
                badgeText = badgeText.substring(0, 35) + "...";
            }
        }

        g2.setFont(new Font("SansSerif", Font.BOLD, 10));
        FontMetrics fm = g2.getFontMetrics();
        int pillW = fm.stringWidth(badgeText) + 24;
        int pillH = 24;
        int pillX = (canvasW - pillW) / 2;
        int pillY = canvasH - pillH - 8;

        // Dark luxury pill
        g2.setColor(new Color(20, 20, 20, 220));
        g2.fillRoundRect(pillX, pillY, pillW, pillH, 12, 12);

        g2.setColor(new Color(212, 175, 55)); // Luxury gold accent
        g2.drawRoundRect(pillX, pillY, pillW, pillH, 12, 12);

        g2.setColor(Color.WHITE);
        int textX = pillX + (pillW - fm.stringWidth(badgeText)) / 2;
        int textY = pillY + ((pillH + fm.getAscent() - fm.getDescent()) / 2) - 1;
        g2.drawString(badgeText, textX, textY);
    }

    private void drawEmptyPlaceholder(Graphics2D g2, int width, int height) {
        g2.setColor(new Color(150, 150, 150));
        g2.setFont(new Font("SansSerif", Font.BOLD, 18));
        String text = "Select Products to Generate Combo Preview";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, (width - fm.stringWidth(text)) / 2, height / 2);
    }

    private static final java.util.Map<String, BufferedImage> IMAGE_MEM_CACHE = new java.util.concurrent.ConcurrentHashMap<>();
    private static final File DISK_CACHE_DIR = new File("uploads", "cache");

    private BufferedImage fetchImage(String urlStr) {
        if (urlStr == null || urlStr.isBlank()) {
            return null;
        }

        BufferedImage cached = IMAGE_MEM_CACHE.get(urlStr);
        if (cached != null) {
            return cached;
        }

        try {
            // 1. Handle local file uploads (/uploads/...)
            if (urlStr.startsWith("/uploads/")) {
                String relative = urlStr.substring("/uploads/".length());
                File localFile = new File("uploads", relative);
                if (localFile.exists() && localFile.isFile()) {
                    BufferedImage img = ImageIO.read(localFile);
                    if (img != null) IMAGE_MEM_CACHE.put(urlStr, img);
                    return img;
                }
                return null;
            }

            // 2. Handle classpath static images (/images/...)
            if (urlStr.startsWith("/images/")) {
                try (InputStream in = getClass().getResourceAsStream("/static" + urlStr)) {
                    if (in != null) {
                        BufferedImage img = ImageIO.read(in);
                        if (img != null) IMAGE_MEM_CACHE.put(urlStr, img);
                        return img;
                    }
                }
                return null;
            }

            // 3. Handle disk cache for remote URLs
            if (!DISK_CACHE_DIR.exists()) {
                DISK_CACHE_DIR.mkdirs();
            }
            String cacheFilename = Integer.toHexString(urlStr.hashCode()) + ".jpg";
            File diskCachedFile = new File(DISK_CACHE_DIR, cacheFilename);
            if (diskCachedFile.exists() && diskCachedFile.length() > 0) {
                try {
                    BufferedImage img = ImageIO.read(diskCachedFile);
                    if (img != null) {
                        IMAGE_MEM_CACHE.put(urlStr, img);
                        return img;
                    }
                } catch (Exception ignored) {}
            }

            // 4. Handle remote HTTP / HTTPS URLs with 5000ms timeout
            String targetUrl = urlStr;
            if (targetUrl.contains("images.unsplash.com") && !targetUrl.contains("fm=jpg")) {
                targetUrl += (targetUrl.contains("?") ? "&" : "?") + "fm=jpg";
            }

            URI uri = URI.create(targetUrl);
            URL url = uri.toURL();
            URLConnection conn = url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            conn.setRequestProperty("Accept", "image/jpeg,image/png,image/*;q=0.9");

            try (InputStream in = conn.getInputStream()) {
                byte[] data = in.readAllBytes();
                try (java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(data)) {
                    BufferedImage img = ImageIO.read(bais);
                    if (img != null) {
                        IMAGE_MEM_CACHE.put(urlStr, img);
                        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(diskCachedFile)) {
                            fos.write(data);
                        } catch (Exception ignored) {}
                    }
                    return img;
                }
            }
        } catch (Exception e) {
            log.debug("Could not fetch image from '{}': {}", urlStr, e.getMessage());
            return null;
        }
    }
}
