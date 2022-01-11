package com.company.monitoring.service;

import com.company.monitoring.api.Handler;
import com.company.monitoring.service.fs.Dir;
import com.company.monitoring.service.task.TaskExecutor;
import com.company.monitoring.api.File;
import com.company.monitoring.service.task.impl.TaskExecutorImpl;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

@SuppressWarnings("rawtypes")
@Slf4j
public class Handlers {

    private final TaskExecutor<String> taskExecutor;
    private final Map<String, Handler> handlerInstances = new HashMap<>();
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    public static final String HANDLERS_MODE = "com.company.monitoring.handlers.mode";
    public static final String HANDLERS_MODE_AUTO = "auto";
    public static final String HANDLERS_MODE_MANUAL = "manual";

    public Handlers() {
        this(new TaskExecutorImpl<>());
    }

    Handlers(TaskExecutor<String> taskExecutor) {
        log.debug("init...");
        loadHandlers(System.getProperty(HANDLERS_MODE, HANDLERS_MODE_AUTO));
        this.taskExecutor = taskExecutor;
    }

    private void loadHandlers(String mode) {
        if (HANDLERS_MODE_AUTO.equalsIgnoreCase(mode)) {
            ServiceLoader.load(Handler.class).forEach(this::put);
        }
    }

    public void put(Handler<File> handler) {
        put(handler.getType(), handler);
    }

    public void put(String type, Class<? extends Handler> handlerClass) {
        log.debug("put type {} handler {}", type, handlerClass);
        try {
            Handler handlerInstance = handlerClass.getDeclaredConstructor().newInstance();
            put(type, handlerInstance);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            log.warn("Exception while instantiating handler type {} class {}", type, handlerClass);
            throw new RuntimeException(e);
        }
    }

    public void put(String type, Handler handler) {
        readWriteLock.writeLock().lock();
        try {
            log.debug("put type {} handler {}", type, handler);

            String typeFromHandler = handler.getType();
            if (!typeFromHandler.equals(type)) {
                log.warn("Passed type {} don't match type from handler {}, will use passed", type, typeFromHandler);
            }
            Handler put = handlerInstances.put(type, handler);
            if (put != null) {
                log.warn("Existing handler was replaced");
            }
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    public void remove(String type) {
        readWriteLock.writeLock().lock();
        try {
            log.debug("remove type {}", type);
            Handler remove = handlerInstances.remove(type);
            if (remove == null) {
                log.warn("There was no handler for type {}", type);
            }
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    public Handler get(String type) {
        readWriteLock.readLock().lock();
        try {
            return handlerInstances.get(type);
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    public void handleEvents(Dir dir, Path path, List<WatchEvent<?>> watchEvents) {
        log.debug("handleEvents dir {}, path {}, watchEvents {} ", dir, path, watchEvents.stream().map(we -> String.format("%s %s", we.kind(), we.context())).collect(Collectors.joining(", ")));
        List<WatchEvent<?>> collect = watchEvents.stream().filter(e -> ENTRY_MODIFY.equals(e.kind()) || ENTRY_CREATE.equals(e.kind())).collect(Collectors.toList());
        collect.forEach(e -> handleEvent(dir, path, e));
    }

    void handleEvent(Dir dir, Path path, WatchEvent<?> e) {
        log.debug("handleEvent dir {}, path {}, event {}", dir, path, String.format("%s %s", e.kind(), e.context()));
        if (ENTRY_CREATE.equals(e.kind())) {
            handleCreatedFile(dir, e);
        } else if (ENTRY_MODIFY.equals(e.kind())) {
            handleModifiedFile(dir, e);
        }
    }

    private void handleCreatedFile(Dir dir, WatchEvent<?> e) {
        String fileName = e.context().toString();
        log.info("CREATED FILE name: {} in dir: {}", fileName, dir.getPath());
    }

    private void handleModifiedFile(Dir dir, WatchEvent<?> e) {

        log.info("handleModifiedFile dir {}, event {}", dir, String.format("%s %s", e.kind(), e.context()));
        String fileName = e.context().toString();
        String fileExt = Utils.getExtension(fileName);
        try {
            Handler handler = get(fileExt);

            if (handler == null) return;
            log.debug("handler is not null {}", handler);
            ifModified(dir, fileName, file -> {
                log.debug("ifModified {}", file);
                FileHandlerTask task = new FileHandlerTask(handler, file);
                log.debug("submitTask {}", task);
                taskExecutor.submitTask(task);
            });
        } catch (IOException ex) {
            log.error(String.format("Error while processing modified file %s in dir %s", fileName, dir), ex);
        }

    }

    private void ifModified(Dir dir, String fileName, Consumer<File> modifiedHandler) throws IOException {
        Path filePath = Paths.get(dir.getPath(), fileName);
        byte[] bytes = Files.readAllBytes(filePath);
        Long crc32 = Utils.getCRC32Checksum(bytes);
        Long checksum = dir.getFileChecksums().get(fileName);
        boolean modified = !crc32.equals(checksum);
        if (modified) {
            File file = new File(fileName, bytes, crc32, filePath.toString());
            modifiedHandler.accept(file);
            dir.getFileChecksums().put(fileName, crc32);
        }
    }

    public void shutdown() {
        taskExecutor.shutdown();
    }
}
