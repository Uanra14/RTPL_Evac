public class Network {

    public Network(int[][] coordinates, int[] demand, DemandPoint[][] links) {

        for (int i = 0; i < links.length; i++) {
            new Arc(i, links[i][0], links[i][1]);
        }

        for (int i = 0; i < coordinates.length; i++) {
            new DemandPoint( demand[i], i, coordinates[i][0], coordinates[i][1]);
        }
    }
}