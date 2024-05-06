public class Arc {

    private final Node start;
    private final Node end;
    private final int id;

    public Arc(int id, Node start, Node end) {
        this.id = id;
        this.start = start;
        this.end = end;
    }

    public int getID() {
        return this.id;
    }
    
    public double getLength() {
        return this.start.getDistance(this.end);
    }

    public int getStart() {
        return this.start.getID();
    }

    public int getEnd() {
        return this.end.getID();
    }
}
