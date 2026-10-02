package enums;

/*
intensity of an exercise.
 */
public enum Intensity {
    LIGHT("L"),
    MODERATE("M"),
    MODERATE_HEAVY("MH"),
    HEAVY("H");

    private final String intensity;

    Intensity(String value) {
        this.intensity = value;
    }

    @Override
    public String toString() {
        return this.intensity;
    }
}
