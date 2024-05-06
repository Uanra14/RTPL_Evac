public class DemandPoint extends Node {
    private final int demand;

    public DemandPoint(int demand, int id, int x, int y) {
        super(id, x, y);
        this.demand = demand;
    }

    public int getDemand() {
        return this.demand;
    }
}
