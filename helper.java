public class helper {

    public int[][] getCoordinateMatrix(String filePath) {
        return null;
    }

    public int[][] getLinksMatrix(String filePath) {
        return null;
    }

    public DemandPoint getDemandPoint(int id, DemandPoint[] demandPoints) {
        for (DemandPoint dp : demandPoints) {
            if (id == dp.getID()) {
                return dp;
            }
        }
        return null;
    }
}
