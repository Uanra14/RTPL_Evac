public class Node {

    public int id;
    public int x;
    public int y;

    public Node(int id, int x, int y) {
        this.id = id;
        this.x = x;
        this.y = y;
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

    public double getDistance(Node other) {
        int otherX = other.getCoordinates()[0];
        int otherY = other.getCoordinates()[1];

        return Math.sqrt((this.x - otherX) * (this.x - otherX) + (this.y - otherY) * (this.y - otherY));
    }
}
