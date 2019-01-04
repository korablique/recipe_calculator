package korablique.recipecalculator.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import korablique.recipecalculator.R;

public enum Goal {
    LOSING_WEIGHT(R.string.losing_weight, 0),
    MAINTAINING_CURRENT_WEIGHT(R.string.maintaining_current_weight, 1),
    MASS_GATHERING(R.string.mass_gathering, 2);

    public static final Map<Integer, Goal> POSITIONS;
    public static final Map<Goal, Integer> POSITIONS_REVERSED;
    private int stringRes;
    private int id;

    static {
        Goal[] elements = Goal.values();
        Map<Integer, Goal> positions = new HashMap<>();
        Map<Goal, Integer> positionsReversed = new HashMap<>();
        for (int index = 0; index < elements.length; index++) {
            positions.put(index, elements[index]);
            positionsReversed.put(elements[index], index);
        }
        POSITIONS = Collections.unmodifiableMap(positions);
        POSITIONS_REVERSED = Collections.unmodifiableMap(positionsReversed);
    }

    Goal(int stringRes, int id) {
        this.stringRes = stringRes;
        this.id = id;
    }

    public int getStringRes() {
        return stringRes;
    }

    public int getId() {
        return id;
    }

    public static Goal fromId(int id) {
        if (id == LOSING_WEIGHT.id) {
            return LOSING_WEIGHT;
        } else if (id == MAINTAINING_CURRENT_WEIGHT.id) {
            return MAINTAINING_CURRENT_WEIGHT;
        } else if (id == MASS_GATHERING.id) {
            return MASS_GATHERING;
        } else {
            throw new IllegalArgumentException("Has no element with this id: " + id);
        }
    }
}
