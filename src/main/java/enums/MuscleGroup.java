package enums;

/*
outlines the major muscle groups of the body.
 */

public enum MuscleGroup {
    CHEST("CHEST"),
    TRICEPS("TRICEPS"),
    BICEPS("BICEPS"),
    CALVES("CALVES"),
    QUADS("QUADS"),
    GLUTES("GLUTES"),
    HAMSTRINGS("HAMSTRINGS"),
    SHOULDERS("SHOULDERS"),
    ADDUCTORS("ADDUCTORS"),
    HIP_FLEXORS("HIP_FLEXORS");

    private final String muscleGroupName;

    MuscleGroup(String muscleGroupName) {
        this.muscleGroupName = muscleGroupName;
    }

    @Override
    public String toString() {
        return muscleGroupName;
    }

}
