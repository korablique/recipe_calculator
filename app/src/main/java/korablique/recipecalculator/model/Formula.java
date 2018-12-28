package korablique.recipecalculator.model;

import java.util.HashMap;
import java.util.Map;

import korablique.recipecalculator.R;

public enum Formula {
    HARRIS_BENEDICT(R.string.harris_benedict, 0),
    MIFFLIN_JEOR(R.string.mifflin_jeor, 1);

    public static final Map<Integer, Formula> POSITIONS = new HashMap<>();
    private int stringRes;
    private int id;

    Formula(int stringRes, int id) {
        this.stringRes = stringRes;
        this.id = id;
    }

    static {
        Formula[] elements = Formula.values();
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

    public static Formula fromId(int id) {
        switch (id) {
            case 0:
                return HARRIS_BENEDICT;
            case 1:
                return MIFFLIN_JEOR;
            default:
                throw new IllegalStateException("Has no element with this id: " + id);
        }
    }
}
