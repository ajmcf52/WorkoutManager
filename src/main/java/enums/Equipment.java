package enums;

/*
different types of equipment that can be used to complete an exercise.
used for searching/filtering.
 */

public enum Equipment {
    NULL("All"),
    BARBELL("Barbell"),
    KETTLEBELL("Kettlebell"),
    BODYWEIGHT("Bodyweight"),
    CABLE("Cable"),
    MACHINE("Machine"),
    DUMBBELL("Dumbbell"),
    BOX("Box"),
    TRX("TRX");

    private final String display;

    Equipment(String display) {
        this.display = display;
    }

    @Override
    public String toString() {
        return display;
    }
}
