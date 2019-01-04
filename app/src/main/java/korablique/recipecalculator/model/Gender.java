package korablique.recipecalculator.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import korablique.recipecalculator.R;

public enum Gender {
    MALE(R.string.male, 0),
    FEMALE(R.string.female, 1);

    public static final Map<Integer, Gender> POSITIONS;
    public static final Map<Gender, Integer> POSITIONS_REVERSED;

    private final int stringRes;
    private int id;

    static {
        Gender[] elements = Gender.values();
        Map<Integer, Gender> positions = new HashMap<>();
        Map<Gender, Integer> positionsReversed = new HashMap<>();
        for (int index = 0; index < elements.length; index++) {
            positions.put(index, elements[index]);
            positionsReversed.put(elements[index], index);
        }
        POSITIONS = Collections.unmodifiableMap(positions);
        POSITIONS_REVERSED = Collections.unmodifiableMap(positionsReversed);
    }

    Gender(int stringRes, int id) {
        this.stringRes = stringRes;
        this.id = id;
    }

    public int getStringRes() {
        return stringRes;
    }

    public int getId() {
        return id;
    }

    public static Gender fromId(int id) {
        if (id == MALE.id) {
            return MALE;
        } else if (id == FEMALE.id) {
            return FEMALE;
        } else {
            throw new IllegalArgumentException("Has no element with this id: " + id);
        }
    }
}
