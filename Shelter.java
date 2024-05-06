public class Shelter extends Node {
    public int capacity;

    public Shelter(int capacity, int id, int x, int y) {
        super(id, x, y);
        this.capacity = capacity;
    }

    public int getCapacity() {
        return this.capacity;
    }
}
