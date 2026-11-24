package com.stylehub.service.image;

import org.springframework.stereotype.Service;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Calculates high-fashion flat-lay collage geometry and bounding boxes
 * for any number of constituent garments.
 */
@Service
public class ImageLayoutService {

    public static final int CANVAS_WIDTH = 800;
    public static final int CANVAS_HEIGHT = 800;
    private static final int PADDING = 24;
    private static final int GAP = 16;

    public List<Rectangle> calculateLayout(int itemCount) {
        if (itemCount <= 0) {
            return Collections.emptyList();
        }

        List<Rectangle> boxes = new ArrayList<>(itemCount);
        int innerW = CANVAS_WIDTH - (PADDING * 2);
        int innerH = CANVAS_HEIGHT - (PADDING * 2);

        switch (itemCount) {
            case 1 -> {
                boxes.add(new Rectangle(PADDING, PADDING, innerW, innerH));
            }
            case 2 -> {
                int colW = (innerW - GAP) / 2;
                boxes.add(new Rectangle(PADDING, PADDING, colW, innerH));
                boxes.add(new Rectangle(PADDING + colW + GAP, PADDING, colW, innerH));
            }
            case 3 -> {
                // Editorial Flat-Lay: 1 Hero Garment (left) + 2 Stacked Garments (right)
                int heroW = (int) (innerW * 0.58);
                int rightW = innerW - heroW - GAP;
                int rowH = (innerH - GAP) / 2;

                boxes.add(new Rectangle(PADDING, PADDING, heroW, innerH));
                boxes.add(new Rectangle(PADDING + heroW + GAP, PADDING, rightW, rowH));
                boxes.add(new Rectangle(PADDING + heroW + GAP, PADDING + rowH + GAP, rightW, rowH));
            }
            case 4 -> {
                // 2x2 Clean Quadrant Grid
                int cellW = (innerW - GAP) / 2;
                int cellH = (innerH - GAP) / 2;

                boxes.add(new Rectangle(PADDING, PADDING, cellW, cellH));
                boxes.add(new Rectangle(PADDING + cellW + GAP, PADDING, cellW, cellH));
                boxes.add(new Rectangle(PADDING, PADDING + cellH + GAP, cellW, cellH));
                boxes.add(new Rectangle(PADDING + cellW + GAP, PADDING + cellH + GAP, cellW, cellH));
            }
            case 5 -> {
                // Top Row: 2 items, Bottom Row: 3 items
                int topW = (innerW - GAP) / 2;
                int topH = (innerH - GAP) / 2;
                int botW = (innerW - (GAP * 2)) / 3;
                int botH = innerH - topH - GAP;

                boxes.add(new Rectangle(PADDING, PADDING, topW, topH));
                boxes.add(new Rectangle(PADDING + topW + GAP, PADDING, topW, topH));

                boxes.add(new Rectangle(PADDING, PADDING + topH + GAP, botW, botH));
                boxes.add(new Rectangle(PADDING + botW + GAP, PADDING + topH + GAP, botW, botH));
                boxes.add(new Rectangle(PADDING + (botW + GAP) * 2, PADDING + topH + GAP, botW, botH));
            }
            default -> {
                // 3 columns x N rows grid
                int cols = 3;
                int rows = (int) Math.ceil((double) itemCount / cols);
                int cellW = (innerW - (GAP * (cols - 1))) / cols;
                int cellH = (innerH - (GAP * (rows - 1))) / rows;

                for (int i = 0; i < itemCount; i++) {
                    int c = i % cols;
                    int r = i / cols;
                    int x = PADDING + c * (cellW + GAP);
                    int y = PADDING + r * (cellH + GAP);
                    boxes.add(new Rectangle(x, y, cellW, cellH));
                }
            }
        }

        return boxes;
    }
}
