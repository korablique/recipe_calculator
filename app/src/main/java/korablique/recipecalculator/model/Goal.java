package korablique.recipecalculator.model;

import java.util.HashMap;
import java.util.Map;

import korablique.recipecalculator.R;

public enum Goal {
    LOSING_WEIGHT(R.string.losing_weight, 0),
    MAINTAINING_CURRENT_WEIGHT(R.string.maintaining_current_weight, 1),
    MASS_GATHERING(R.string.mass_gathering, 2);

    public static final Map<Integer, Goal> POSITIONS = new HashMap<>();
    private int stringRes;
    private int id;

    Goal(int stringRes, int id) {
        this.stringRes = stringRes;
        this.id = id;
    }

    static {
        Goal[] elements = Goal.values();
        for (int index = 0; index < elements.length; index++) {
            POSITIONS.put(index, elements[index]);
        }
    }

    public int getStringRes() {
        return stringRes;
    }

    public int getId() {
        return id;
    }

    public static Goal fromId(int id) {
        switch (id) {
            case 0:
                return LOSING_WEIGHT;
            case 1:
                return MAINTAINING_CURRENT_WEIGHT;
            case 2:
                return MASS_GATHERING;
            default:
                throw new IllegalStateException("Has no element with this id: " + id);
        }
    }
}
