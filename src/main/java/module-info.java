module com.example.chessgame.chessgamegui {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.chessgame.chessgamegui to javafx.fxml;
    exports com.example.chessgame.chessgamegui;
}