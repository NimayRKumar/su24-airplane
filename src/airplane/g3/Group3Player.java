package airplane.g3;
import airplane.sim.Plane;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;


public class Group3Player extends airplane.sim.Player {

    private Logger logger = Logger.getLogger(this.getClass()); // for logging
    private HashMap<Integer, ArrayList<Integer>> bins;

    private int delayMultiplier = 10;
    private int takeOffDelay = 5;

    private boolean[] divergentIndices;

    private HashMap<String, ArrayList<Integer>> departures = new HashMap();
    @Override
    public String getName() { return "Group 3 Player"; }

    @Override
    public void startNewGame(ArrayList<Plane> planes) {
        logger.info("Starting new game");
        if (planes.size() >= 5) {
            this.delayMultiplier = 20;
            this.takeOffDelay = 25;
        }
    }

    private void resetBearing(ArrayList<Plane> planes, double[] bearings) {
        for (int i=0; i<planes.size(); ++i) {
            planes.get(i).setBearing(bearings[i]);
        }
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

    private HashMap<Integer, ArrayList<Integer>> binPlanes(ArrayList<Plane> planes) {
        HashMap<Integer, ArrayList<Integer>> bins = new HashMap<>();

        for (int i=0; i< planes.size(); ++i) {
            Plane pi = planes.get(i);
            double slope = (pi.getY() - pi.getDestination().getY()) / (pi.getX() - pi.getDestination().getX());

            double signX = Math.signum(pi.getX() - pi.getDestination().getX());
            double signY = Math.signum(pi.getY() - pi.getDestination().getY());
            int bin;

            //going right
            if (signX == -1) {
                //going down
                if (signY == -1) {
                    bin = 1;
                }
                //going up
                else if (signY == 1) {
                    bin = 7;
                }
                //going horizontally right
                else {
                    bin = 0;
                }
            }
            //going left
            else if (signX == 1) {
                //going down
                if (signY == -1) {
                    bin = 3;
                }
                //going up
                else if (signY == 1) {
                    bin = 5;
                }
                //going horizontally left
                else {
                    bin = 4;
                }
            }
            //going vertically down
            else if (signY == -1){
                bin = 2;
            }
            //going vertically up
            else {
                bin = 6;
            }

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

    private boolean isSameDepart(Plane p1, Plane p2) {
        return ((p1.getLocation().x == p2.getLocation().x) && (p1.getLocation().y == p2.getLocation().y));
    }
    private boolean isSameDest(Plane p1, Plane p2) {
        return ((p1.getDestination().x == p2.getDestination().x) && (p1.getDestination().y == p2.getDestination().y));
    }

    private boolean[] getDivergentPlanes(ArrayList<Plane> planes) {
        boolean[] divergentIndices = new boolean[planes.size()];

        for (int i = 0; i < planes.size() - 1; ++i) {
            for (int j = i+1; j < planes.size(); ++j) {
                Plane pi = planes.get(i);
                Plane pj = planes.get(j);

                //if planes have same destination and neither are already landed
                if (isSameDepart(pi, pj) && pi.getBearing() != -2 && pj.getBearing() != -2) {
                    divergentIndices[i] = true;
                    divergentIndices[j] = true;
                }
            }
        }

        return divergentIndices;
    }

    private void setDepartures(ArrayList<Plane> planes) {
        for (int i = 0; i < planes.size(); ++i) {
            Plane pi = planes.get(i);
            String origin = pi.getLocation().toString();

            if (departures.containsKey(origin)) {
                departures.get(origin).add(i);
            }
            else {
                departures.put(origin, new ArrayList<>(List.of(i)));
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

    private boolean detectCollision(ArrayList<Plane> planes, int steps) {
        double[] xVals = new double[planes.size()];
        double[] yVals = new double[planes.size()];
        int count = 0;

        for(int i = 0; i < planes.size(); ++i) {
            xVals[i] = planes.get(i).getX();
            yVals[i] = planes.get(i).getY();
        }

        while (true) {
            ++count;
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

            if (count > steps && steps > 0) {
                break;
            }
        }

        return false;
    }

    private boolean canTakeOff(ArrayList<Plane> planes, int pi, double[] bearings, double[] delays, int round) {
        Plane p1 = planes.get(pi);

        for(int i=0; i<planes.size(); ++i) {
            Plane p2 = planes.get(i);

            if(i != pi && bearings[i] == -1 && bearings[pi] == -1 && (isSameDepart(p1, p2) || isSameDest(p1, p2))) {
                delays[i] = round + this.takeOffDelay;
                continue;
            }

            if (i == pi || bearings[pi] == -2 || bearings[i] == -1 || bearings[i] == -2){
                continue;
            }

            double x1 = p1.getX();
            double y1 = p1.getY();
            double x2 = p2.getX();
            double y2 = p2.getY();
            double dist = Math.sqrt(Math.pow(x1 - x2, 2.0) + Math.pow(y1 - y2, 2.0));
            if (dist <= 50.0) {
                return false;
            }

        }
        return true;
    }

    @Override
    public double[] updatePlanes(ArrayList<Plane> planes, int round, double[] bearings) {
        double[] newBearings = bearings.clone();
        double[] calcBearings = new double[planes.size()];
        boolean collision = false;

        double[] delays = new double[planes.size()];

        if (round == 1) {
            this.divergentIndices = getDivergentPlanes(planes);
            setDepartures(planes);
            if (planes.size() >= 5) {
                this.bins = binPlanes(planes);
            }
            else {
                ArrayList<Integer> bin = new ArrayList<>();
                for(int i=0; i<planes.size(); ++i){
                    bin.add(i);
                }
                HashMap<Integer, ArrayList<Integer>> bins = new HashMap<>();
                bins.put(0, bin);
                this.bins = bins;
            }
        }

        //if planes headed straight to destination, would it cause a crash?
        for (int i = 0; i < planes.size(); ++i) {
            Plane pi = planes.get(i);

            if (pi.getBearing() != -2) {
                calcBearings[i] = calculateBearing(pi.getLocation(), pi.getDestination());
                pi.setBearing(calcBearings[i]);
            }
        }

        if (round > 1 && planes.size() > 1) {
            collision = detectCollision(planes, -1);
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

        if(currPlanes.size() > 2) {
            for (int i = 0; i < currPlanes.size(); ++i) {
                delays[currPlanes.get(i)] = this.delayMultiplier * i;
            }
        }

        //crash - divert planes from each other
        if(collision) {
            for (int i = 0; i < planes.size(); ++i) {
                if (!currPlanes.contains(i)) {
                    continue;
                }
                Plane pi = planes.get(i);
                if (bearings[i] != -1 && bearings[i] != -2 && round >= delays[i]) {
                    if (i % 2 == 0) {
                        newBearings[i] = getUpdatedBearing(pi.getBearing(), 0);
                    }
                    else {
                        newBearings[i] = getUpdatedBearing(pi.getBearing(), 90);
                    }
                }
                else if (bearings[i] != -2) {
                    //keep delayed planes grounded
                    if (round < delays[i] || !canTakeOff(planes, i, bearings, delays, round)) {
                        newBearings[i] = -1;
                    }
                    else {
                        newBearings[i] = calculateBearing(pi.getLocation(), pi.getDestination());
                    }
                }
            }
        }
        //no crash - divert planes back on route
        else {
            for (int i = 0; i < planes.size(); ++i) {
                if (!currPlanes.contains(i)) {
                    continue;
                }

                Plane pi = planes.get(i);
                double newBearing = calculateBearing(pi.getLocation(), pi.getDestination());
                if (bearings[i] == -1 && round >= pi.getDepartureTime() && round >= delays[i]) {
                    newBearings[i] = newBearing;
                } else if (pi.getBearing() != -1 && bearings[i] != -2 && round >= pi.getDepartureTime() && round >= delays[i]) {
                    newBearings[i] = getUpdatedBearing(pi.getBearing(), newBearing);
                }
            }
        }

        resetBearing(planes, newBearings);
        return newBearings;
    }
}