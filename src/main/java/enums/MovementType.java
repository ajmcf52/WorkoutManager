package enums;

// describes the type of movement/exercise.
public enum MovementType {
    STRENGTH("Strength"),
    CARDIO("Cardio"),
    DYNAMIC_FLOW("Dynamic Flow"),
    STATIC_STRETCH("Static stretch"),
    MYOFASCIAL_RELEASE("Myofascial Release");

    private final String type;

    MovementType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return type;
    }

}
