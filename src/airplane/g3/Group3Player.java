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
                if (isSameDest(pi, pj) && pi.getBearing() != -2 && pj.getBearing() != -2) {
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


    private boolean checkOutOfBounds(double[] locs) {
        for (double l : locs) {
            if (l < 0 || l > 100)
                return true;
        }
        return false;
    }


    private boolean detectCollision(ArrayList<Plane> planes, double[] bearings) {
        Plane p0 = planes.get(0);
        Plane p1 = planes.get(1);
        double x0 = p0.getX();
        double y0 = p0.getY();
        double x1 = p1.getX();
        double y1 = p1.getY();

        while (true) {
            if (checkOutOfBounds(new double[]{x0, y0, y0, y1})) {
                return false;
            }

            double dist = Math.sqrt(Math.pow(x1 - x0, 2.0) + Math.pow(y1 - y0, 2.0));

            if (dist <= 5.0) {
                return true;
            }

            double rb0 = (bearings[0] - 90) * Math.PI/180;
            double rb1 = (bearings[1] - 90) * Math.PI/180;
            x0 += Math.cos(rb0) * p0.getVelocity();
            y0 += Math.sin(rb0) * p0.getVelocity();

            x1 += Math.cos(rb1) * p1.getVelocity();
            y1 += Math.sin(rb1) * p1.getVelocity();
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
