package enums;

/*
represents different sections of a workout.
 */
public enum SectionType {
    WARM_UP("Warm Up"),
    MAIN_WORKOUT("Main Workout"),
    COOL_DOWN("Cool Down");

    private final String sectionType;

    SectionType(String text) {
        this.sectionType = text;
    }

    @Override
    public String toString() {
        return this.sectionType;
    }
}
