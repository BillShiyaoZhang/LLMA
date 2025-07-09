package LLMABelief;

public class Belief<T> {
    T obj;
    double value;

    public Belief(T obj, double value) {
        this.obj = obj;
        this.value = value;
    }
}
