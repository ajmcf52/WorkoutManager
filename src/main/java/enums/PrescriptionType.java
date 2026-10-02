package enums;

/*
differentiates different types of exercises/movements.
 */
public enum PrescriptionType {
    DURATION("Duration"),
    REPS("Reps");

    private final String type;

    PrescriptionType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return this.type;
    }
}
