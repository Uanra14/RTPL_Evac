import java.io.IOException;
import java.util.List;
import com.gurobi.gurobi.GRB;
import com.gurobi.gurobi.GRBEnv;
import com.gurobi.gurobi.GRBException;
import com.gurobi.gurobi.GRBLinExpr;
import com.gurobi.gurobi.GRBModel;
import com.gurobi.gurobi.GRBVar;

/**
 * Class containing the ZDWCD model
 * 
 * @author 562606ad
 */
public class ZDWCD {
    public GRBModel model;
    public GRBEnv env;

    public GRBVar[] excessDemand;
    public GRBVar[] accumulatedDemand;
    public GRBVar[] demand;
    public GRBVar[] thereIsExcessDemand;
    public GRBVar[][] isClosestPickUp;
    public GRBVar[][] demandRealised;
    public GRBVar[][] typeDemandRealised;

    public int numDP;
    public int numDV;
    public int numSh;
    public int numTypes;
    
    public int[] numOfEachType;
    public int[][] typeAssignment;
    public double[] isPickUpLocationValues;
    public double[][] isClosestPickUpValues;
    public double[][][] tripsValues;
    
    /**
     * Constructor for the ZDWCD model
     * @param isPickUpLocation Array of binary variables indicating whether a demand point is a pick-up location
     * @param isClosestPickUp Array of binary variables indicating whether a demand point is the closest pick-up location to another demand point
     * @param trips Array of binary variables indicating whether a bus travels from a demand point to a shelter
     * @param busCap Capacity of the buses
     * @param numBus Number of buses
     * @param demandVectors List of the possible vectors of demand for each demand point
     * @param longestWalk Maximum allowed walking time
     * @param maxT Maximum allowed driving time for the buses
     * @param times Matrix containing the driving times from each demand point to each shelter
     * @param walkingTimes Matrix containing the walking times between each pair of nodes
     * @param selectedDemandVectors List of selected demand vectors
     * @param shelterCap Array containing the capacities of the shelters
     * @param types Array containing the types of the demand points
     * @param assignment Matrix containing the assignments of the demand points, 1 if the demand point is of that type, 0 otherwise
     * @param parameter Correlation parameter
     * @throws IOException
     */
    public ZDWCD(GRBVar[] isPickUpLocation, GRBVar[][] isClosestPickUp, GRBVar[][][] trips,
    int busCap, int numBus, List<Integer[]> demandVectors, int longestWalk,
    int maxT, int[][] times, int[][] walkingTimes, List<Integer[]> selectedDemandVectors,
    int[] shelterCap, int[] types, int[] assignment, double parameter) throws IOException {

        this.numDP = isPickUpLocation.length;
        this.numDV = demandVectors.size(); // possible values for a demand point
        this.numSh = shelterCap.length;
        this.numTypes = types.length;

        this.numOfEachType = helper.getNumOfEachType(types, assignment);
        this.typeAssignment = helper.assignTypes(types, assignment);
        
        this.excessDemand = new GRBVar[numDP];
        this.accumulatedDemand = new GRBVar[numDP];
        this.demand = new GRBVar[numDP];
        this.thereIsExcessDemand = new GRBVar[numDP];
        this.demandRealised = new GRBVar[numDP][numDV];
        this.typeDemandRealised = new GRBVar[numTypes][numDV];
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
                }

            for (int s = 0; s < numDV; s++) {
                for (int i = 0; i < numDP; i++) {
                    demandRealised[i][s] = model.addVar(0, 1, 0, GRB.BINARY, "demandRealised" + "_" + i + "_" + s);
                }
                for (int t = 0; t < numTypes; t++) {
                    typeDemandRealised[t][s] = model.addVar(0, 1, 0, GRB.BINARY, "typeDemandRealised" + "_" + t + "_" + s);
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
            for (int t = 0; t < numTypes; t++) {
                GRBLinExpr lhs = new GRBLinExpr();

                for (int s = 1; s < numDV; s++) {
                    lhs.addTerm(1, typeDemandRealised[t][s]);
                }
                model.addConstr(lhs, GRB.EQUAL, 1, "oneTypeDemandRealised_" + t);
            }

            // 6.
            for (int i = 0; i < numDP; i++) {
                GRBLinExpr rhs = new GRBLinExpr();

                for (int s = 0; s < numDV; s++) {
                    rhs.addTerm(demandVectors.get(s)[i], demandRealised[i][s]);
                }
                model.addConstr(demand[i], GRB.LESS_EQUAL, rhs, "demandRealised_" + i);
            }

            // 7.
            GRBLinExpr lhs7 = new GRBLinExpr();

            for (int t = 0; t < numTypes; t++) {
                lhs7.addTerm(1, typeDemandRealised[t][2]);
            }
            model.addConstr(lhs7, GRB.LESS_EQUAL, 1, "OnlyOneHighTypeDemandRealised");

            // 8.
            for (int t = 0; t < numTypes; t++) {
                GRBLinExpr lhs = new GRBLinExpr();
                GRBLinExpr rhs = new GRBLinExpr();

                for (int i = 0; i < numDP; i++) {
                    lhs.addTerm(typeAssignment[i][t], demandRealised[i][1]);
                }
                rhs.addTerm(Math.ceil(numOfEachType[t] * parameter), typeDemandRealised[t][1]);

                model.addConstr(lhs, GRB.GREATER_EQUAL, rhs, "lowDemandRealised" + t);
            }

            // 9.
            for (int t = 0; t < numTypes; t++) {
                GRBLinExpr lhs = new GRBLinExpr();
                GRBLinExpr rhs = new GRBLinExpr();

                for (int i = 0; i < numDP; i++) {
                    lhs.addTerm(typeAssignment[i][t], demandRealised[i][2]);
                }
                rhs.addTerm((int) Math.ceil(numOfEachType[t]), typeDemandRealised[t][2]);

                model.addConstr(lhs, GRB.EQUAL, rhs, "highDemandRealised_" + t);
            }

            // 10.
            for (int i = 0; i < numDP; i++) {
                GRBLinExpr lhs = new GRBLinExpr();

                for (int s = 0; s < numDV; s++) {
                    lhs.addTerm(1, demandRealised[i][s]);
                }
                model.addConstr(lhs, GRB.EQUAL, 1, "demandRealised_" + i);
            }
        } catch (GRBException e) {
            e.printStackTrace();
        }
    }

    /**
     * Solve the ZDWCD model
     * @throws IOException
     * @throws GRBException
     */
    public void solve() throws IOException, GRBException {
        model.optimize();
    }

    /**
     * Dispose of the ZDWCD model
     * @throws GRBException
     */
    public void dispose() throws GRBException {
        model.dispose();
        env.dispose();
    }

    /**
     * Get the objective value of the ZDWCD model
     * @return Objective value
     * @throws GRBException
     */
    public double getObjective() throws GRBException {
        model.optimize();
        return model.get(GRB.DoubleAttr.ObjVal);
    }

    /**
     * Write the ZDWCD model to a file
     * @throws IOException
     * @throws GRBException
     */
    public void write() throws IOException, GRBException {
        model.write("ZDWCD.lp");
    }
}
