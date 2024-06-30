import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.io.FileOutputStream;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Helper class containing methods to generate demand values, write them to an Excel file, get the number of each type of demand point,
 * assign types to demand points, get the distance matrix from a CSV file, get the times from the demand points to the shelters in the Sioux Falls network,
 * get the times from the demand points to the shelters in the Rotterdam City network, and read columns from a CSV file
 * 
 * @author 562606ad
 */
public class helper {

    /**
     * Generate random demand values for each demand point
     * @param size Number of demand points
     * @return List of nominal demand values, low demand values, and high demand values
     */
    public static List<Integer[]> generateDemandValues(int size) {
        Integer[] demandValues = new Integer[size];
        Integer[] lowDemandValues = new Integer[size];
        Integer[] highDemandValues = new Integer[size];
        List<Integer[]> demandValuesList = new ArrayList<Integer[]>();

        for (int i = 0; i < size; i++) {
            demandValues[i] = (int) ((int) (Math.random() * (60 - 26 + 1)) + 26);
            lowDemandValues[i] = (int) (demandValues[i] * (Math.random() * (0.5) + 0.5));
            highDemandValues[i] = (int) (demandValues[i] * ((Math.random() * (1.5 - 2 + 1)) + 1.5));
        }
        demandValuesList.add(demandValues);
        demandValuesList.add(lowDemandValues);
        demandValuesList.add(highDemandValues);

        return demandValuesList;
    }

    /**
     * Write demand values to an Excel file in order to store them and use them for all the scenarios
     * @param demandValuesList List of nominal demand values, low demand values, and high demand values
     * @param filePath Path to the Excel file
     * @throws IOException
     */
    public static void writeDemandValuesToExcel(List<Integer[]> demandValuesList, String filePath) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Demand Values");

        int rowNum = 0;
        for (Integer[] demandValues : demandValuesList) {
            Row row = sheet.createRow(rowNum++);
            int colNum = 0;
            for (Integer value : demandValues) {
                Cell cell = row.createCell(colNum++);
                cell.setCellValue(value);
            }
        }
        try (FileOutputStream outputStream = new FileOutputStream(filePath)) {
            workbook.write(outputStream);
        } catch (Exception e) {
            System.out.println("Error writing to Excel file: " + e.getMessage());
        }
        workbook.close();
    }

    /**
     * Get the amount of each type of demand point in a network
     * @param types Array of demand point types
     * @param assignment Array of demand point assignments
     * @return Array of the number of each type of demand point
     */
    public static int[] getNumOfEachType(int[] types, int[] assignment) {
        int[] numTypes = new int[types.length];

        for (int i = 0; i < assignment.length; i++) {
            for (int j = 1; j <= types.length; j++)
            if (assignment[i] == j) {
                numTypes[j - 1] += 1;
            }
        }
        return numTypes;
    }

    /**
     * Assign types to demand points
     * @param types Array of demand point types
     * @param assignment Array of demand point assignments
     * @return Matrix containing 1 if the demand point is of that type, 0 otherwise
     */
    public static int[][] assignTypes(int[] types, int[] assignment) {
        int[][] isType = new int[assignment.length][types.length];

        for (int i = 0; i < assignment.length; i++) {
            for (int j = 0; j < types.length; j++) {
                if (assignment[i] == types[j]) {
                    isType[i][j] = 1;
                }
            }
        }
        return isType;
    }

    /**
     * Get the distance matrix from a CSV file
     * @param filePath Path to the CSV file
     * @param size Number nodes in the network
     * @return A matrix containing the distances between each pair of nodes
     */
    public static int[][] getTimesMatrix(String filePath, int size) {
        int[] columnIndices = {0, 1, 2};
        List<int[]> times = null;

        try {
            times = helper.readColumnsFromCSV(filePath, columnIndices);
        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
        }
        int[][] timesMatrix = new int[size][size];
        int kDT = 0;

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                timesMatrix[i][j] = times.get(kDT)[2] * 2 / 60;
                kDT++;
            }
        }
        return timesMatrix;
    }

    /**
     * Get only the times from the demand points to the shelters in the Sioux Falls network, using the fact that the shelter locations are known
     * @param timesMatrix Matrix containing the times between each pair of nodes
     * @return Matrix containing the times from each demand point to the shelters
     */
    public static int[][] getTimesDPToSheltersSF(int[][] timesMatrix) {
        int[][] timesDPtoShelters = new int[15][4];
        int dp = 0;

        for (int i = 0; i < 24; i++) {
            int sh = 0;
            
            if (i != 13 && i != 14 && i != 18 && i != 22 && i != 23 && i != 12 && i != 19 && i != 20 && i != 21) {
                for (int j = 0; j < 24; j++) {
                    if (j == 21 || j == 19 || j == 20 || j == 12) {
                        timesDPtoShelters[dp][sh] = timesMatrix[i][j];
                        sh++;
                    }
                }
                dp++;
            }
        }
        return timesDPtoShelters;
    }
    /**
     * Get only the times from the demand points to the shelters in the Rotterdam City network, using the fact that the shelter locations are at the first numShelters index of the matrix
     * @param timesMatrix Matrix containing the times between each pair of nodes
     * @param numShelters Number of shelters in the network
     * @return Matrix containing the times from each demand point to the shelters
     */
    public static int[][] getTimesDPToSheltersRD(int[][] timesMatrix, int numShelters) {
        int[][] timesDPtoShelters = new int[timesMatrix.length - numShelters][numShelters];
        int dp = 0;

        for (int i = 0; i < timesMatrix.length; i++) {
            int sh = 0;
            
            if (i > numShelters - 1) {
                for (int j = 0; j < 24; j++) {
                    if (j < numShelters) {
                        timesDPtoShelters[dp][sh] = timesMatrix[i][j];
                        sh++;
                    }
                }
                dp++;
            }
        }
        return timesDPtoShelters;
    }

    /**
     * Read columns from a CSV file
     * @param filePath Path to the CSV file
     * @param columnIndices Indices of the columns to read
     * @return List of the values in the columns
     * @throws IOException
     */
    public static List<int[]> readColumnsFromCSV(String filePath, int[] columnIndices) throws IOException {
        List<int[]> data = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                // Extract the columns based on indices provided
                int[] selectedColumns = new int[columnIndices.length];

                for (int i = 0; i < columnIndices.length; i++) {
                    selectedColumns[i] = Integer.parseInt(values[columnIndices[i]]);
                }
                data.add(selectedColumns);
            }
        }
        return data;
    }

    private static Random rand = new Random(100);

    public static List<Integer[]> generateDemandVectors(List<Integer[]> demandValues) {
        int size = demandValues.get(0).length;
        List<Integer[]> demandVectors = new ArrayList<Integer[]>();
        
        Integer[] demand = demandValues.get(0);
        Integer[] lowDemand = demandValues.get(1);
        Integer[] highDemand = demandValues.get(2);

        for (int i = 0; i < 100000; i++) {
            Integer[] demandVector = new Integer[size];

            for (int j = 0; j < size; j++) {
                double x = rand.nextDouble();
            
                if (x < 1.0/3) {
                    demandVector[j] = (demand[j]);
                } else if (x < 2.0/3) {
                    demandVector[j] = (lowDemand[j]);
                } else {
                    demandVector[j] = (highDemand[j]);
                }
            }
            demandVectors.add(demandVector);
        }
        return demandVectors;
    }
}
