package korablique.recipecalculator.model;

import java.util.HashMap;
import java.util.Map;

import korablique.recipecalculator.R;

public enum Gender {
    MALE(R.string.male, 1),
    FEMALE(R.string.female, 2);

    public static final Map<Integer, Gender> POSITIONS = new HashMap<>();
    private final int stringRes;
    private int id;

    Gender(int stringRes, int id) {
        this.stringRes = stringRes;
        this.id = id;

    }

    static {
        Gender[] elements = Gender.values();
        for (int index = 0; index < elements.length; index++) {
            POSITIONS.put(index + 1, elements[index]);
        }
    }

    public int getStringRes() {
        return stringRes;
    }

    public int getId() {
        return id;
    }

    public static Gender fromId(int id) {
        switch (id) {
            case 1:
                return MALE;
            case 2:
                return FEMALE;
            default:
                throw new IllegalStateException("Has no element with this id: " + id);
        }
    }
}
