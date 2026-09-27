module com.example.workoutmanager {
    requires javafx.controls;
    requires javafx.fxml;

    requires com.fasterxml.jackson.databind;
    requires java.desktop;

    opens com.workoutmanager to javafx.fxml;
    opens model to com.fasterxml.jackson.databind;
    opens enums to com.fasterxml.jackson.databind;

    exports com.workoutmanager;
    exports model;
    exports enums;
}