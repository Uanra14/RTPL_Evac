import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import com.gurobi.gurobi.GRB;
import com.gurobi.gurobi.GRBEnv;
import com.gurobi.gurobi.GRBException;
import com.gurobi.gurobi.GRBLinExpr;
import com.gurobi.gurobi.GRBModel;
import com.gurobi.gurobi.GRBVar;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Class for the Robust Transportation Planning with Demand Uncertainty (RTPL) model
 * 
 * @author 562606ad
 */
public class RTPL {
    public GRBModel model;
    public GRBEnv env;

    public GRBVar[] isPickUpLocation;
    public GRBVar[][] busAllocation;
    public GRBVar[][][] trips;
    public GRBVar[][] isClosestPickUp;
    public GRBVar[] distanceToPU;
    public GRBVar[][] accDemand;
    
    public int numBus;
    public int busCap;
    public int numDP;
    public int numSh;
    public int pess;
    public int longestWalk;
    public int maxT;
    public int[] shelterCap;
    public int[][] times;
    public int[][] walkingTimes;
    public List<Integer[]> demandVectors;
    public List<Integer[]> selectedDemandVectors;
    
    /**
     * Constructor for the RTPL model
     * @param longestWalk Maximum walking distance
     * @param maxT Maximum driving time for the buses
     * @param times Travel times between demand points and shelters
     * @param pess Degree of pessimism
     * @param walkingTimes Walking times between demand points
     * @param demandVectors List of the possible vectors of demand for each demand point
     * @param selectedDemandVectors List of selected demand vectors
     * @param shelterCap Array of shelter capacities
     * @param numBus Number of buses
     * @param busCap Capacity of each bus
     */
    public RTPL(int longestWalk, int maxT, int[][] times, int pess,
    int[][] walkingTimes, List<Integer[]> demandVectors, List<Integer[]> selectedDemandVectors, 
    int[] shelterCap, int numBus, int busCap) {

        this.numDP = demandVectors.get(0).length;
        this.numSh = shelterCap.length;

        // Decision Variables
        this.isPickUpLocation = new GRBVar[numDP];
        this.busAllocation = new GRBVar[numBus][numDP];
        this.trips = new GRBVar[numBus][numDP][numSh];
        this.isClosestPickUp = new GRBVar[numDP][numDP];
        this.distanceToPU = new GRBVar[numDP];
        this.accDemand = new GRBVar[numDP][selectedDemandVectors.size()];

        // Parameters
        this.longestWalk = longestWalk;
        this.maxT = maxT;
        this.times = times;
        this.pess = pess;
        this.walkingTimes = walkingTimes;
        this.demandVectors = demandVectors;
        this.selectedDemandVectors = selectedDemandVectors;
        this.shelterCap = shelterCap;
        this.numBus = numBus;
        this.busCap = busCap;

        try {
            this.env = new GRBEnv();
            this.model = new GRBModel(this.env);

            // Add variables
            for (int i = 0; i < this.numDP; i++) {
                isPickUpLocation[i] = this.model.addVar(0, 1, 0, GRB.BINARY, "pickUpLoc_" + i);
                distanceToPU[i] = this.model.addVar(0, GRB.INFINITY, 0, GRB.CONTINUOUS, "distanceToPU_" + i);

                for (int d = 0; d < selectedDemandVectors.size(); d++) {
                    accDemand[i][d] = this.model.addVar(0, GRB.INFINITY, 0, GRB.CONTINUOUS, "accDemand_" + i + "_" + d);
                }
                for (int j = 0; j < numDP; j++) {
                    isClosestPickUp[i][j] = this.model.addVar(0, 1, 0, GRB.BINARY, "closestPickUp_" + i + "_" + j);
                }
            }

            for (int b = 0; b < numBus; b++) {
                for (int i = 0; i < numDP; i++) {
                    busAllocation[b][i] = this.model.addVar(0, 1, 0, GRB.BINARY, "busAlloc_" + b + "_" + i);

                    for (int j = 0; j < numSh; j++) {
                        trips[b][i][j] = this.model.addVar(0, GRB.INFINITY, 0, GRB.INTEGER, "numTrips_" + b + "_" + i + "_" + j);
                    }
                }
            }

            // Define objective function
            GRBLinExpr objExpr = new GRBLinExpr();

            for (int b = 0; b < numBus; b++) {
                for (int i = 0; i < numDP; i++) {
                    for (int j = 0; j < numSh; j++) {
                        objExpr.addTerm(times[i][j], trips[b][i][j]);
                    }
                }
            }
            this.model.setObjective(objExpr, GRB.MINIMIZE);

            // Add constraints ---------------------------------------------------

            // 1. Demand Point Satisfaction
            for (int d = 0; d < selectedDemandVectors.size(); d++) {
                for (int i = 0; i < numDP; i++) {
                    GRBLinExpr constraintExpr = new GRBLinExpr();

                    for (int b = 0; b < numBus; b++) {
                        for (int j = 0; j < numSh; j++) {
                            constraintExpr.addTerm(busCap, trips[b][i][j]);
                        }
                    }
                    this.model.addConstr(constraintExpr, GRB.GREATER_EQUAL, accDemand[i][d], "DemandSatisfaction_" + i + "_" + d);
                }
            }

            // 2. Shelter Capacity
            for (int j = 0; j < numSh; j++) {
                GRBLinExpr constraintExpr = new GRBLinExpr();

                for (int b = 0; b < numBus; b++) {
                    for (int i = 0; i < numDP; i++) {
                        constraintExpr.addTerm(busCap, trips[b][i][j]);
                    }
                }
                this.model.addConstr(constraintExpr, GRB.LESS_EQUAL, shelterCap[j], "Capacity_" + j);
            }

            // 3. Bus to only 1 PUP
            for (int b = 0; b < numBus; b++) {
                GRBLinExpr constraintExpr = new GRBLinExpr();

                for (int i = 0; i < numDP; i++) {
                    constraintExpr.addTerm(1, busAllocation[b][i]);
                }
                this.model.addConstr(constraintExpr, GRB.EQUAL, 1, "BusToOnePUP_" + b);
            }

            // 4. Only PUP have bus
            for (int i = 0; i < numDP; i++) {
                GRBLinExpr lhs = new GRBLinExpr();
                GRBLinExpr rhs = new GRBLinExpr();

                rhs.addTerm(numBus, isPickUpLocation[i]);

                for (int b = 0; b < numBus; b++) {
                    lhs.addTerm(1, busAllocation[b][i]);
                }
                this.model.addConstr(lhs, GRB.LESS_EQUAL, rhs, "OnlyPUPHaveBus_" + i);
            }

            // 5. Only if bus is allocated to PUP, then trips are made
            for (int i = 0; i < numDP; i++) {
                for (int b = 0; b < numBus; b++) {
                    GRBLinExpr lhs = new GRBLinExpr();
                    GRBLinExpr rhs = new GRBLinExpr();

                    for (int j = 0; j < numSh; j++) {
                        lhs.addTerm(1, trips[b][i][j]);
                    }
                    rhs.addTerm(GRB.MAXINT, busAllocation[b][i]);

                    this.model.addConstr(lhs, GRB.LESS_EQUAL, rhs, "BusAllocToPUP_" + i + "_" + b);
                }
            }

            // 6. Defines the distance to the closest pick-up location
            for (int i = 0; i < numDP; i++) {
                GRBLinExpr lhs = new GRBLinExpr();
                GRBLinExpr rhs = new GRBLinExpr();

                lhs.addTerm(1, distanceToPU[i]);

                for (int p = 0; p < numDP; p++) {
                    rhs.addTerm(walkingTimes[i][p], isClosestPickUp[p][i]);
                }
                this.model.addConstr(lhs, GRB.EQUAL, rhs, "DistanceToClosestPU_" + i);
            }

            // 7. Forces the distance to the one to closest pick-up location
            for (int i = 0; i < numDP; i++) {
                for (int p = 0; p < numDP; p++) {
                    GRBLinExpr lhs = new GRBLinExpr();
                    GRBLinExpr rhs = new GRBLinExpr();

                    lhs.addTerm(1.0, distanceToPU[i]);
                    rhs.addTerm(walkingTimes[i][p], isPickUpLocation[p]);
                    rhs.addConstant(GRB.MAXINT);
                    rhs.addTerm(-GRB.MAXINT, isPickUpLocation[p]);

                    this.model.addConstr(lhs, GRB.LESS_EQUAL, rhs, "DistanceToPU_" + i + "_" + p);
                }
            }

            // 8. Makes sure every demand point has a pick-up location
            for (int i = 0; i < numDP; i++) {

                GRBLinExpr constraintExpr = new GRBLinExpr();

                for (int p = 0; p < numDP; p++) {
                    constraintExpr.addTerm(1, isClosestPickUp[p][i]);
                }
                this.model.addConstr(constraintExpr, GRB.EQUAL, 1, "HasClosestPU_" + i);
            }

            // 9. Makes sure that only pick-up locations can be the closest pick-up location to a demand point
            for (int i = 0; i < numDP; i++) {
                for (int p = 0; p < numDP; p++) {
                    GRBLinExpr constraintExpr = new GRBLinExpr();
                    constraintExpr.addTerm(1, isClosestPickUp[p][i]);

                    this.model.addConstr(constraintExpr, GRB.LESS_EQUAL, isPickUpLocation[p], "CanBeClosestPU_" + i + "_" + p);
                }
            }

            // 10. Accumulated demand
            for (int d = 0; d < selectedDemandVectors.size(); d++) {
                for (int p = 0; p < numDP; p++) {
                    GRBLinExpr lhs = new GRBLinExpr();
                    GRBLinExpr rhs = new GRBLinExpr();

                    lhs.addTerm(1, accDemand[p][d]);

                    for (int i = 0; i < numDP; i++) {
                        rhs.addTerm(selectedDemandVectors.get(d)[i], isClosestPickUp[p][i]);
                    }
                    this. model.addConstr(lhs, GRB.EQUAL, rhs, "DemandDef_" + p + "_" + d);
                }
            }

            // 11.
            for (int i = 0; i < numDP; i++) {
                GRBLinExpr constrExpr = new GRBLinExpr();
                constrExpr.addTerm(1, distanceToPU[i]);

                this.model.addConstr(constrExpr, GRB.LESS_EQUAL, longestWalk, "MaxWalk_" + i);
            }

            // 12.
            for (int b = 0; b < numBus; b++) {
                GRBLinExpr lhs = new GRBLinExpr();
                for (int i = 0; i < numDP; i++) {
                    for (int j = 0; j < numSh; j ++) {
                        lhs.addTerm(times[i][j], trips[b][i][j]);
                    }
                }
                this.model.addConstr(lhs, GRB.LESS_EQUAL, maxT, "MaxTime_" + b);
            }
            
        } catch (GRBException e) {
            e.printStackTrace();
        }
    }

    /**
     * Solves the RTPL model
     * @throws IOException
     * @throws GRBException
     */
    public void solve() throws IOException, GRBException {
        this.model.optimize();
    }

    /**
     * Writes the RTPL model to an lp file
     */
    public void write() {
        try {
            this.model.write("RTPL_" + pess + ".lp");
        } catch (GRBException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Writes the results of the RTPL model to an Excel file
     * @param duration Duration of the optimization process
     * @param count Number of the run
     * @throws GRBException
     * @throws IOException
     */
    public void writeToExcel(long duration, int count) throws GRBException, IOException {
        // Create a new Excel workbook
        Workbook workbook = new XSSFWorkbook();

        // Create a new sheet for the results
        Sheet sheet = workbook.createSheet("Results_" + pess + "_" + count);

        // Create a header row
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Bus");
        headerRow.createCell(1).setCellValue("Demand Point");
        headerRow.createCell(2).setCellValue("Shelter");
        headerRow.createCell(3).setCellValue("Trips");
        headerRow.createCell(4).setCellValue("Accumulated Demand");
        headerRow.createCell(5).setCellValue("Time (min)");
        headerRow.createCell(6).setCellValue("Duration (ms)");
        
        
        // Write the results to the sheet
        int k = 0;
        int rowIndex = 1;
        int d = selectedDemandVectors.size() - 1;

        for (int b = 0; b < numBus; b++) {
            for (int i = 0; i < numDP; i++) {
                if (busAllocation[b][i].get(GRB.DoubleAttr.X) == 1.0) {
                    if (isPickUpLocation[i].get(GRB.DoubleAttr.X) == 1.0) {
                        for (int j = 0; j < numSh; j++) {
                            if (trips[b][i][j].get(GRB.DoubleAttr.X) > 0.0) {
                                Row dataRow = sheet.createRow(rowIndex);
                                dataRow.createCell(0).setCellValue(b);
                                dataRow.createCell(1).setCellValue(i);
                                dataRow.createCell(2).setCellValue(j);
                                dataRow.createCell(3).setCellValue(trips[b][i][j].get(GRB.DoubleAttr.X));
                                dataRow.createCell(4).setCellValue(accDemand[i][d].get(GRB.DoubleAttr.X));
                                if (k < 1) {
                                    dataRow.createCell(5).setCellValue(model.get(GRB.DoubleAttr.ObjVal));
                                    dataRow.createCell(6).setCellValue(duration);
                                    k++;
                                }
                                rowIndex++;
                            }
                        }
                    }
                }
            }
        }
        // Create a new Excel file
        try (FileOutputStream fileOut = new FileOutputStream("RTPL_" + pess + "_" + count + "_Results.xlsx")) {
            workbook.write(fileOut);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Dispose of the workbook
        workbook.close();
    }

    /**
     * Disposes of the RTPL model
     * @throws GRBException
     */
    public void dispose() throws GRBException {
        model.dispose();
        env.dispose();
    }
    
    /**
     * Prints the results of the RTPL model
     * @throws GRBException
     */
    public void printResults() throws GRBException {
        for (int b = 0; b < numBus; b++) {
            for (int i = 0; i < numDP; i++) {
                if (busAllocation[b][i].get(GRB.DoubleAttr.X) == 1.0) {
                    if (isPickUpLocation[i].get(GRB.DoubleAttr.X) == 1.0) {
                        for (int j = 0; j < numSh; j++) {
                            if (trips[b][i][j].get(GRB.DoubleAttr.X) > 0.0) {
                                System.out.println("Bus " + b + " picks up at demand point " + i + " and drops off at shelter " + j + " " + trips[b][i][j].get(GRB.DoubleAttr.X) + " times.");
                            }
                        }
                    }
                }
            }
        }
        System.out.println("Objective value: " + this.model.get(GRB.DoubleAttr.ObjVal));
    }

    /**
     * Runs the RTPL model with demand uncertainty count times
     * @param nominalDemand Array of nominal demand values
     * @param highDemand Array of high demand values
     * @param shelterCapacities Array of shelter capacities
     * @param walkingTimesPath Path to the walking times file
     * @param drivingTimesPath Path to the driving times file
     * @param longestWalk Maximum walking distance
     * @param maxT Maximum driving time for the buses
     * @param busCap Capacity of each bus
     * @param numBus Number of buses
     * @param size Size of the network
     * @param count Number of runs
     * @throws IOException
     * @throws GRBException
     */
    public static void runFullRTPL(Integer[] nominalDemand, Integer[] highDemand,
    int[] shelterCapacities, String walkingTimesPath, String drivingTimesPath, int longestWalk, int maxT,
    int busCap, int numBus, int size, int count) throws IOException, GRBException {
        
        // Get the walking times
        int[][] walkingTimesMatrix = helper.getTimesMatrix(walkingTimesPath, size);

        // Get the driving times
        int[][] drivingTimesMatrix = helper.getTimesMatrix(drivingTimesPath, size);
        int[][] timesDPtoShelters = helper.getTimesDPToSheltersSF(drivingTimesMatrix);

        // Incorporate demand uncertainty in the model
        List<Integer[]> demandVectors = new ArrayList<Integer[]>();
        demandVectors.add(nominalDemand);
        demandVectors.add(highDemand);

        for (int pess = 0; pess <= 15; pess++) {
            List<Integer[]> selectedDemandVectors = new ArrayList<Integer[]>();
            selectedDemandVectors.add(nominalDemand);
            // Create the model
            RTPL rtpl = new RTPL(longestWalk, maxT, timesDPtoShelters, pess, walkingTimesMatrix,
            demandVectors, selectedDemandVectors, shelterCapacities, numBus, busCap);

            long startTime = System.nanoTime();

            rtpl.solve();
            
            WCD wcd = new WCD(rtpl.isPickUpLocation, rtpl.isClosestPickUp, rtpl.trips,
            busCap, numBus, pess, demandVectors, longestWalk, maxT, timesDPtoShelters, walkingTimesMatrix,
            selectedDemandVectors, shelterCapacities);  

            double excessDemand = wcd.getObjective(); // solve WCD

            while (excessDemand > 0) {
                Integer[] newDemand = new Integer[nominalDemand.length];

                for (int i = 0; i < nominalDemand.length; i++) {
                    newDemand[i] = (int) wcd.demand[i].get(GRB.DoubleAttr.X);
                }
                selectedDemandVectors.add(newDemand);

                rtpl.dispose();
                rtpl = new RTPL(longestWalk, maxT, timesDPtoShelters, pess, walkingTimesMatrix,
                demandVectors, selectedDemandVectors, shelterCapacities, numBus, busCap);

                rtpl.solve();

                wcd.dispose();
                wcd = new WCD(rtpl.isPickUpLocation, rtpl.isClosestPickUp, rtpl.trips,
                busCap, numBus, pess, demandVectors, longestWalk, maxT, timesDPtoShelters, walkingTimesMatrix,
                selectedDemandVectors, shelterCapacities);

                excessDemand = wcd.getObjective();
            }

            long endTime = System.nanoTime();
            long duration = (endTime - startTime) / 1_000_000; // Convert to milliseconds

            rtpl.writeToExcel(duration, count);
            rtpl.dispose();
        }
    }
    
    /**
     * Runs the RTPL model with demand uncertainty 1 time for 1 pessimism value
     * @param nominalDemand Array of nominal demand values
     * @param highDemand Array of high demand values 
     * @param shelterCapacities Array of shelter capacities
     * @param walkingTimesPath Path to the walking times file
     * @param drivingTimesPath Path to the driving times file
     * @param longestWalk Maximum walking distance
     * @param maxT Maximum driving time for the buses
     * @param busCap Capacity of each bus
     * @param numBus Number of buses
     * @param pess Pessimism value
     * @param size Size of the network
     * @throws IOException
     * @throws GRBException
     */
    public static void runRTPL(Integer[] nominalDemand, Integer[] highDemand,
    int[] shelterCapacities, String walkingTimesPath, String drivingTimesPath, int longestWalk, int maxT,
    int busCap, int numBus, int pess, int size) throws IOException, GRBException {
        // Get the walking times
        int[][] walkingTimesMatrix = helper.getTimesMatrix(walkingTimesPath, size);

        // Get the driving times
        int[][] drivingTimesMatrix = helper.getTimesMatrix(drivingTimesPath, size);
        int[][] timesDPtoShelters = helper.getTimesDPToSheltersRD(drivingTimesMatrix, size);

        // Incorporate demand uncertainty in the model
        List<Integer[]> demandVectors = new ArrayList<Integer[]>();
        demandVectors.add(nominalDemand);
        demandVectors.add(highDemand);


        List<Integer[]> selectedDemandVectors = new ArrayList<Integer[]>();
        selectedDemandVectors.add(nominalDemand);

        // Create the model
        RTPL rtpl = new RTPL(longestWalk, maxT, timesDPtoShelters, pess, walkingTimesMatrix,
        demandVectors, selectedDemandVectors, shelterCapacities, numBus, busCap);

        long startTime = System.nanoTime();

        rtpl.solve();
        
        WCD wcd = new WCD(rtpl.isPickUpLocation, rtpl.isClosestPickUp, rtpl.trips,
        busCap, numBus, pess, demandVectors, longestWalk, maxT, timesDPtoShelters, walkingTimesMatrix,
        selectedDemandVectors, shelterCapacities);  

        double excessDemand = wcd.getObjective(); // solve WCD

        while (excessDemand > 0) {
            Integer[] newDemand = new Integer[nominalDemand.length];

            for (int i = 0; i < nominalDemand.length; i++) {
                newDemand[i] = (int) wcd.demand[i].get(GRB.DoubleAttr.X);
            }
            selectedDemandVectors.add(newDemand);

            rtpl.dispose();
            
            rtpl = new RTPL(longestWalk, maxT, timesDPtoShelters, pess, walkingTimesMatrix,
            demandVectors, selectedDemandVectors, shelterCapacities, numBus, busCap);

            rtpl.solve();

            wcd.dispose();
            wcd = new WCD(rtpl.isPickUpLocation, rtpl.isClosestPickUp, rtpl.trips,
            busCap, numBus, pess, demandVectors, longestWalk, maxT, timesDPtoShelters, walkingTimesMatrix,
            selectedDemandVectors, shelterCapacities);

            excessDemand = wcd.getObjective();
        }
        wcd.dispose();
        rtpl.dispose();
        long endTime = System.nanoTime();
        long duration = (endTime - startTime) / 1_000_000; // Convert to milliseconds

        System.out.println("Total time taken: " + duration + " ms");
    }

    /**
     * Runs the RTPL model with demand uncertainty 1 time for 1 pessimism value
     * and multiple demand simulations, and reports the success rate
     * @param nominalDemand Array of nominal demand values
     * @param highDemand Array of high demand values 
     * @param shelterCapacities Array of shelter capacities
     * @param walkingTimesPath Path to the walking times file
     * @param drivingTimesPath Path to the driving times file
     * @param longestWalk Maximum walking distance
     * @param maxT Maximum driving time for the buses
     * @param busCap Capacity of each bus
     * @param numBus Number of buses
     * @param pess Pessimism value
     * @param size Size of the network
     * @param demandSimulations List of dimulated demand values
     * @throws IOException
     * @throws GRBException
     */
    public static double runRTPLSim(Integer[] nominalDemand, Integer[] highDemand,
    int[] shelterCapacities, String walkingTimesPath, String drivingTimesPath, int longestWalk, int maxT,
    int busCap, int numBus, int pess, int size, List<Integer[]> demandSimulations) throws IOException, GRBException {

        // Get the walking times
        int[][] walkingTimesMatrix = helper.getTimesMatrix(walkingTimesPath, size);

        // Get the driving times
        int[][] drivingTimesMatrix = helper.getTimesMatrix(drivingTimesPath, size);
        int[][] timesDPtoShelters = helper.getTimesDPToSheltersRD(drivingTimesMatrix, shelterCapacities.length);
        // int [][] timesDPtoShelters = helper.getTimesDPToSheltersSF(drivingTimesMatrix);

        // Incorporate demand uncertainty in the model
        List<Integer[]> demandVectors = new ArrayList<Integer[]>();
        demandVectors.add(nominalDemand);
        demandVectors.add(highDemand);


        List<Integer[]> selectedDemandVectors = new ArrayList<Integer[]>();
        selectedDemandVectors.add(nominalDemand);

        // Create the model
        RTPL rtpl = new RTPL(longestWalk, maxT, timesDPtoShelters, pess, walkingTimesMatrix,
        demandVectors, selectedDemandVectors, shelterCapacities, numBus, busCap);

        rtpl.solve();

        WCD wcd = new WCD(rtpl.isPickUpLocation, rtpl.isClosestPickUp, rtpl.trips,
        busCap, numBus, pess, demandVectors, longestWalk, maxT, timesDPtoShelters, walkingTimesMatrix,
        selectedDemandVectors, shelterCapacities);  

        double excessDemand = wcd.getObjective(); // solve WCD

        while (excessDemand > 0) {
            Integer[] newDemand = new Integer[nominalDemand.length];

            for (int i = 0; i < nominalDemand.length; i++) {
                newDemand[i] = (int) wcd.demand[i].get(GRB.DoubleAttr.X);
            }
            selectedDemandVectors.add(newDemand);

            rtpl.dispose();
            
            rtpl = new RTPL(longestWalk, maxT, timesDPtoShelters, pess, walkingTimesMatrix,
            demandVectors, selectedDemandVectors, shelterCapacities, numBus, busCap);

            rtpl.solve();

            wcd.dispose();
            wcd = new WCD(rtpl.isPickUpLocation, rtpl.isClosestPickUp, rtpl.trips,
            busCap, numBus, pess, demandVectors, longestWalk, maxT, timesDPtoShelters, walkingTimesMatrix,
            selectedDemandVectors, shelterCapacities);

            excessDemand = wcd.getObjective();
        }

        int[][] isClosestPickUp = new int[rtpl.numDP][rtpl.numDP];
        for (int i = 0; i < rtpl.numDP; i++) {
            for (int p = 0; p < rtpl.numDP; p++) {
                isClosestPickUp[i][p] = (int) rtpl.isClosestPickUp[i][p].get(GRB.DoubleAttr.X);
            }
        }

        int[][][] trips = new int[rtpl.numBus][rtpl.numDP][rtpl.numSh];
        for (int b = 0; b < rtpl.numBus; b++) {
            for (int i = 0; i < rtpl.numDP; i++) {
                for (int j = 0; j < rtpl.numSh; j++) {
                    trips[b][i][j] = (int) rtpl.trips[b][i][j].get(GRB.DoubleAttr.X);
                }
            }
        }

        List<Integer[]> accDemandsSim = new ArrayList<Integer[]>();
        
        for (int k = 0; k < demandSimulations.size(); k++) {
            Integer[] accDemandSim = new Integer[rtpl.numDP];

            for (int i = 0; i < rtpl.numDP; i++) {
                accDemandSim[i] = 0;
            }
            for (int i = 0; i < rtpl.numDP; i++) {
                for (int j = 0; j < rtpl.numDP; j ++) {
                    if (isClosestPickUp[i][j] == 1) {
                        accDemandSim[i] += demandSimulations.get(k)[j];
                    }    
                }
            }
            accDemandsSim.add(accDemandSim);
        }

        int failures = 0;

        for (int k = 0; k < demandSimulations.size(); k++) {
            int accSimDemand = 0;
            boolean fail = false;

            for (int i = 0; i < rtpl.numDP; i++) {
                accSimDemand = accDemandsSim.get(k)[i];
                int actualCapacity = 0;

                for (int j = 0; j < rtpl.numSh; j++) {
                    for (int b = 0; b < rtpl.numBus; b++) {
                        actualCapacity += rtpl.busCap * trips[b][i][j];
                    }
                }
                if (accSimDemand > actualCapacity) {
                    fail = true;
                }
            }
            if (fail) {
                failures++;
            }
        }
        double successRate = (1 - (failures / (double) demandSimulations.size())) * 100;

        wcd.dispose();
        rtpl.dispose();

        return successRate; 
    }
}
    