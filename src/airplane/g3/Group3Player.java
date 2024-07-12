package airplane.g3;
import airplane.sim.Plane;

import java.awt.*;
import java.util.ArrayList;

import org.apache.log4j.Logger;


public class Group3Player extends airplane.sim.Player {

    private Logger logger = Logger.getLogger(this.getClass()); // for logging

    @Override
    public String getName() { return "Group 3 Player"; }

    @Override
    public void startNewGame(ArrayList<Plane> planes) {
        logger.info("Starting new game");
    }

    private double getUpdatedBearing(double currBearing, double finalBearing) {
        double sign = Math.signum(finalBearing - currBearing);
        double diff = Math.min(10, Math.abs(finalBearing - currBearing));
        double newBearing = currBearing + sign * diff;

        if (newBearing >= 360) {
            newBearing -= 360;
        }
        else if (newBearing < 0) {
            newBearing += 360;
        }

        return newBearing;
    }

    private void resetBearings(ArrayList<Plane> planes, double[] bearings) {
        for (int i = 0; i < planes.size(); ++i) {
            planes.get(i).setBearing(bearings[i]);
        }
    }

    private Point calculateIntersectionPoint(double m0, double b0, double m1, double b1) {
        if (m0 == m1) {
            return null;
        }

        double x = (b1 - b0) / (m1 - m0);
        double y = m0 * x + b0;

        Point point = new Point();
        point.setLocation(x, y);
        return point;
    }

    private int getClosestMultiple(int n, int x) {
        if (x > n) {
            return x;
        }

        n += x / 2;
        n -= (n % x);
        return n;
    }

    private static void sortPlanesByDeparture(ArrayList<Plane> planes) {
        planes.sort((p1, p2) -> {
            if (p1.getDepartureTime() < p2.getDepartureTime()) {
                return 1;
            } else if (p1.getDepartureTime() > p2.getDepartureTime()) {
                return -1;
            }
            return 0;
        });
    }

    private boolean isSameDepart(Plane p1, Plane p2) {
        return ((p1.getLocation().x == p2.getLocation().x) && (p1.getLocation().y == p2.getLocation().y));
    }
    private boolean isSameDest(Plane p1, Plane p2) {
        return ((p1.getDestination().x == p2.getDestination().x) && (p1.getDestination().y == p2.getDestination().y));
    }

    private boolean[] getConvergentPlanes(ArrayList<Plane> planes) {
        ArrayList<Plane> convergentPlanes = new ArrayList<>();
        boolean[] convergentIndices = new boolean[planes.size()];

        for (int i = 0; i < planes.size() - 1; ++i) {
            for (int j = i+1; j < planes.size(); ++j) {
                Plane pi = planes.get(i);
                Plane pj = planes.get(j);

                //if planes have same destination and neither are already landed
                if (isSameDepart(pi, pj) || (isSameDest(pi, pj)) && pi.getBearing() != -2 && pj.getBearing() != -2) {
                    convergentPlanes.add(pi);
                    convergentIndices[i] = true;
                    convergentIndices[j] = true;
                }
            }
        }

        sortPlanesByDeparture(convergentPlanes);
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

    private double updateBearing(Plane plane, int index, int round, int delay, double currBearing) {
        double newBearing = 0.0;

        if (plane.getBearing() != -1 && plane.getBearing() != -2 && round >= delay) {
            if (index % 2 == 0) {
                newBearing = getUpdatedBearing(plane.getBearing(), 0);
            }
            else {
                newBearing = getUpdatedBearing(plane.getBearing(), 90);
            }
        }
        else {
            newBearing = currBearing;
        }

        return newBearing;
    }

    @Override
    public double[] updatePlanes(ArrayList<Plane> planes, int round, double[] bearings) {
        double[] newBearings = bearings.clone();
        double[] calcBearings = new double[planes.size()];
        boolean collision = false;

        //separate planes headed to same destination vs those headed to different destinations

        //ArrayList<Plane> convergentPlanes = getConvergentPlanes(planes);
        boolean[] convergentIndices = getConvergentPlanes(planes);
        //ArrayList<Plane> divergentPlanes = new ArrayList<>(planes);
        //divergentPlanes.removeAll(convergentPlanes);
        //ArrayList<Plane> delayedPlanes = new ArrayList<>();
        double[] delays = new double[planes.size()];

        //if planes headed straight to destination, would it cause a crash?
        for (int i = 0; i < planes.size(); ++i) {
            Plane pi = planes.get(i);

            if (pi.getBearing() != -2) {
                calcBearings[i] = calculateBearing(pi.getLocation(), pi.getDestination());
                pi.setBearing(calcBearings[i]);
            }
        }
        if (round > 1 && planes.size() > 1) {
            collision = detectCollision(planes, calcBearings);
        }

        for (int i = 1; i < planes.size(); ++i) {
            if (convergentIndices[i]) {
                delays[i] = 5 * i;
            }
        }

        //crash - divert planes from each other
        if(collision) {
            for (int i = 0; i < planes.size(); ++i) {
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
                    if (round < delays[i]) {
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
                Plane pi = planes.get(i);
                double newBearing = calculateBearing(pi.getLocation(), pi.getDestination());
                if (bearings[i] == -1 && round >= pi.getDepartureTime()) {
                    newBearings[i] = newBearing;
                } else if (pi.getBearing() != -1 && bearings[i] != -2 && round >= pi.getDepartureTime()) {
                    newBearings[i] = getUpdatedBearing(pi.getBearing(), newBearing);
                }
            }
        }

        return newBearings;
    }
}
