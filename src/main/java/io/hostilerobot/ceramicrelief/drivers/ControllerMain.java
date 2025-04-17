package io.hostilerobot.ceramicrelief.drivers;

import io.hostilerobot.ceramicrelief.controller.DataController;
import io.hostilerobot.ceramicrelief.controller.JsonDataProcessor;
import io.hostilerobot.ceramicrelief.controller.TextControllerDirectory;
import io.hostilerobot.ceramicrelief.controller.TextControllerMatcher;

import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class ControllerMain {
    public static void main(String[] args) throws Exception{
//        TextControllerDirectory.builder()
//                .
        new ControllerMain();
    }

    private AtomicBoolean shutdown = new AtomicBoolean(false);
    ControllerMain() throws Exception {

        var resource = getClass().getResource("/controller/").getPath();
        // hacky bullshit so we can get the right file
        resource = resource.replace("/target/classes/", "/src/main/resources/");
        TextControllerDirectory.builder()
                .and(TextControllerMatcher.fileExtension("json"), f ->
                        DataController.builder(JsonDataProcessor.builder(Map.class).build())
                        .addListener( System.out::println )
                        .disableMerging()
                        .build(f))
                .onError(Throwable::printStackTrace)
                .onComplete(() -> shutdown.set(true))
                .build(Paths.get(resource));
        while(!shutdown.get()) {
            Thread.sleep(1000);
        }
    }

}
