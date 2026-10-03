package enums;

/*
outlines the major muscle groups of the body.
 */

public enum MuscleGroup {
    NULL("All"),
    CHEST("Chest"),
    TRICEPS("Triceps"),
    BICEPS("Biceps"),
    CALVES("Calves"),
    QUADS("Quads"),
    GLUTES("Glutes"),
    HAMSTRINGS("Hamstrings"),
    SHOULDERS("Shoulders"),
    ADDUCTORS("Adductors"),
    CORE("Core"),
    HIP_FLEXORS("Hip Flexors");

    private final String muscleGroupName;

    MuscleGroup(String muscleGroupName) {
        this.muscleGroupName = muscleGroupName;
    }

    @Override
    public String toString() {
        return muscleGroupName;
    }

}
