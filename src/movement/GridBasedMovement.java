package movement;

import core.*;

public class GridBasedMovement extends MovementModel {
    private double minX, maxX, minY, maxY;
    private int gridX, gridY;
    private int cols, rows;
    private Coord lastWaypoint;

    private static int currentId = 0;

    private static final int PATH_LENGTH = 1;

    public static final String GLOBAL_NAMESPACE = "GridBasedRandomWaypoint";
    public static final String NROF_GRID_X_S = "nrofGridX";
    public static final String NROF_GRID_Y_S = "nrofGridY";

    public GridBasedMovement(Settings settings) {
        super(settings);
        Settings globalSettings = new Settings(GLOBAL_NAMESPACE);

        this.cols = getSettingInt(globalSettings, settings, NROF_GRID_X_S, 3);
        this.rows = getSettingInt(globalSettings, settings, NROF_GRID_Y_S, 3);

        assignGrid();
        calculateBounds();
    }

    public GridBasedMovement(GridBasedMovement proto) {
        super(proto);
        this.gridX = proto.gridX;
        this.gridY = proto.gridY;
        this.cols = proto.cols;
        this.rows = proto.rows;

        calculateBounds();
    }

    private int getSettingInt(Settings global, Settings group, String key, int def) {
        if (global.contains(key))
            return global.getInt(key);
        if (group.contains(key))
            return group.getInt(key);
        return def;
    }

    private void assignGrid() {
        int totalGrids = this.cols * this.rows;

        int pickedGridId = currentId % totalGrids;

        this.gridX = pickedGridId % this.cols;
        this.gridY = pickedGridId / this.cols;
    }

    private void calculateBounds() {
        double sizeX = getMaxX() / cols;
        double sizeY = getMaxY() / rows;

        this.minX = this.gridX * sizeX;
        this.maxX = (this.gridX + 1) * sizeX;
        this.minY = this.gridY * sizeY;
        this.maxY = (this.gridY + 1) * sizeY;
    }
    // ------------------------------------------------------

    @Override
    public Coord getInitialLocation() {
        assert rng != null : "MovementModel not initialized!";
        Coord c = randomCoord();
        this.lastWaypoint = c;
        return c;
    }

    @Override
    public Path getPath() {
        Path p = new Path(generateSpeed());
        p.addWaypoint(lastWaypoint.clone());
        Coord c = lastWaypoint;

        for (int i = 0; i < PATH_LENGTH; i++) {
            c = randomCoord();
            p.addWaypoint(c);
        }

        this.lastWaypoint = c;
        return p;
    }

    @Override
    public GridBasedMovement replicate() {
        GridBasedMovement next = new GridBasedMovement(this);

        // PERBAIKAN: Tentukan grid host baru ini, lalu naikkan ID-nya
        next.assignGrid();
        currentId++;
        next.calculateBounds();

        return next;
    }

    protected Coord randomCoord() {
        double x = rng.nextDouble() * (maxX - minX) + minX;
        double y = rng.nextDouble() * (maxY - minY) + minY;
        return new Coord(x, y);
    }
}