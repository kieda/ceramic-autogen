package io.hostilerobot.ceramicrelief.controller;

import io.methvin.watcher.DirectoryChangeEvent;
import io.methvin.watcher.DirectoryChangeListener;
import io.methvin.watcher.DirectoryWatcher;
import io.methvin.watcher.hashing.FileHasher;
import org.apache.commons.io.IOExceptionList;
import org.apache.commons.io.filefilter.DelegateFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.OrFileFilter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Usage:
 *
 * TextControllerDirectory
 *      .builder()
 *      .and(TextControllerFactory.fileExtension("json"), f -> JsonTextController(f))
 *      .and(TextControllerFactory.fileExtension("xml"), f -> XMLTextController(f))
 *      .and(new CustomTextControllerFactory())
 *      .build();
 */
public class TextControllerDirectory {
    private final List<TextControllerFactory> factories;
    private final File directoryToWatch;
    private final DirectoryWatcher watcher;
    private final ControllerChangeListener changeListener;
    // maps a file to a set of TextControllerFiles
    // the Integer defines the position in the factories: List<TextControllerFactory>
    // this way
    private final Map<File, Map<Integer, TextController>> individualControllers;
    private final Consumer<Throwable> onError;

    private class ControllerChangeListener implements DirectoryChangeListener{
        @Override
        public void onEvent(DirectoryChangeEvent directoryChangeEvent) throws IOException {
            if(directoryChangeEvent.isDirectory()) {
                // do nothing on directories
                return;
            }

            Path path = directoryChangeEvent.path();
            
            switch(directoryChangeEvent.eventType()) {
                case MODIFY:
                case CREATE:
                    File updatedFile = path.toFile();
                    try {
                        // if there's a problem creating a controller (e.g. malformed data), don't pass the error up
                        // which will stop the watcher. Instead, notify the user
                        createControllersOnFile(updatedFile.getParentFile(), updatedFile.getName());
                    } catch (IOException ex) {
                        onError.accept(ex);
                    }
                    break;
                case DELETE:
                    File deletedFile = path.toFile();

                    // release all controllers on the file
                    individualControllers.get(deletedFile).forEach( (idx, controller) -> {
                        controller.release();
                    });

                    // clean up map entry
                    individualControllers.remove(deletedFile);

                    break;
                case OVERFLOW:
                    throw new IOException("Overflow: " + directoryChangeEvent.path());
            }
        }
    }

    private TextControllerDirectory(File controller, List<TextControllerFactory> factories,
                                    final Runnable onComplete,
                                    final Consumer<Throwable> onError) throws IOException {
        if(!controller.isDirectory()) {
            throw new IOException("file " + controller + " is not a directory");
        }
        this.onError = onError;
        this.directoryToWatch = Objects.requireNonNull(controller);
        this.factories = Objects.requireNonNull(factories);
        if(factories.isEmpty()) {
            throw new IllegalArgumentException("Expected >= 1 factory, got 0");
        }

        individualControllers = new HashMap<>();

        this.changeListener = new ControllerChangeListener();
        this.watcher = DirectoryWatcher.builder()
                .path(directoryToWatch.toPath())
                .fileHasher(FileHasher.LAST_MODIFIED_TIME)
                .listener(changeListener)
                .build();

        loadAllFiles();
        CompletableFuture<Void> whenDone = this.watcher.watchAsync();
        whenDone.whenComplete((v, err) -> {
            // release each controller
            individualControllers.values().forEach(map -> map.values().forEach(TextController::release));
            // clear all maps
            individualControllers.values().forEach(Map::clear);
            individualControllers.clear();
            // todo - clean up internal state of directorycontroller and all file controllers
            if (err == null && onComplete != null) onComplete.run();
            else if(err != null && onError != null) onError.accept(err);
        });
    }

    private void createControllersOnFile(File parentFolder, String child) throws IOException{
        File childFile = new File(parentFolder, child);

        List<IOException> exceptions = null;

        for(int i = 0; i < factories.size(); i++) {
            TextControllerFactory factory = factories.get(i);
            if(factory.accept(parentFolder, child)) {
                var map = individualControllers.computeIfAbsent(childFile, key -> new HashMap<>());
                try {
                    if (map.containsKey(i)) {
                        map.get(i).update(new FileInputStream(childFile));
                    } else {
                        map.put(i, factory.newController(childFile));
                    }
                } catch(IOException ex) {
                    if(exceptions == null) exceptions = new ArrayList<>();
                    exceptions.add(ex);
                }
            }
        }
        if(exceptions != null) {
            throw new IOExceptionList(exceptions);
        }
    }

    /**
     * creates the initial state by loading all of the files in the directory and make matching ones
     * mapped to the TextControllerFile.
     * @throws IOException
     */
    private void loadAllFiles() throws IOException {
        List<IOFileFilter> filters = factories.stream()
                .map(DelegateFileFilter::new)
                .collect(Collectors.toUnmodifiableList());
        for(String child: Objects.requireNonNull(directoryToWatch.list(new OrFileFilter(filters)))) {
            createControllersOnFile(directoryToWatch, child);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder{
        private final List<TextControllerFactory> factories = new ArrayList<>();
        private Runnable onComplete = null;
        private Consumer<Throwable> onError = null;
        public Builder and(TextControllerFactory factory){
            factories.add(factory);
            return this;
        }
        public Builder and(FilenameFilter matcher, Function<File, TextController> newController) {
            factories.add(new DelegateControllerFactory(matcher, newController));
            return this;
        }
        public Builder onComplete(Runnable onComplete) {
            this.onComplete = onComplete;
            return this;
        }
        public Builder onError(Consumer<Throwable> onError) {
            this.onError = onError;
            return this;
        }
        public TextControllerDirectory build(Path directory) throws IOException {
            return new TextControllerDirectory(directory.toFile(), factories, onComplete, onError);
        }
    }
}
