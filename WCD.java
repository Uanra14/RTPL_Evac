import java.io.IOException;
import java.util.List;
import com.gurobi.gurobi.GRB;
import com.gurobi.gurobi.GRBEnv;
import com.gurobi.gurobi.GRBException;
import com.gurobi.gurobi.GRBLinExpr;
import com.gurobi.gurobi.GRBModel;
import com.gurobi.gurobi.GRBVar;

/**
 * Class containing the WCD model
 * 
 * @author 562606ad
 */
public class WCD {
    public GRBModel model;
    public GRBEnv env;

    public GRBVar[] excessDemand;
    public GRBVar[] accumulatedDemand;
    public GRBVar[] demand;
    public GRBVar[] thereIsExcessDemand;
    public GRBVar[][] isClosestPickUp;
    public GRBVar[][] demandRealised;
    public GRBVar[][][] typeDemandRealised;

    public int numDP;
    public int numDV;
    public int numSh;
    
    public double[] isPickUpLocationValues;
    public double[][] isClosestPickUpValues;
    public double[][][] tripsValues;

    /**
     * Constructor for the WCD model
     * @param isPickUpLocation Array of binary variables indicating whether a demand point is a pick-up location
     * @param isClosestPickUp Array of binary variables indicating whether a demand point is the closest pick-up location to another demand point
     * @param trips Array of binary variables indicating whether a bus travels from a demand point to a shelter
     * @param busCap Capacity of the buses
     * @param numBus Number of buses
     * @param pess Degree of pessimism
     * @param demandVectors List of the possible vectors of demand for each demand point
     * @param longestWalk Maximum allowed walking time
     * @param maxT Maximum allowed driving time for the buses
     * @param times Matrix containing the driving times from each demand point to each shelter
     * @param walkingTimes Matrix containing the walking times between each pair of nodes
     * @param selectedDemandVectors List of selected demand vectors
     * @throws IOException
     */
    public WCD(GRBVar[] isPickUpLocation, GRBVar[][] isClosestPickUp, GRBVar[][][] trips,
    int busCap, int numBus, int pess, List<Integer[]> demandVectors, int longestWalk,
    int maxT, int[][] times, int[][] walkingTimes, List<Integer[]> selectedDemandVectors,
    int[] shelterCap) throws IOException {

        this.numDP = isPickUpLocation.length;
        this.numDV = demandVectors.size(); // possible values for a demand point
        this.numSh = shelterCap.length;
        
        this.excessDemand = new GRBVar[numDP];
        this.accumulatedDemand = new GRBVar[numDP];
        this.demand = new GRBVar[numDP];
        this.thereIsExcessDemand = new GRBVar[numDP];
        this.demandRealised = new GRBVar[numDP][numDV];
        this.isPickUpLocationValues = new double[numDP];
        this.isClosestPickUpValues = new double[numDP][numDP];
        this.tripsValues = new double[numBus][numDP][numSh];

        try {
            this.env = new GRBEnv();
            this.model = new GRBModel(env);

            for (int i = 0; i < numDP; i++) {
                isPickUpLocationValues[i] = isPickUpLocation[i].get(GRB.DoubleAttr.X);

                for (int p = 0; p < numDP; p++) {
                    isClosestPickUpValues[i][p] = isClosestPickUp[i][p].get(GRB.DoubleAttr.X);
                }
                for (int b = 0; b < numBus; b++) {
                    for (int j = 0; j < numSh; j++) {
                        tripsValues[b][i][j] = trips[b][i][j].get(GRB.DoubleAttr.X);
                    }
                }
            }

            for (int i = 0; i < numDP; i++) {
                excessDemand[i] = model.addVar(- GRB.INFINITY, GRB.INFINITY, 0, GRB.CONTINUOUS, "excessDemand" + i);
                accumulatedDemand[i] = model.addVar(0, GRB.INFINITY, 0, GRB.CONTINUOUS, "accumulatedDemand" + i);
                demand[i] = model.addVar(0, GRB.INFINITY, 0, GRB.CONTINUOUS, "demand" + i);
                thereIsExcessDemand[i] = model.addVar(0, 1, 0, GRB.BINARY, "thereIsExcessDemand" + i);
                
                for (int s = 0; s < numDV; s++) {
                    demandRealised[i][s] = model.addVar(0, 1, 0, GRB.BINARY, "demandRealised" + i + "_" + s);
                }
            }

            // Define objective function
            GRBLinExpr objExpr = new GRBLinExpr();

            for (int i = 0; i < numDP; i++) {
                objExpr.addTerm(1, excessDemand[i]);
            }
            model.setObjective(objExpr, GRB.MAXIMIZE);

            // Add constraints ---------------------------------------------------

            // 1. Bound excess demand of all demand points that are not pick-up locations to 0
            for (int i = 0; i < numDP; i++) {
                GRBLinExpr rhs = new GRBLinExpr();
                rhs.addTerm(10000, thereIsExcessDemand[i]);
                model.addConstr(excessDemand[i], GRB.LESS_EQUAL, rhs, "constraint1_" + i);
            }

            // 2. Excess demand of a demand point is the difference between the capacity of the buses in the current solution and the new accumulated demand
            for (int i = 0; i < numDP; i++) {
                GRBLinExpr lhs = new GRBLinExpr();
                GRBLinExpr rhs = new GRBLinExpr();

                lhs.addTerm(1, excessDemand[i]);
                lhs.addTerm(- 1, accumulatedDemand[i]);

                for (int b = 0; b < numBus; b++) {
                    for (int j = 0; j < numSh; j++) {
                        lhs.addConstant(busCap * tripsValues[b][i][j]);
                    }
                }
                rhs.addConstant((10000));
                rhs.addTerm(- 10000, thereIsExcessDemand[i]);
                
                model.addConstr(lhs, GRB.LESS_EQUAL, rhs, "constraint2_" + i);
            }

            // 3.
            for (int i = 0; i < numDP; i++) {
                    model.addConstr(excessDemand[i], GRB.GREATER_EQUAL, 0, "excessDemandNonNegative_" + i);
            }

            // 4. 
            for (int p = 0; p < numDP; p++) {
                GRBLinExpr rhs = new GRBLinExpr();

                for (int i = 0; i < numDP; i++) {
                    rhs.addTerm(isClosestPickUpValues[p][i], demand[i]);
                }
                model.addConstr(accumulatedDemand[p], GRB.EQUAL, rhs, "accDemandDef_" + p);
            }
            
            // 5.
            for (int i = 0; i < numDP; i++) {
                    GRBLinExpr rhs = new GRBLinExpr();

                    for (int s = 0; s < numDV; s++) {
                        rhs.addTerm(demandVectors.get(s)[i], demandRealised[i][s]);
                    }
                    model.addConstr(demand[i], GRB.LESS_EQUAL, rhs, "demandChoice_" + i);
            }

            // 6.
            for (int i = 0; i < numDP; i++) {
                GRBLinExpr lhs = new GRBLinExpr();

                for (int s = 0; s < numDV; s++) {
                    lhs.addTerm(1, demandRealised[i][s]);
                }
                model.addConstr(lhs, GRB.EQUAL, 1, "demandRealised_" + i);
            }

            // 7.
            GRBLinExpr lhs = new GRBLinExpr();

            for (int i = 0; i < numDP; i++) {
                for (int s = 1; s < numDV; s++) {
                    lhs.addTerm(1, demandRealised[i][s]);
                }
            }
            model.addConstr(lhs, GRB.LESS_EQUAL, pess, "demandRealisedSum");

        } catch (GRBException e) {
            e.printStackTrace();
        }
    }

    /**
     * Solve the WCD model
     * @throws IOException
     * @throws GRBException
     */
    public void solve() throws IOException, GRBException {
        model.optimize();
    }

    /**
     * Dispose of the WCD model
     * @throws GRBException
     */
    public void dispose() throws GRBException {
        model.dispose();
        env.dispose();
    }

    /**
     * Get the objective value of the WCD model
     * @return Objective value
     * @throws GRBException
     */
    public double getObjective() throws GRBException {
        model.optimize();
        return model.get(GRB.DoubleAttr.ObjVal);
    }

    /**
     * Write the WCD model to a file
     * @throws IOException
     * @throws GRBException
     */
    public void write() throws IOException, GRBException {
        model.write("WCD.lp");
    }
}
