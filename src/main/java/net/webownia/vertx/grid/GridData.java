package net.webownia.vertx.grid;

/**
 * Created by abarczewski on 2015-03-30.
 */
public class GridData {
    protected int number;
    protected int countConquer;

    public GridData(int number) {
        this.number = number;
    }

    protected void increment() {
        countConquer += 1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GridData gridData = (GridData) o;

        if (number != gridData.number) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = number;
        return result;
    }
}
