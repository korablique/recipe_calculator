package korablique.recipecalculator.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import korablique.recipecalculator.R;

public enum Lifestyle {
    PASSIVE_LIFESTYLE(R.string.passive_lifestyle, 0, PhysicalActivityCoefficients.PASSIVE_LIFESTYLE),
    INSIGNIFICANT_ACTIVITY(R.string.insignificant_activity, 1, PhysicalActivityCoefficients.INSIGNIFICANT_ACTIVITY),
    MEDIUM_ACTIVITY(R.string.medium_activity, 2, PhysicalActivityCoefficients.MEDIUM_ACTIVITY),
    ACTIVE_LIFESTYLE(R.string.active_lifestyle, 3, PhysicalActivityCoefficients.ACTIVE_LIFESTYLE),
    PROFESSIONAL_SPORTS(R.string.professional_sports, 4, PhysicalActivityCoefficients.PROFESSIONAL_SPORTS);

    public static final Map<Integer, Lifestyle> POSITIONS;
    public static final Map<Lifestyle, Integer> POSITIONS_REVERSED;
    private final int stringRes;
    private final int id;
    private final float physActivityCoefficient;

    static {
        Lifestyle[] elements = Lifestyle.values();
        Map<Integer, Lifestyle> positions = new HashMap<>();
        Map<Lifestyle, Integer> positionsReversed = new HashMap<>();
        for (int index = 0; index < elements.length; index++) {
            positions.put(index, elements[index]);
            positionsReversed.put(elements[index], index);
        }
        POSITIONS = Collections.unmodifiableMap(positions);
        POSITIONS_REVERSED = Collections.unmodifiableMap(positionsReversed);
    }

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

    public static Lifestyle fromId(int id) {
        if (id == PASSIVE_LIFESTYLE.id) {
            return PASSIVE_LIFESTYLE;
        } else if (id == INSIGNIFICANT_ACTIVITY.id) {
            return INSIGNIFICANT_ACTIVITY;
        } else if (id == MEDIUM_ACTIVITY.id) {
            return MEDIUM_ACTIVITY;
        } else if (id == ACTIVE_LIFESTYLE.id) {
            return ACTIVE_LIFESTYLE;
        } else if (id == PROFESSIONAL_SPORTS.id) {
            return PROFESSIONAL_SPORTS;
        } else {
            throw new IllegalArgumentException("Has no element with this id: " + id);
        }
    }
}
