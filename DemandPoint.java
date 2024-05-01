public class DemandPoint {

    private final int demand;
    private final int id;
    private final int[] distances;

    public DemandPoint(int demand, int id, int[] distances) {
        this.demand = demand;
        this.id = id;
        this.distances = distances;
    }

    public int getDemand() {
        return this.demand;
    }

    public int getID() {
        return this.id;
    }

    public int getDistance(DemandPoint other) {
        return this.distances[other.getID()];
    }
}