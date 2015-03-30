package net.webownia.vertx.grid;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by abarczewski on 2015-03-30.
 */
public class UserData {
    private String userId;
    protected int countAllClick;
    protected Map<Integer, GridData> gridDataMap = new HashMap<>(0);

    public UserData(String userId, int sizeGrid) {
        this.userId = userId;
        for (int i = 1; i <= sizeGrid; i++) {
            gridDataMap.put(i, new GridData(i));
        }
    }

    protected void increment() {
        countAllClick += 1;
    }
}
