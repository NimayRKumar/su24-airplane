package airplane.g3;

import airplane.sim.Plane;

import java.util.ArrayList;
import java.util.ArrayList;
import org.apache.log4j.Logger;
import airplane.sim.Plane;
import airplane.sim.Player;


public class Group3Player extends airplane.sim.Player {

    private Logger logger = Logger.getLogger(this.getClass()); // for logging

    @Override
    public String getName() { return "Group 3 Player"; }

    @Override
    public void startNewGame(ArrayList<Plane> planes) {
        logger.info("Starting new game");
    }

    @Override
    public double[] updatePlanes(ArrayList<Plane> planes, int round, double[] bearings) {
        return new double[0];
    }
}
