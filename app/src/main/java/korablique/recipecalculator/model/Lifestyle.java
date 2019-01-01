package korablique.recipecalculator.model;

import java.util.HashMap;
import java.util.Map;

import korablique.recipecalculator.R;

public enum Lifestyle {
    PASSIVE_LIFESTYLE(R.string.passive_lifestyle, 0, PhysicalActivityCoefficients.PASSIVE_LIFESTYLE),
    INSIGNIFICANT_ACTIVITY(R.string.insignificant_activity, 1, PhysicalActivityCoefficients.INSIGNIFICANT_ACTIVITY),
    MEDIUM_ACTIVITY(R.string.medium_activity, 2, PhysicalActivityCoefficients.MEDIUM_ACTIVITY),
    ACTIVE_LIFESTYLE(R.string.active_lifestyle, 3, PhysicalActivityCoefficients.ACTIVE_LIFESTYLE),
    PROFESSIONAL_SPORTS(R.string.professional_sports, 4, PhysicalActivityCoefficients.PROFESSIONAL_SPORTS);

    public static final Map<Integer, Lifestyle> POSITIONS = new HashMap<>();
    public static final Map<Lifestyle, Integer> POSITIONS_REVERSED = new HashMap<>();
    private final int stringRes;
    private final int id;
    private final float physActivityCoefficient;

    Lifestyle(int stringRes, int id, float physActivityCoefficient) {
        this.stringRes = stringRes;
        this.id = id;
        this.physActivityCoefficient = physActivityCoefficient;
    }

    public int getStringRes() {
        return stringRes;
    }

    public int getId() {
        return id;
    }

    public float getPhysActivityCoefficient() {
        return physActivityCoefficient;
    }

    static {
        Lifestyle[] elements = Lifestyle.values();
        for (int index = 0; index < elements.length; index++) {
            POSITIONS.put(index, elements[index]);
            POSITIONS_REVERSED.put(elements[index], index);
        }
    }

    public static Lifestyle fromId(int id) {
        switch (id) {
            case 0:
                return PASSIVE_LIFESTYLE;
            case 1:
                return INSIGNIFICANT_ACTIVITY;
            case 2:
                return MEDIUM_ACTIVITY;
            case 3:
                return ACTIVE_LIFESTYLE;
            case 4:
                return PROFESSIONAL_SPORTS;
            default:
                throw new IllegalStateException("Has no element with this id: " + id);
        }
    }
}
