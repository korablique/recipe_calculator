package korablique.recipecalculator.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import korablique.recipecalculator.R;

public enum Formula {
    HARRIS_BENEDICT(R.string.harris_benedict, 0),
    MIFFLIN_JEOR(R.string.mifflin_jeor, 1);

    public static final Map<Integer, Formula> POSITIONS;
    public static final Map<Formula, Integer> POSITIONS_REVERSED;
    private int stringRes;
    private int id;

    static {
        Formula[] elements = Formula.values();
        Map<Integer, Formula> positions = new HashMap<>();
        Map<Formula, Integer> positionsReversed = new HashMap<>();
        for (int index = 0; index < elements.length; index++) {
            positions.put(index, elements[index]);
            positionsReversed.put(elements[index], index);
        }
        POSITIONS = Collections.unmodifiableMap(positions);
        POSITIONS_REVERSED = Collections.unmodifiableMap(positionsReversed);
    }

    Formula(int stringRes, int id) {
        this.stringRes = stringRes;
        this.id = id;
    }

    public int getStringRes() {
        return stringRes;
    }

    public int getId() {
        return id;
    }

    public static Formula fromId(int id) {
        if (id == HARRIS_BENEDICT.id) {
            return HARRIS_BENEDICT;
        } else if (id == MIFFLIN_JEOR.id) {
            return MIFFLIN_JEOR;
        } else {
            throw new IllegalArgumentException("Has no element with this id: " + id);
        }
    }
}
