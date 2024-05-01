public class DemandPoint {

    private final int demand;
    private final int id;
    private final int x;
    private final int y;

    public DemandPoint(int demand, int id, int x, int y) {
        this.demand = demand;
        this.id = id;
        this.x = x;
        this.y = y;
    }

    public int getDemand() {
        return this.demand;
    }

    public int getID() {
        return this.id;
    }

    public int[] getCoordinates() {
        int[] coordinates = new int[2];
        coordinates[0] = this.x;
        coordinates[1] = this.y;

        return coordinates;
    }

    public double getDistance(DemandPoint other) {
        int otherX = other.getCoordinates()[0];
        int otherY = other.getCoordinates()[1];

        return Math.sqrt((this.x - otherX) * (this.x - otherX) + (this.y - otherY) * (this.y - otherY));
    }
}
