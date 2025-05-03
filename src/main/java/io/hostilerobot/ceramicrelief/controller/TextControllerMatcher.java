package io.hostilerobot.ceramicrelief.controller;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.function.Function;

/**
 * a mechanism to generate a text controller.
 * We might want to have multiple different files to match each with their own way of handling data, like Json vs XML.
 *
 * We might also want to have multiple listeners that listen to the same file.
 */
public interface TextControllerMatcher extends FilenameFilter{
    TextController newController(InputStream in) throws IOException;

    public static TextControllerMatcher of(FilenameFilter matcher, TextControllerFactory newController) {
        return new DelegateControllerMatcher(matcher, newController);
    }

    public static FilenameFilter fileExtension(String fileExtension) {
        return (f, name) -> FilenameUtils.isExtension(name, fileExtension);
    }
    public static FilenameFilter nameAndExtension(String fileName, String fileExtension) {
        return (f, name) -> Objects.equals(fileName, FilenameUtils.getBaseName(name))
                && FilenameUtils.isExtension(name, fileExtension);
    }
}

class DelegateControllerMatcher implements TextControllerMatcher {
    private final FilenameFilter matcher;
    private final TextControllerFactory newController;
    DelegateControllerMatcher(FilenameFilter matcher, TextControllerFactory newController) {
        this.matcher = matcher;
        this.newController = newController;
    }

    @Override
    public TextController newController(InputStream in) throws IOException {
        return newController.apply(in);
    }

    @Override
    public boolean accept(File dir, String name) {
        return matcher.accept(dir, name);
    }
}
