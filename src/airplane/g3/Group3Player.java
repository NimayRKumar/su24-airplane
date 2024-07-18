package airplane.g3;
import airplane.sim.Plane;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;


public class Group3Player extends airplane.sim.Player {

    private Logger logger = Logger.getLogger(this.getClass()); // for logging
    private HashMap<String, ArrayList<Integer>> departures = new HashMap();
    private HashMap<String, ArrayList<Integer>> arrivals = new HashMap();

    private double[] bearings;

    private HashMap<Integer, ArrayList<Integer>> bins;


    @Override
    public String getName() { return "Group 3 Player"; }

    @Override
    public void startNewGame(ArrayList<Plane> planes) {
        logger.info("Starting new game");
        bearings = new double[planes.size()];
    }

    private double getOppositeBearing(double bearing, int sign) {
        return validateBearing(bearing + sign * 180);
    }

    private double validateBearing(double bearing) {
        if (bearing >= 360) {
            bearing -= 360;
        }
        else if (bearing < 0) {
            bearing += 360;
        }

        return bearing;
    }

    private double getUpdatedBearing(double currBearing, double finalBearing) {
        double sign = Math.signum(finalBearing - currBearing);
        double diff = Math.min(10, Math.abs(finalBearing - currBearing));
        double newBearing = currBearing + sign * diff;

        return validateBearing(newBearing);
    }

    private boolean withinDistance(double x1, double y1, double x2, double y2, double radius) {
        double dist = Math.sqrt(Math.pow(x1 - x2, 2.0) + Math.pow(y1 - y2, 2.0));
        if (dist <= radius) {
            return true;
        }
        return false;
    }

    private void setDeparturesAndArrivals(ArrayList<Plane> planes) {
        for (int i = 0; i < planes.size(); ++i) {
            Plane pi = planes.get(i);
            String origin = pi.getLocation().toString();
            String dest = pi.getDestination().toString();

            if (departures.containsKey(origin)) {
                departures.get(origin).add(i);
            }
            else {
                departures.put(origin, new ArrayList<>(List.of(i)));
            }

            if (arrivals.containsKey(dest)) {
                arrivals.get(dest).add(i);
            }
            else {
                arrivals.put(dest, new ArrayList<>(List.of(i)));
            }
        }
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

    private HashMap<Integer, ArrayList<Integer>> binPlanes(ArrayList<Plane> planes) {
        HashMap<Integer, ArrayList<Integer>> bins = new HashMap<>();

        for (int i=0; i< planes.size(); ++i) {
            Plane pi = planes.get(i);
            double slope = (pi.getY() - pi.getDestination().getY()) / (pi.getX() - pi.getDestination().getX());
            double angle = validateBearing(Math.atan(slope) * 180 / Math.PI);
            int bin = (int) angle / 30;

            if (bins.get(bin) == null) {
                ArrayList<Integer> arr = new ArrayList<>();
                arr.add(i);
                bins.put(bin, arr);
            }
            else {
                bins.get(bin).add(i);
            }
        }

        return bins;
    }

    private HashMap<String, ArrayList<Integer>> detectCollisions2(ArrayList<Plane> planes, ArrayList<Integer> currPlanes, boolean[] collidingPlanes) {
        HashMap<String, ArrayList<Integer>> collisions = new HashMap<>();
        double[] x = new double[currPlanes.size()];
        double[] y = new double[currPlanes.size()];

        if (currPlanes.size() <= 1) {
            return collisions;
        }

        for(int i = 0; i < currPlanes.size(); ++i) {
            x[i] = planes.get(i).getX();
            y[i] = planes.get(i).getY();
        }

        while (true) {
            if (checkAllOutOfBounds(x, y)) {
                break;
            }

            for (int i = 0; i < currPlanes.size(); ++i) {
                for (int j = i+1; j < currPlanes.size(); ++j ) {
                    Integer pi = currPlanes.get(i);
                    Integer pj = currPlanes.get(j);
                    if (withinDistance(x[i], y[i], x[j], y[j], 5.0)) {
                        String key1 = pi + ";" + pj;
                        String key2 = pj + ";" + pi;
                        if (collisions.get(key1) == null && collisions.get(key2) == null) {
                            ArrayList<Integer> col = new ArrayList<>();
                            col.add(pi);
                            col.add(pj);
                            collisions.put(key1, col);
                            collidingPlanes[pi] = true;
                            collidingPlanes[pj] = true;
                        }
                    }
                }
            }

            //update positions based on trajectory & velocity
            for (int i=0; i<currPlanes.size(); ++i) {
                Plane pi = planes.get(currPlanes.get(i));
                double radialBearing = (pi.getBearing() - 90) * Math.PI/180;
                x[i] += Math.cos(radialBearing) * pi.getVelocity();
                y[i] += Math.sin(radialBearing) * pi.getVelocity();
            }
        }
        return collisions;
    }

    private double[] delaySubset(HashMap<String, ArrayList<Integer>> map, double[] delays) {
        for (String key : map.keySet()) {
            ArrayList<Integer> indices = map.get(key);
            int delay = 0;
            for (Integer i : indices) {
                delays[i] += 10 * delay;
                ++delay;
            }
        }
        return delays;
    }

    private double[] delayPlanes(ArrayList<Plane> planes, ArrayList<Integer> currPlanes) {
        double[] delays = new double[planes.size()];

        delaySubset(arrivals, delays);
        delaySubset(departures, delays);

        return delays;
    }

    @Override
    public double[] updatePlanes(ArrayList<Plane> planes, int round, double[] bearings) {
        double[] currBearings = bearings.clone();
        double[] newBearings = bearings.clone();
        double[] calcBearings = new double[planes.size()];
        boolean[] collidingPlanes = new boolean[planes.size()];

        HashMap<String, ArrayList<Integer>> collisions = null;

        if (round == 1) {
            this.bins = binPlanes(planes);
            setDeparturesAndArrivals(planes);
        }

        //if planes headed straight to destination, would it cause a crash?
        for (int i = 0; i < planes.size(); ++i) {
            Plane pi = planes.get(i);

            if (pi.getBearing() != -2) {
                calcBearings[i] = calculateBearing(pi.getLocation(), pi.getDestination());
                pi.setBearing(calcBearings[i]);
            }
        }

        ArrayList<Integer> currPlanes = null;

        for (Integer bin : this.bins.keySet()) {
            for (Integer i : this.bins.get(bin)) {
                Plane pi = planes.get(i);
                if (pi.getBearing() == -2) {
                    this.bins.get(bin).remove(i);
                }
            }

            if (this.bins.get(bin).isEmpty()) {
                this.bins.remove(bin);
            }
            else {
                currPlanes = this.bins.get(bin);
                break;
            }
        }

        double[] delays = delayPlanes(planes, currPlanes);

        //update only planes in current batch
        for (int i=0; i<planes.size(); ++i) {
            if (currPlanes.contains(i)) {
                Plane pi = planes.get(i);
                double newBearing = calculateBearing(pi.getLocation(), pi.getDestination());
                if (round < delays[i] && round < pi.getDepartureTime()) {
                    newBearings[i] = -1;
                    planes.get(i).setBearing(currBearings[i]);
                }
                else if (bearings[i] == -1 && round >= pi.getDepartureTime()) {
                    newBearings[i] = newBearing;
                } else if (pi.getBearing() != -1 && bearings[i] != -2 && round >= pi.getDepartureTime()) {
                    newBearings[i] = getUpdatedBearing(pi.getBearing(), newBearing);
                }
            }
            else {
                newBearings[i] = -1;
            }
        }

        if (round > 1 && planes.size() > 1) {
            collisions = detectCollisions2(planes, currPlanes, collidingPlanes);
        }

        //divert colliding planes
        if(collisions != null && !collisions.isEmpty()) {
            for (String collision : collisions.keySet()) {
                int i1 = collisions.get(collision).get(0);
                int i2 = collisions.get(collision).get(1);
                Plane p1 = planes.get(i1);
                Plane p2 = planes.get(i2);

                if (bearings[i1] != -1 && bearings[i1] != -2 && round >= delays[i1] && round >= p1.getDepartureTime()) {
                    newBearings[i1] = getUpdatedBearing(p1.getBearing(), 0);
                }
                else if (bearings[i1] != -2) {
                    if (round < delays[i1]) {
                        newBearings[i1] = -1;
                        p1.setBearing(currBearings[i1]);
                    }
                    else if (round >= p1.getDepartureTime()) {
                        newBearings[i1] = calculateBearing(p1.getLocation(), p1.getDestination());
                    }
                }
                if (bearings[i2] != -1 && bearings[i2] != -2 && round >= delays[i2] && round >= p2.getDepartureTime()) {
                    newBearings[i2] = getUpdatedBearing(p2.getBearing(), 90);
                }
                else if (bearings[i2] != -2) {
                    if (round < delays[i2]) {
                        newBearings[i2] = -1;
                        p2.setBearing(currBearings[i2]);
                    }
                    else if (round >= p2.getDepartureTime()) {
                        newBearings[i2] = calculateBearing(p2.getLocation(), p2.getDestination());
                    }
                }
            }
        }

        //divert non-colliding planes en route
        for (int i = 0; i < planes.size(); ++i) {
            if (collidingPlanes[i] || !currPlanes.contains(i)) {
                continue;
            }

            Plane pi = planes.get(i);
            double newBearing = calculateBearing(pi.getLocation(), pi.getDestination());
            if (round < delays[i] && round < pi.getDepartureTime()) {
                newBearings[i] = -1;
                planes.get(i).setBearing(currBearings[i]);
            }
            else if (bearings[i] == -1 && round >= pi.getDepartureTime()) {
                newBearings[i] = newBearing;
            } else if (pi.getBearing() != -1 && bearings[i] != -2 && round >= pi.getDepartureTime()) {
                newBearings[i] = getUpdatedBearing(pi.getBearing(), newBearing);
            }
        }

        return newBearings;
    }
}