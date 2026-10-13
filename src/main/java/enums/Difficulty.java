package enums;
/*
declares the difficulty of an exercise out of five stars.
half units are included.
 */








public enum Difficulty {
    NULL("All"),
    BEGINNER("Beginner"),
    NOVICE("Novice"),
    INTERMEDIATE("Intermediate"),
    ADVANCED("Advanced"),
    EXPERT("Expert");


    private final String display;

    Difficulty(String display) {
        this.display = display;
    }

    @Override
    public String toString() {
        return display;
    }

}
