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
    private boolean[] getConvergentPlanes(ArrayList<Plane> planes) {
        ArrayList<Plane> convergentPlanes = new ArrayList<>();
        boolean[] convergentIndices = new boolean[planes.size()];

        for (int i = 0; i < planes.size() - 1; ++i) {
            for (int j = i+1; j < planes.size(); ++j) {
                Plane pi = planes.get(i);
                Plane pj = planes.get(j);

                //if planes have same destination and neither are already landed
                if (pi.getDestination().equals(pj.getDestination()) && pi.getBearing() != -2 && pj.getBearing() != -2) {
                    convergentPlanes.add(pi);
                    convergentIndices[i] = true;
                    convergentIndices[j] = true;
                }
            }
        }

        return convergentIndices;
        //return convergentPlanes;
    }
    @Override
    public double[] updatePlanes(ArrayList<Plane> planes, int round, double[] bearings) {
        final double SAFETY_RADIUS = 5.0; // Adjusted safety radius
        boolean[] convergentIndices = getConvergentPlanes(planes);

        double[] delays = new double[planes.size()];
        for (int i = 0; i < planes.size(); i++) {
            Plane p = planes.get(i);
           //System.err.println(convergentIndices[i]);
            if(convergentIndices[i] && p.getBearing() == -1){
                int adjustedDepartureTime = p.getDepartureTime() + (i * 10);
                if (round >= adjustedDepartureTime) {
                    bearings[i] = calculateBearing(p.getLocation(), p.getDestination());
                }
            }
            else if (p.getBearing() == -1 && round >= p.getDepartureTime()) {
                System.err.println(p.getDepartureTime());
                bearings[i] = calculateBearing(p.getLocation(), p.getDestination());
            }
            else if (p.getBearing() != -1 && p.getBearing() != -2) {
                boolean adjusted = false;

                // Check for potential collision with other planes
                for (Plane l1 : planes) {
                    for (Plane l2 : planes) {
                        if (!l1.equals(l2) && l1.getBearing() != -2 && l1.getBearing() != -1 && l2.getBearing() != -2 && l2.getBearing() != -1) {
                            if (l1.getLocation().distance(l2.getLocation()) < SAFETY_RADIUS + 7) {
                                bearings[i] += 10;
                                if (bearings[i] > 360) {
                                    bearings[i] = bearings[i] % 360;
                                }
                                adjusted = true;
                                break;
                            }
                        }
                    }
                    if (adjusted) break;
                }

                // If no collision adjustment was made, adjust bearing toward destination
                if (!adjusted) {
                    double needed = calculateBearing(p.getLocation(), p.getDestination());
                    double currentBearing = bearings[i];
                    double difference = needed - currentBearing;

                    // Normalize the difference to the range [-180, 180]
                    if (difference > 180) {
                        difference -= 360;
                    } else if (difference < -180) {
                        difference += 360;
                    }

                    if (Math.abs(difference) <= 10) {
                        bearings[i] = needed;
                    } else if (difference > 0) {
                        bearings[i] += 10;
                    } else {
                        bearings[i] -= 10;
                    }

                    if (bearings[i] < 0) {
                        bearings[i] += 360;
                    } else if (bearings[i] >= 360) {
                        bearings[i] -= 360;
                    }
                }
            }
        }
        return bearings;
    }
}
