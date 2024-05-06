import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class helper {

    public static int[][] getCoordinateMatrix(String filePath) {
        return null;
    }

    public static int[][] getLinksMatrix(String filePath) {
        return null;
    }

    public static Node getNode(int id, ArrayList<Node> demandPoints) {
        for (Node dp : demandPoints) {
            if (id == dp.getID()) {
                return dp;
            }
        }
        return null;
    }

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
}
