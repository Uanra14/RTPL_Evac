import java.util.ArrayList;

public class Network {

    public Network(int[][] coordinates, int[] value, int[][] links) {

        ArrayList<Node> nodes = new ArrayList<>();
        ArrayList<DemandPoint> demandPoints = new ArrayList<>();
        ArrayList<Shelter> shelters = new ArrayList<>();

        // Create the nodes
        for (int i = 0; i < coordinates.length; i++) {
            if (i != 13 || i != 20 || i != 21 || i != 23) {
                DemandPoint dp = new DemandPoint(value[i], i, coordinates[i][0], coordinates[i][1]);
                demandPoints.add(dp);
                nodes.add(dp);
            } else {
                Shelter shelter = new Shelter(value[i], i, coordinates[i][0], coordinates[i][1]);
                shelters.add(shelter);
                nodes.add(shelter);
            }
        }
        // Create the arcs
        for (int i = 0; i < links.length; i++) {
            new Arc(i, helper.getNode(links[i][0], nodes), helper.getNode(links[i][1], nodes));
        }

      
    }
}
