import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.gurobi.gurobi.GRBException;

public class Main {

    public static void main(String[] args) throws IOException, GRBException {

        // ** SIOUX FALLS ** -----------------------------------------------------------------------------------------

        Integer[] nominalDemand = {
            60, 42, 40, 46, 38,
            50, 34, 44, 44, 52,
            48, 40, 36, 26, 30 };
        Integer[] lowDemand = {
            35, 39, 31, 30, 25,
            31, 23, 41, 23, 39,
            48, 32, 36, 23, 17 };
        Integer[] highDemand = {
            109, 66, 65, 84, 65,
            84, 57, 69, 69, 93,
            92, 65, 66, 49, 50 };

        int[] shelterCapacities = {240, 333, 360, 300};

        String walkingTimesPath = "C:\\Users\\arnau\\Documents\\2023-2024\\Bsc2 Thesis\\Sioux Falls\\walkingtimeALL.csv"; // Path to the CSV file
        String drivingTimesPath = "C:\\Users\\arnau\\Documents\\2023-2024\\Bsc2 Thesis\\Sioux Falls\\drivingTimeALL.csv"; // Path to the CSV file

        int longestWalk = 60;
        int maxT = 180;
        int busCap = 30;
        int numBus = 10;
        int size = 24;
        
        double[] values = {0.0, 0.05, 0.1, 0.15, 0.2, 0.25, 0.3, 0.35, 0.4, 0.45, 0.5, 0.55, 0.6, 0.65, 0.7, 0.75, 0.8, 0.85, 0.9, 0.95, 1};

        // // ** SRTPL ** ------------------------------------------------------------------------------------------------
        
        // // for (int count = 0; count < 10; count++) {
        // //     for (double parameter : values) {
        // //         ZDRTPL.runZDRTPL(nominalDemand, lowDemand, highDemand, shelterCapacities, walkingTimesPath,
        // //         drivingTimesPath, longestWalk, maxT, busCap, numBus, parameter, size, count);
        // //     }
        // // }

        // // ** RTPL ** -------------------------------------------------------------------------------------------------
        
        // // for (int count = 0; count < 10; count++) {
        // //     RTPL.runFullRTPL(nominalDemand, highDemand, shelterCapacities, walkingTimesPath,
        // //     drivingTimesPath, longestWalk, maxT, busCap, numBus, size, count);
        // // }

        // // ** RTPL SIMULATED DEMANDS ** --------------------------------------------------------------------------------
        
        int[] columnIndices = new int[15];

        for (int i = 0; i < columnIndices.length; i++) {
            columnIndices[i] = i;
        }

        List<int[]> simulatedValues = helper.readColumnsFromCSV("C:\\Users\\arnau\\Documents\\2023-2024\\Bsc2 Thesis\\Sioux Falls\\SimulatedDemands.csv", columnIndices);
        List<Integer[]> simulatedValuesConv = new ArrayList<Integer[]>();

        for (int[] demandValues : simulatedValues) {
            Integer[] simulatedValueConv = new Integer[demandValues.length];

            for (int i = 0; i < demandValues.length; i++) {
                simulatedValueConv[i] = (Integer) demandValues[i];
            }
            simulatedValuesConv.add(simulatedValueConv);
        }

        double[] successRates = new double[nominalDemand.length + 1];

        for (int pess = 0; pess <= nominalDemand.length; pess++) {
            successRates[pess] = RTPL.runRTPLSim(nominalDemand, highDemand, shelterCapacities, walkingTimesPath,
            drivingTimesPath, longestWalk, maxT, busCap, numBus, pess, size, simulatedValuesConv);
        }

        for (int i = 0; i < successRates.length; i++) {
            System.out.println("Success rate for pessimism level " + i + ": " + successRates[i]);
        }

        // ** SRTPL SIMULATED DEMANDS ** --------------------------------------------------------------------------------

        // double[] successRates = new double[values.length + 1];

        // for (int i = 0; i < values.length; i++) {
        //     successRates[i] =  ZDRTPL.runZDRTPLSim(nominalDemand, lowDemand, highDemand, shelterCapacities, walkingTimesPath,
        //     drivingTimesPath, longestWalk, maxT, busCap, numBus, values[i], size, simulatedValuesConv);
        // }

        // for (int i = 0; i < values.length; i++) {
        //     System.out.println("Success rate for correlation level " + values[i] + ": " + successRates[i]);
        // }

        // --**ROTTERDAM**-----------------------------------------------------------------------------------------

        // int[] shelterCapacitiesRD = {500, 500, 700, 700, 700, 1000, 1000}; // 7 shelters (think abt this later)
        // double[] values = {0.0, 0.05, 0.1, 0.15, 0.2, 0.25, 0.3, 0.35, 0.4, 0.45, 0.5, 0.55, 0.6, 0.65, 0.7, 0.75, 0.8, 0.85, 0.9, 0.95, 1};
        // int size = 57;

        // String walkingTimesPathRD = "C:\\Users\\arnau\\Documents\\2023-2024\\Bsc2 Thesis\\Rotterdam\\walkingtimeALL.csv"; // Path to the CSV file
        // String drivingTimesPathRD = "C:\\Users\\arnau\\Documents\\2023-2024\\Bsc2 Thesis\\Rotterdam\\drivingTimeALL.csv"; // Path to the CSV file

        // int maxT = 180;
        // int busCap = 30;
        // int numBus = 30;
        // int longestWalk = 60;

        // helper.writeDemandValuesToExcel(helper.generateDemandValues(size), "C:\\Users\\arnau\\Documents\\2023-2024\\Bsc2 Thesis\\Rotterdam\\Demands.xlsx");

        // int[] columnIndices = new int[50];

        // for (int i = 0; i < columnIndices.length; i++) {
        //     columnIndices[i] = i;
        // }

        // List<int[]> demandValuesList = helper.readColumnsFromCSV("C:\\Users\\arnau\\Documents\\2023-2024\\Bsc2 Thesis\\Rotterdam\\Demands.csv", columnIndices);
        // List<Integer[]> demandValuesListConv = new ArrayList<Integer[]>();

        // for (int[] demandValues : demandValuesList) {
        //     Integer[] demandValuesConv = new Integer[demandValues.length];

        //     for (int i = 0; i < demandValues.length; i++) {
        //         demandValuesConv[i] = (Integer) demandValues[i];
        //     }
        //     demandValuesListConv.add(demandValuesConv);
        // }

        // List<int[]> simulatedValues = helper.readColumnsFromCSV("C:\\Users\\arnau\\Documents\\2023-2024\\Bsc2 Thesis\\Rotterdam\\SimulatedDemands.csv", columnIndices);
        // List<Integer[]> simulatedValuesConv = new ArrayList<Integer[]>();

        // for (int[] demandValues : simulatedValues) {
        //     Integer[] simulatedValueConv = new Integer[demandValues.length];

        //     for (int i = 0; i < demandValues.length; i++) {
        //         simulatedValueConv[i] = (Integer) demandValues[i];
        //     }
        //     simulatedValuesConv.add(simulatedValueConv);
        // }

        // ** SRTPL ** ------------------------------------------------------------------------------------------------

        // for (int count = 0; count < 10; count++) {
        //     for (double parameterValue : values) {
        //         ZDRTPL.runZDRTPL(demandValuesListConv.get(0), demandValuesListConv.get(1), demandValuesListConv.get(2), shelterCapacitiesRD, walkingTimesPathRD,
        //         drivingTimesPathRD, longestWalk, maxT, busCap, numBus, parameterValue, size, count);
        //     }
        // }
        
        // ** RTPL ** -------------------------------------------------------------------------------------------------

        // for (int count = 0; count < 10; count++) {
        //     RTPL.runFullRTPL(demandValuesListConv.get(0), demandValuesListConv.get(2), shelterCapacitiesRD, walkingTimesPathRD,
        //     drivingTimesPathRD, longestWalk, maxT, busCap, numBus, size, count);
        // }

        // ** RTPL SIMULATED DEMANDS ** --------------------------------------------------------------------------------

        // double[] successRates = new double[demandValuesListConv.get(0).length + 1];

        // for (int pess = 0; pess <= demandValuesListConv.get(0).length; pess++) {
        //     successRates[pess] =  RTPL.runRTPLSim(demandValuesListConv.get(0), demandValuesListConv.get(2), shelterCapacitiesRD, walkingTimesPathRD,
        //     drivingTimesPathRD, longestWalk, maxT, busCap, numBus, pess, size, simulatedValuesConv);
        // }

        // for (int i = 0; i < successRates.length; i++) {
        //     System.out.println("Success rate for pessimism level " + i + ": " + successRates[i]);
        // }
       
        // ** SRTPL SIMULATED DEMANDS ** --------------------------------------------------------------------------------

        // double[] successRates = new double[values.length + 1];

        // for (int i = 0; i < values.length; i++) {
        //     successRates[i] =  ZDRTPL.runZDRTPLSim(demandValuesListConv.get(0), demandValuesListConv.get(1), demandValuesListConv.get(2), shelterCapacitiesRD, walkingTimesPathRD,
        //     drivingTimesPathRD, longestWalk, maxT, busCap, numBus, values[i], size, simulatedValuesConv);
        // }

        // for (int i = 0; i < successRates.length; i++) {
        //     System.out.println("Success rate for correlation level " + values[i] + ": " + successRates[i]);
        // }
    }
}
