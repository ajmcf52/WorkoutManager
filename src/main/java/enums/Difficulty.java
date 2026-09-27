package enums;
/*
declares the difficulty of an exercise out of five stars.
half units are included.
 */








public enum Difficulty {
    NULL("All"),
    ONE("★☆☆☆☆"),
    ONE_AND_HALF("★½☆☆☆"),
    TWO("★★☆☆☆"),
    TWO_AND_HALF("★★½☆☆"),
    THREE("★★★☆☆"),
    THREE_AND_HALF("★★★½☆"),
    FOUR("★★★★☆"),
    FOUR_AND_HALF("★★★★½"),
    FIVE("★★★★★");


    private final String display;

    Difficulty(String display) {
        this.display = display;
    }

    @Override
    public String toString() {
        return display;
    }

}
