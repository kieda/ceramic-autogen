package io.hostilerobot.ceramicrelief.controller;

import java.io.IOException;
import java.io.InputStream;

@FunctionalInterface
public interface TextControllerFactory {
    public TextController apply(InputStream inputStream) throws IOException;
}
