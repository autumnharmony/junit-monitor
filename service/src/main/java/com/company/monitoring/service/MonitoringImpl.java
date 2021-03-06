package com.company.monitoring.service;

import com.company.monitoring.api.Configurable;
import com.company.monitoring.api.Handler;
import com.company.monitoring.api.Monitoring;
import com.company.monitoring.service.fs.Dir;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.Watchable;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;


@Slf4j
public class MonitoringImpl implements Monitoring {

    public enum State {
        STARTING(1),
        RUNNING(2),
        STOPPING(3),
        STOPPED(4);

        private final int value;

        State(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

    }

    private WatchService watchService;
    private final ExecutorService watchServiceExecutor;
    private final ExecutorService fsChangesExecutor;
    private Handlers handlers;

    private final Map<String, Dir> pathToDir = new ConcurrentHashMap<>();
    private final Set<Dir> dirsToRegister = ConcurrentHashMap.newKeySet();
    private final Map<Dir, WatchKey> watchKeys = new ConcurrentHashMap<>();

    private final AtomicBoolean needStop = new AtomicBoolean(false);
    private final AtomicReference<State> state = new AtomicReference<>(State.STOPPED);
    private final Set<Future> futures = ConcurrentHashMap.newKeySet();

    public boolean isInProgress() {
        return futures.stream().anyMatch(f -> !f.isCancelled() && !f.isDone());
    }

    public AtomicReference<State> getState() {
        return state;
    }

    void setHandlers(Handlers handlers) {
        this.handlers = handlers;
    }

    void setWatchService(WatchService watchService) {
        this.watchService = watchService;
    }

    public MonitoringImpl() {
        handlers = new Handlers();
        fsChangesExecutor = Executors.newCachedThreadPool(runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName(String.format("%s-%s", "fsChangesExecutor", UUID.randomUUID()));
            return thread;
        });
        watchServiceExecutor = Executors.newCachedThreadPool(runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName(String.format("%s-%s", "watchServiceExecutor", UUID.randomUUID()));
            return thread;
        });
    }


    MonitoringImpl(ExecutorService watchServiceExecutor, WatchService watchService, ExecutorService fsChangesExecutor, Handlers handlers) {
        this.watchServiceExecutor = watchServiceExecutor;
        this.watchService = watchService;
        this.fsChangesExecutor = fsChangesExecutor;
        this.handlers = handlers;
    }

    @Override
    public void monitorDir(String pathString) {
        log.debug("monitorDir {}", pathString);
        Dir dir = pathToDir.computeIfAbsent(pathString, this::createDir);

        if (!isRunning()) {
            registerLater(dir);
        } else {
            registerNow(dir);
        }
    }

    public boolean isRunning() {
        return State.RUNNING.equals(getState().get());
    }

    Dir createDir(String pathString) {
        return new Dir(pathString);
    }

    void registerNow(Dir dir, boolean force) {
        log.debug("registerNow dir {}, force {}", dir, force);
        String path = dir.getPath();
        if (Files.notExists(Paths.get(path))) {
            try {
                Files.createDirectories(Paths.get(path));
            } catch (IOException e) {
                log.warn("Cant create directory {}", dir);
                return;
            }
        }

        if (!watchKeyExists(dir) || force) {
            try {
                WatchKey watchKey = register(path);
                boolean active = true;
                watchKeys.put(dir, watchKey);
                dir.setActive(active);
                log.info("DIR {} REGISTERED", path);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            log.warn("Already registered");
        }
    }

    void registerNow(Dir dir) {
        registerNow(dir, false);
    }

    WatchKey register(String path) throws IOException {
        return Paths.get(path).register(watchService, ENTRY_MODIFY);
    }

    void registerLater(Dir dir) {
        log.debug("registerLater dir {}", dir);
        dirsToRegister.add(dir);
    }

    private boolean watchKeyExists(Dir dir) {
        return watchKeys.containsKey(dir);
    }

    @Override
    public void forgetDir(String path) {
        log.debug("forgetDir path {}", path);
        Dir dir = pathToDir.get(path);
        this.forgetDir(dir);
    }

    private void forgetDir(Dir dir) {
        dir.setActive(false);
        log.info("DIR {} will be UNREGISTERED", dir.getPath());
    }


    @Override
    public void start() {
        log.debug("start");

        if (state.compareAndSet(State.STOPPED, State.STARTING)) {
            log.info("oldState {}", State.STOPPED);
            startImpl();
            state.set(State.RUNNING);
            log.info("newState {}", state.get());
        } else {
            log.info("Monitoring state: {}", state.get());
        }

    }

    protected void startImpl() {
        log.debug("startImpl");
        needStop.set(false);
        try {
            if (watchService == null) {
                watchService = createWatchService();
            }
        } catch (IOException e) {
            throw new RuntimeException("Can't create watch service", e);
        }

        watchServiceExecutor.submit(() -> {
            log.debug("watchServiceExecutor submitted while true task");
            while (true) {
                AtomicBoolean needStop = getNeedStop();
                if (needStop.get()) {
                    log.debug("needStop == true, will break loop");
                    break;
                }
                WatchKey poll = watchService.poll();
                if (poll == null) continue;
                log.info("some changes in directory detected");
                String dirPath = poll.watchable().toString();
                List<WatchEvent<?>> watchEvents = poll.pollEvents();
                Watchable watchable = poll.watchable();

                log.info("watchable: {}", watchable);
                log.info("pollEvents: {}", watchEvents.stream().map(e -> e.kind() + " " + e.context()).collect(Collectors.toList()));
                if (pathToDir.containsKey(dirPath)) {
                    Dir dir = pathToDir.get(dirPath);
                    if (dir.isActive()) {
                        log.info("dir is active");
                        Path watchablePath = Paths.get(((Path) poll.watchable()).toString());
                        List<WatchEvent<?>> events = new ArrayList<>(watchEvents);
                        if (!events.isEmpty()) {
                            log.info("submit to fsChangesExecutor {}, events {}", poll, watchEvents.stream().map(event -> event.context() + ", " + event.kind()).map(Objects::toString).collect(Collectors.joining("\n")));
                            Future<?> submit = fsChangesExecutor.submit(() -> handlePoll(watchablePath, events));
                            futures.add(submit);
                        }
                        boolean valid = poll.reset();
                        if (!valid) {
                            registerNow(pathToDir.get(dirPath), true);
                        }
                    } else {
                        log.info("dir is not active");
                        // inactive
                        Dir remove = pathToDir.remove(dirPath);
                        WatchKey wk = watchKeys.remove(remove);
                        log.info("DIR {} removed from monitoring", remove);
                    }
                }
            }
        });
        log.info("Server started");

        dirsToRegister.forEach(this::registerNow);
        dirsToRegister.clear();
    }

    @Override
    public void stop() {
        log.debug("stop");
        if (state.compareAndSet(State.RUNNING, State.STOPPING)) {
            log.info("oldState {}", State.RUNNING);
            stopImpl();
            state.set(State.STOPPED);
            log.info("newState {}", state.get());
        } else {
            log.info("Monitoring is not running, nothing to stop");
        }
    }

    private void stopImpl() {
        log.debug("stopImpl");
        needStop.set(true);
        log.debug("needStop set to true");

        // unregister
        pathToDir.values().forEach(this::forgetDir);
        dirsToRegister.addAll(pathToDir.values());
        pathToDir.clear();
    }

    @Override
    public void shutdown() {
        log.info("shutdown...");
        try {
            watchService.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        fsChangesExecutor.shutdown();
        watchServiceExecutor.shutdown();
        handlers.shutdown();
    }

    @Override
    public void assignHandler(String type, String clazz) {
        log.info("assignHandler type: {}, class: {}", type, clazz);
        try {
            Class<Handler> aClass = (Class<Handler>) Class.forName(clazz);
            Handler handlerInstance = aClass.getDeclaredConstructor().newInstance();
            assignHandler(type, handlerInstance);
        } catch (ClassNotFoundException | InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void assignHandler(String type, String clazz, Object[] extra) {
        log.info("assignHandler type: {}, class: {}, extra: {}", type, clazz, extra);

        try {
            Class[] parameterTypes = Arrays.stream(extra).map(x -> x.getClass()).toArray(Class[]::new);
            Class<Handler> aClass = (Class<Handler>) Class.forName(clazz);
            Handler handlerInstance = aClass.getDeclaredConstructor(parameterTypes).newInstance(extra);
            assignHandler(type, handlerInstance);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException | ClassNotFoundException e) {
            log.warn("Exception while instantiating handler type {} class {}", type, clazz);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void assignHandler(String type, Handler handler) {
        log.info("assignHandler type: {}, handler: {}", type, handler);
        handlers.put(type, handler);
    }

    @Override
    public void revokeHandler(String type) {
        log.info("revokeHandler type: {}", type);
        handlers.remove(type);
    }

    @Override
    public Handler getHandler(String type) {
        return handlers.get(type);
    }

    @Override
    public void configureHandler(String type, Object[] objects) {
        Handler handler = handlers.get(type);
        if (handler == null) {
            throw new IllegalStateException(String.format("Can't find handler for type %s", type));
        }
        if (handler instanceof Configurable) {
            ((Configurable) handler).configure(objects);
        }
    }

    public AtomicBoolean getNeedStop() {
        return needStop;
    }

    private WatchService createWatchService() throws IOException {
        log.debug("creating watch service");
        return FileSystems.getDefault().newWatchService();
    }

    void handlePoll(Path path, List<WatchEvent<?>> watchEvents) {
        log.debug("handlePoll {}", path);
        if (path != null) {
            log.debug("watchable {}", path);
            String pathString = path.toString();
            Dir dir = pathToDir.get(pathString);
            if (dir != null) {
                log.debug("dir is not null, will handle events");
                handlers.handleEvents(dir, path, watchEvents);
            }
        }
    }

}
