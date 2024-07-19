package airplane.g3;

import airplane.sim.Plane;

import java.util.ArrayList;
import java.util.List;
import java.util.ArrayList;
import org.apache.log4j.Logger;
import airplane.sim.Plane;
import airplane.sim.Player;


public class Group3Player extends airplane.sim.Player {

    private Logger logger = Logger.getLogger(this.getClass()); // for logging
    private List<List<Plane>> bearingBuckets;
    public int[] delayTime = new int[100];
    @Override
    public String getName() { return "Group 3 Player"; }

    @Override
    public void startNewGame(ArrayList<Plane> planes) {
        logger.info("Starting new game");
/*
        // Place planes into buckets based on their initial bearing
        for (Plane p : planes) {
            double bearing = calculateBearing(p.getLocation(), p.getDestination());
            int bucketIndex = (int) (bearing / 45) % 8;
            bearingBuckets.get(bucketIndex).add(p);
        }


        bearingBuckets = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            bearingBuckets.add(new ArrayList<>());
        }
*/
        // Place planes into buckets based on their initial bearing
        double[] minna = new double[50];
        double bearing=-1;
        for (int i = 0; i < planes.size(); i++) {
                bearing = calculateBearing(planes.get(i).getLocation(), planes.get(i).getDestination());
                minna[i] = bearing;
                if (bearing > 315) {
                    delayTime[i] = 1;
                }
                else if (bearing > 270) { delayTime[i] = 350; }
                else if (bearing > 225) { delayTime[i] = 300; }
                else if (bearing > 180) { delayTime[i] = 250; }
                else if (bearing > 135) { delayTime[i] = 200; }
                else if (bearing > 90) { delayTime[i] = 150; }
                else if (bearing > 45) { delayTime[i] = 100; }
                else  { delayTime[i] = 350; }
        }
        return;
/*
        // Initialize departure time array
        departureTime = new double[planes.size()];
        // Calculate departure times
        for (int i = 0; i < bearingBuckets.size(); i++) {
            List<Plane> bucket = bearingBuckets.get(i);
            double maxDepartureTime = 0;

            // Find the maximum departure time in this bucket
            for (Plane p : bucket) {
                double bearing = calculateBearing(p.getLocation(), p.getDestination());
                int index = planes.indexOf(p);
                maxDepartureTime = Math.max(maxDepartureTime, p.getDepartureTime());
            }

            // Set departure times for planes in this bucket
            for (int j = 0; j < bucket.size(); j++) {
                Plane p = bucket.get(j);
                int planeIndex = planes.indexOf(p);
                departureTime[planeIndex] = maxDepartureTime + j; // Set departure time based on bucket and index
            }
        }
        */
    }
    private boolean[] getConvergentPlanes(ArrayList<Plane> planes) {
        ArrayList<Plane> convergentPlanes = new ArrayList<>();
        boolean[] convergentIndices = new boolean[planes.size()];

        for (int i = 0; i < planes.size() - 1; ++i) {
            for (int j = i+1; j < planes.size(); ++j) {
                Plane pi = planes.get(i);
                Plane pj = planes.get(j);

                //if planes have same destination and neither are already landed
                if ((pi.getDestination().equals(pj.getDestination()) || pi.getDestination().distance(pj.getDestination()) < 10) && pi.getBearing() != -2 && pj.getBearing() != -2) {
                    convergentPlanes.add(pi);
                    convergentIndices[i] = true;
                    convergentIndices[j] = true;
                }
            }
        }

        return convergentIndices;
        //return convergentPlanes;
    }
    private boolean checkAllOutOfBounds(double[] xVals, double[] yVals) {
        int withinBounds = 0;
        for (int i=0; i<xVals.length; ++i) {
            double xi = xVals[i];
            double yi = yVals[i];
            if(xi >= 0 && xi <= 100 && yi >= 0 && yi <= 100) {
                ++withinBounds;
            }
        }

        return (withinBounds == 0);
    }
    private boolean detectCollision(ArrayList<Plane> planes, double[] bearings) {
        double[] xVals = new double[planes.size()];
        double[] yVals = new double[planes.size()];

        for(int i = 0; i < planes.size(); ++i) {
            xVals[i] = planes.get(i).getX();
            yVals[i] = planes.get(i).getY();
        }

        while (true) {
            if (checkAllOutOfBounds(xVals, yVals)) {
                return false;
            }

            for (int i = 0; i < planes.size(); ++i) {
                for (int j = i+1; j < planes.size(); ++j ) {
                    double dist = Math.sqrt(Math.pow(xVals[i] - xVals[j], 2.0) + Math.pow(yVals[i] - yVals[j], 2.0));
                    if (dist <= 5.0) {
                        return true;
                    }
                }
            }

            //update positions based on trajectory & velocity
            for (int i = 0; i < planes.size(); ++i) {
                Plane pi = planes.get(i);
                double radialBearing = (pi.getBearing() - 90) * Math.PI/180;
                xVals[i] += Math.cos(radialBearing) * pi.getVelocity();
                yVals[i] += Math.sin(radialBearing) * pi.getVelocity();
            }
        }
    }
    @Override
    public double[] updatePlanes(ArrayList<Plane> planes, int round, double[] bearings) {

        for (int i = 0; i < planes.size(); i++) {
            Plane p = planes.get(i);

            //System.err.println(convergentIndices[i]);
            // if(convergentIndices[i]){
            //   int adjustedDepartureTime = p.getDepartureTime() + (i * 10);
            // if ( p.getBearing() == -1 && round >= adjustedDepartureTime) {
            //   bearings[i] = calculateBearing(p.getLocation(), p.getDestination());
            //}
            //}
            if (p.getBearing() == -1 && p.getBearing() != -2 && round >= (p.getDepartureTime() + delayTime[i])
                   // && round >= delayTime[i]
                    &&  p.dependenciesHaveLanded(bearings)) {
                //  System.err.println(p.getDepartureTime());
                bearings[i] = calculateBearing(p.getLocation(), p.getDestination());
            }
        }
        return bearings;
    }
}
