package movement;

import core.*;
import java.util.ArrayList;
import java.util.List;

public class GatheringnCommunityMovement extends MovementModel {
    private int cols, rows;
    private int totalGrids;
    private int gatheringGridId;
    private Coord lastWaypoint;

    private int homeGridId;
    private int currentGridId;

    private double probHomeToGathering;
    private double probHomeToElse;
    private double probElseToHome;
    private double probElseToElse;

    public static final String NROF_GRID_X_S = "nrofGridX";
    public static final String NROF_GRID_Y_S = "nrofGridY";
    public static final String GATHERING_GRID_S = "gatheringGridId";

    public static final String HOME_X_S = "homeX";
    public static final String HOME_Y_S = "homeY";
    public static final String GATHERING_X_S = "gatheringX";
    public static final String GATHERING_Y_S = "gatheringY";

    public static final String P_HOME_GATHER_S = "probHomeToGathering";
    public static final String P_HOME_ELSE_S = "probHomeToElse";
    public static final String P_ELSE_HOME_S = "probElseToHome";
    public static final String P_ELSE_ELSE_S = "probElseToElse";

    public GatheringnCommunityMovement(Settings settings) {
        super(settings);

        // Set up grid size
        this.cols = settings.contains(NROF_GRID_X_S) ? settings.getInt(NROF_GRID_X_S) : 4;
        this.rows = settings.contains(NROF_GRID_Y_S) ? settings.getInt(NROF_GRID_Y_S) : 3;
        this.totalGrids = this.cols * this.rows;

        // Set up gathering location (default is last grid)
        int gX = settings.contains(GATHERING_X_S) ? settings.getInt(GATHERING_X_S) : (this.cols - 1);
        int gY = settings.contains(GATHERING_Y_S) ? settings.getInt(GATHERING_Y_S) : (this.rows - 1);
        this.gatheringGridId = (gY * this.cols) + gX;

        // Set up home location
        int hX = settings.contains(HOME_X_S) ? settings.getInt(HOME_X_S) : 0;
        int hY = settings.contains(HOME_Y_S) ? settings.getInt(HOME_Y_S) : 0;
        this.homeGridId = (hY * this.cols) + hX;

        // Set destination selection probabilities, or use default values
        this.probHomeToGathering = settings.contains(P_HOME_GATHER_S) ? settings.getDouble(P_HOME_GATHER_S) : 0.8;
        this.probHomeToElse = settings.contains(P_HOME_ELSE_S) ? settings.getDouble(P_HOME_ELSE_S) : 0.2;
        this.probElseToHome = settings.contains(P_ELSE_HOME_S) ? settings.getDouble(P_ELSE_HOME_S) : 0.9;
        this.probElseToElse = settings.contains(P_ELSE_ELSE_S) ? settings.getDouble(P_ELSE_ELSE_S) : 0.1;

        // Current location starts at home
        this.currentGridId = this.homeGridId;
    }

    // Copy constructor for replication
    public GatheringnCommunityMovement(GatheringnCommunityMovement proto) {
        super(proto);
        this.cols = proto.cols;
        this.rows = proto.rows;
        this.totalGrids = proto.totalGrids;
        this.gatheringGridId = proto.gatheringGridId;

        this.probHomeToGathering = proto.probHomeToGathering;
        this.probHomeToElse = proto.probHomeToElse;
        this.probElseToHome = proto.probElseToHome;
        this.probElseToElse = proto.probElseToElse;

        this.homeGridId = proto.homeGridId;
        this.currentGridId = proto.currentGridId;
    }

    @Override
    public Path getPath() {
        Path p = new Path(generateSpeed());
        p.addWaypoint(lastWaypoint.clone());

        double r = rng.nextDouble();
        int nextGridId;

        if (this.currentGridId == this.homeGridId) { // Currently at home
            if (r < probHomeToGathering) { 
                nextGridId = this.gatheringGridId; // Go to gathering (0.8 probability)
            } else {
                nextGridId = getRandomElsewhere(); // Go to random elsewhere (0.2 probability)
            }
        } else { // Currently elsewhere (including gathering)
            if (r < probElseToHome) {
                nextGridId = this.homeGridId; // Go back home (0.9 probability)
            } else {
                nextGridId = getRandomElsewhere(); // Go to random elsewhere (0.1 probability)
            }
        }

        this.currentGridId = nextGridId; // Update current location
        Coord c = getRandomCoordInGrid(nextGridId); // Get random coordinate in the selected grid
        p.addWaypoint(c);
        this.lastWaypoint = c;
        return p;

    }

    @Override
    public Coord getInitialLocation() {
        Coord c = getRandomCoordInGrid(this.homeGridId);
        this.lastWaypoint = c;
        return c;
    }

    private int getRandomElsewhere() {
        List<Integer> elsewhereList = new ArrayList<>();
        for (int i = 0; i < totalGrids; i++) { // Exclude home, gathering, and current grid
            if (i == this.homeGridId || i == this.gatheringGridId || i == this.currentGridId) { // Skip home, gathering, and current grid
                continue;
            }
            elsewhereList.add(i); // Add all other grids to the list of possible elsewhere locations
        }

        if (elsewhereList.isEmpty()) // If there are no other grids available
            return this.homeGridId; // Just incase cant stay in current grid, go home
        
        return elsewhereList.get(rng.nextInt(elsewhereList.size())); // Randomly select one of the available elsewhere grids
    }

    private Coord getRandomCoordInGrid(int gridId) {
        double sizeX = getMaxX() / cols;
        double sizeY = getMaxY() / rows;

        int col = gridId % cols;
        int row = gridId / cols;

        double minX = col * sizeX;
        double maxX = (col + 1) * sizeX;
        double minY = row * sizeY;
        double maxY = (row + 1) * sizeY;

        double x = rng.nextDouble() * (maxX - minX) + minX;
        double y = rng.nextDouble() * (maxY - minY) + minY;
        return new Coord(x, y);
    }

    @Override
    public GatheringnCommunityMovement replicate() {
        return new GatheringnCommunityMovement(this);
    }
}
