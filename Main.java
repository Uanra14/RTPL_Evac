import java.io.IOException;
import java.util.List;

public class Main {

    public static void main(String[] args) {

        String coordinateFile = "C:\\Users\\arnau\\Documents\\2023-2024\\Bsc2 Thesis\\Sioux Falls\\SiouxFalls_node.csv"; // Path to the CSV file
        int[] columnIndices = {1, 2};

        List<int[]> coordinates = null;
        int[][] coordinateData = null; 

        try {
            coordinates = helper.readColumnsFromCSV(coordinateFile, columnIndices);
            coordinateData = coordinates.toArray(new int[0][]);

        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
        }
        for (int[] row : coordinateData) {
            System.out.println("[" + row[0] + ", " + row[1] + "]");
        }

        RTPL.solveRTPL(coordinateData);
    }
}