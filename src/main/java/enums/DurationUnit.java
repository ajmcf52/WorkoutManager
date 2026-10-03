package enums;

public enum DurationUnit {
    MIN("min"),
    SEC("sec");

    private final String durationUnit;

    DurationUnit(String durationUnit) {
        this.durationUnit = durationUnit;
    }

    @Override
    public String toString() {
        return durationUnit;
    }
}
