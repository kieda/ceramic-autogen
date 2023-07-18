package io.hostilerobot.ceramicrelief.controller;

import io.methvin.watcher.DirectoryChangeEvent;
import io.methvin.watcher.DirectoryChangeListener;
import io.methvin.watcher.DirectoryWatcher;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchService;
import java.util.Map;

/**
 * format (mesh)
 *
 * decimal := double
 * topFrac := int
 * botFrac := int
 * frac := [topFrac]/[botFrac]
 *       | [decimal]
 *
 * policy := NONE | MIRROR | ADJACENT
 * vertex := \([frac], [frac], [frac]\)
 * triangle := <[name], [name], [name]>
 *
 * ======
 * file format
 * ======
 *
 * vertices:
 * ([name] = [vertex])*
 * edges:
 * \([name], [name]\) = [policy]
 * faces:
 * [name] = \([name], [name], [name]\)
 * [otherinfo]
 */
public class TextControllerDirectory {

    private final Path directoryToWatch;
    private final DirectoryWatcher watcher;

    private final ControllerChangeListener changeListener;

    private static class ControllerChangeListener implements DirectoryChangeListener{
//        private Map<Path, Object>


        @Override
        public void onEvent(DirectoryChangeEvent directoryChangeEvent) throws IOException {
            switch(directoryChangeEvent.eventType()) {
                case CREATE:

                    break;
                case DELETE:

                    break;
                case MODIFY:
                    break;
            }
        }
    }

    /*
     * just use simple key-value pairs separated by whitespace
     * "section" is defined by a name along with a colon ':'
     * to define multiple values in a key value pair (parentheses) are used
     * note that we can have separation by any whitespace, so we can have the file on one line if necessary
     * use # for comment. this goes till end of line
     */

    public TextControllerDirectory(Path controllerDir) throws IOException {
        this.directoryToWatch = controllerDir;
        this.changeListener = new ControllerChangeListener();
        this.watcher = DirectoryWatcher.builder()
                .path(controllerDir)
                .listener(changeListener)
                .build();
    }


}
