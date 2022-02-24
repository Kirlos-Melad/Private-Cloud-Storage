package com.example.privatecloudstorage.model;

import android.os.FileObserver;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * A FileObserver that observes all the files/folders within given directory
 * recursively. It automatically starts/stops monitoring new folders/files
 * created after starting the watch.
 */
public class RecursiveFileObserver extends FileObserver {

    private final Map<String, FileObserver> mObservedDirectories = new HashMap<>();

    private String mObservedParentDirectory;

    private int mMask;

    private EventListener mListener;

    public interface EventListener {
        void onEvent(int event, File file);
    }

    public RecursiveFileObserver(String path, EventListener listener) {
        this(path, ALL_EVENTS, listener);
    }

    public RecursiveFileObserver(String path, int mask, EventListener listener) {
        super(path, mask);
        mObservedParentDirectory = path;
        mMask = mask | FileObserver.CREATE | FileObserver.DELETE_SELF;
        mListener = listener;
    }

    private void startWatching(String path) {
        synchronized (mObservedDirectories) {
            if (mObservedDirectories.containsKey(path)) {
                return;
            }
            FileObserver observer = new SingleDirectoryObserver(path, mMask);
            observer.startWatching();
            mObservedDirectories.put(path, observer);
        }
    }

    @Override
    public void startWatching() {
        Stack<String> stack = new Stack<>();
        stack.push(mObservedParentDirectory);

        // Recursively watch all child directories
        while (!stack.empty()) {
            String parent = stack.pop();
            startWatching(parent);

            File path = new File(parent);
            File[] files = path.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (isUserDirectory(file)) {
                        stack.push(file.getAbsolutePath());
                    }
                }
            }
        }
    }

    private boolean isUserDirectory(File file) {
        return file.isDirectory() && !file.getName().equals(".") && !file.getName().equals("..");
    }

    private void stopWatching(String path) {
        synchronized (mObservedDirectories) {
            FileObserver observer = mObservedDirectories.remove(path);
            if (observer != null) {
                observer.stopWatching();
            }
        }
    }

    @Override
    public void stopWatching() {
        synchronized (mObservedDirectories) {
            for (FileObserver observer : mObservedDirectories.values()) {
                observer.stopWatching();
            }
            mObservedDirectories.clear();
        }
    }

    /**
     * Is Called on any passed event in a new thread
     * Must call startWatching ONCE to start listening to events
     *
     * @param event
     * @param path
     */
    @Override
    public void onEvent(int event, String path) {
        File file;
        if (path == null) {
            file = new File(mObservedParentDirectory);
        } else {
            file = new File(mObservedParentDirectory, path);
        }
        notify(event, file);
    }

    private void notify(int event, File file) {
        if (mListener != null) {
            mListener.onEvent(event & FileObserver.ALL_EVENTS, file);
        }
    }

    private class SingleDirectoryObserver extends FileObserver {
        private String mDirectoryPath;

        public SingleDirectoryObserver(String path, int mask) {
            super(path, mask);
            mDirectoryPath = path;
        }

        @Override
        public void onEvent(int event, String path) {
            File file;
            if (path == null) {
                file = new File(mDirectoryPath);
            } else {
                file = new File(mDirectoryPath, path);
            }

            switch (event & FileObserver.ALL_EVENTS) {
                case DELETE_SELF:
                    RecursiveFileObserver.this.stopWatching(mDirectoryPath);
                    break;
                case CREATE:
                case MOVED_TO:
                    if (isUserDirectory(file)) {
                        RecursiveFileObserver.this.startWatching(file.getAbsolutePath());
                    }
                    break;
            }

            RecursiveFileObserver.this.notify(event, file);
        }
    }
}