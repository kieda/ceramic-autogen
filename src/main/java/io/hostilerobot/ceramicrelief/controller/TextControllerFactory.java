package io.hostilerobot.ceramicrelief.controller;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.function.Function;

/**
 * a mechanism to generate a text controller.
 * We might want to have multiple different files to match each with their own way of handling data, like Json vs XML.
 *
 * We might also want to have multiple listeners that listen to the same file.
 */
public interface TextControllerFactory extends FilenameFilter{
    TextController newController(File file) throws IOException;

    public static TextControllerFactory of(FilenameFilter matcher, Function<File, TextController> newController) {
        return new DelegateControllerFactory(matcher, newController);
    }

    public static FilenameFilter fileExtension(String fileExtension) {
        return (f, name) -> FilenameUtils.isExtension(name, fileExtension);
    }
}

class DelegateControllerFactory implements TextControllerFactory{
    private final FilenameFilter matcher;
    private final Function<File, TextController> newController;
    DelegateControllerFactory(FilenameFilter matcher, Function<File, TextController> newController) {
        this.matcher = matcher;
        this.newController = newController;
    }

    @Override
    public TextController newController(File file) throws IOException {
        return newController.apply(file);
    }

    @Override
    public boolean accept(File dir, String name) {
        return matcher.accept(dir, name);
    }
}
