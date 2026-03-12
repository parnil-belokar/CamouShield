module org.example.cams {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires java.sql;
    requires jbcrypt;
    requires java.net.http;
    requires com.fasterxml.jackson.databind;
    requires javafx.graphics;
    requires com.github.kwhat.jnativehook;
    requires deeplearning4j.nn;
    requires nd4j.api;
    requires com.google.auth.oauth2;
    requires firebase.admin;
    requires google.cloud.firestore;

    opens org.example.cams to javafx.fxml;
    exports org.example.cams;
}