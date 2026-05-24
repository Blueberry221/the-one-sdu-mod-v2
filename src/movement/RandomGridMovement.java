package movement;

import core.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class RandomGridMovement extends MovementModel {
    private static boolean isMapGenerated = false;
    private static final String HOME_X_SETTING = "homeX";
    private static final String HOME_Y_SETTING = "homeY";
    private static final String GRID_COUNT_X = "gridCountX";
    private static final String GRID_COUNT_Y = "gridCountY";

    private Coord lastWaypoint;
    private int currentGridX, currentGridY;

    private int homeX, homeY;
    private int gridCountX, gridCountY;

    public RandomGridMovement(Settings settings) {
        super(settings);

        if (settings.contains(HOME_X_SETTING)) {
            homeX = settings.getInt(HOME_X_SETTING);
        } else {
            homeX = 0;
        }

        if (settings.contains(HOME_Y_SETTING)) {
            homeY = settings.getInt(HOME_Y_SETTING);
        } else {
            homeY = 0;
        }

        if (settings.contains(GRID_COUNT_X)) {
            gridCountX = settings.getInt(GRID_COUNT_X);
        } else {
            gridCountX = 1;
        }

        if (settings.contains(GRID_COUNT_Y)) {
            gridCountY = settings.getInt(GRID_COUNT_Y);
        } else {
            gridCountY = 1;
        }

        if (!isMapGenerated) {
            autoGenerateMap(gridCountX, gridCountY);
            isMapGenerated = true;
        }
    }

    protected RandomGridMovement(RandomGridMovement grp) {
        super(grp);
        this.homeX = grp.homeX;
        this.homeY = grp.homeY;
        this.gridCountX = grp.gridCountX;
        this.gridCountY = grp.gridCountY;
    }

    // Untuk generate map secara otomatis
    private void autoGenerateMap(int gX, int gY) {
        try {
            // Ambil ukuran dunia simulasi dari settings
            Settings s = new Settings("MovementModel");
            double[] worldSize = s.getCsvDoubles("worldSize");
            int width = (int) worldSize[0];
            int height = (int) worldSize[1];

            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = image.createGraphics();

            // set warna background map
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, width, height);

            // set warna garis grid
            g2d.setColor(Color.GRAY);
            g2d.setStroke(new BasicStroke(25));

            int cellW = width / gX;
            for (int i = 1; i < gX; i++)
                g2d.drawLine(i * cellW, 0, i * cellW, height);

            int cellH = height / gY;
            for (int i = 1; i < gY; i++)
                g2d.drawLine(0, i * cellH, width, i * cellH);

            g2d.dispose();

            // Simpan ke folder data/spatial/
            File output = new File("data/spatial/auto_grid.png");
            if (!output.getParentFile().exists())
                output.getParentFile().mkdirs();
            ImageIO.write(image, "png", output);

            System.out.println("[AUTO-MAP] Peta " + width + "x" + height + " (" + gX + "x" + gY + ") berhasil dibuat!");
        } catch (Exception e) {
            System.err.println("[AUTO-MAP] Gagal membuat peta otomatis: " + e.getMessage());
        }
    }

    @Override
    public Coord getInitialLocation() {
        assert rng != null : "MovementModel not initialized!";
        currentGridX = homeX;
        currentGridY = homeY;

        Coord c = randomCoordInGrid(currentGridX, currentGridY);

        this.lastWaypoint = c;
        return c;
    }

    @Override
    public Path getPath() {
        Path p;
        p = new Path(generateSpeed());
        p.addWaypoint(lastWaypoint.clone());

        setNextGridElsewhere();
        Coord c = randomCoordInGrid(currentGridX, currentGridY);
        p.addWaypoint(c);

        this.lastWaypoint = c;
        return p;
    }

    private void setNextGridElsewhere() {
        if (gridCountX <= 1 && gridCountY <= 1) {
            return;
        }

        int rX, rY;
        do {
            rX = rng.nextInt(gridCountX);
            rY = rng.nextInt(gridCountY);
        } while ((rX == currentGridX && rY == currentGridY));

        currentGridX = rX;
        currentGridY = rY;
    }

    protected Coord randomCoordInGrid(int gridX, int gridY) {
        return new Coord(
                (rng.nextDouble() * getCoordX()) + (gridX * getCoordX()),
                (rng.nextDouble() * getCoordY()) + (gridY * getCoordY()));
    }

    private double getCoordX() {
        return getMaxX() / (double) gridCountX;
    }

    private double getCoordY() {
        return getMaxY() / (double) gridCountY;
    }

    @Override
    public MovementModel replicate() {
        return new RandomGridMovement(this);
    }
}