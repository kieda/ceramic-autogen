module ceramic.autogen {
    requires com.fasterxml.jackson.databind;
    requires commons.math;
    requires directory.watcher;
    requires javafx.fxml;
    requires org.apache.commons.io;
    requires org.jgrapht.core;
    requires org.jheaps;
    requires rtree2;
    requires javafx.swing;

    exports io.hostilerobot.ceramicrelief.drivers;
    exports io.hostilerobot.ceramicrelief.drivers.rtee;
}