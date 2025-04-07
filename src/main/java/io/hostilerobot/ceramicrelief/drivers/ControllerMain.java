package io.hostilerobot.ceramicrelief.drivers;

import io.hostilerobot.ceramicrelief.controller.JsonDataController;
import io.hostilerobot.ceramicrelief.controller.TextControllerDirectory;
import io.hostilerobot.ceramicrelief.controller.TextControllerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class ControllerMain {
    public static void main(String[] args) throws Exception{
//        TextControllerDirectory.builder()
//                .
        new ControllerMain();
    }


    ControllerMain() throws Exception {

        var resource = getClass().getResource("/controller/").getPath();
        // hacky bullshit so we can get the right file
        resource = resource.replace("/target/classes/", "/src/main/resources/");
        TextControllerDirectory.builder()
                .and(TextControllerFactory.fileExtension("json"), f -> {
                    System.out.println("making controller for " + f);
                    var controller = new JsonDataController<>(Map.class);
                    controller.addListener( System.out::println );
                    return controller;
                })
                .onError(Throwable::printStackTrace)
                .build(Paths.get(resource));
        while(true) {
            Thread.sleep(1000);
        }
    }

}
