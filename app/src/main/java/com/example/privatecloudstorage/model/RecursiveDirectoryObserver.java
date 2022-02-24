package com.example.privatecloudstorage.model;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import android.os.FileObserver;
import android.util.Log;

/**
 * A Directory Observer that observes all the files/folders within given directory recursively.
 * It automatically starts/stops monitoring folders/files
 */
public class RecursiveDirectoryObserver extends FileObserver {
    // Used for debugging
    private static final String TAG = "Recursive File Observer";

    // Holds all the directories inside the parent directory
    private final Map<String, FileObserver> mObservedDirectories = new HashMap<>();

    private String mObservedParentDirectory;

    private int mMask;

    private EventListener mListener;

    // Custom Event Listener
    public interface EventListener {
        void onEvent(int event, File file);
    }

    public RecursiveDirectoryObserver(String path, EventListener listener) {
        this(path, ALL_EVENTS, listener);
    }

    public RecursiveDirectoryObserver(String path, int mask, EventListener listener) {
        super(path, mask);
        mObservedParentDirectory = path;
        mMask = mask | FileObserver.CREATE | FileObserver.DELETE_SELF;
        mListener = listener;
    }

    /**
     * if the directory is new create a single directory observer
     *
     * @param path
     */
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

    /**
     * Start watching the parent directory recursively
     */
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

    /**
     * Check if it's a User Directory
     *
     * @param file
     * @return
     */
    private boolean isUserDirectory(File file) {
        return file.isDirectory() && !file.getName().equals(".") && !file.getName().equals("..");
    }

    /**
     * Stop watching a directory
     *
     * @param path
     */
    private void stopWatching(String path) {
        synchronized (mObservedDirectories) {
            FileObserver observer = mObservedDirectories.remove(path);
            if (observer != null) {
                observer.stopWatching();
            }
        }
    }

    /**
     * Stop watching the parent directory recursively
     */
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
        Log.d("TAG", "onEvent MAIN: =====================================" + "Current thread" + Thread.currentThread());
        File file;
        if (path == null) {
            file = new File(mObservedParentDirectory);
        } else {
            file = new File(mObservedParentDirectory, path);
        }
        notify(event, file);
    }

    /**
     * On Event call the user defined function
     *
     * @param event
     * @param file
     */
    private void notify(int event, File file) {
        if (mListener != null) {
            mListener.onEvent(event & FileObserver.ALL_EVENTS, file);
        }
    }

    /**
     * Class to observe a single directory
     */
    private class SingleDirectoryObserver extends FileObserver {
        private String mDirectoryPath;

        public SingleDirectoryObserver(String path, int mask) {
            super(path, mask);
            mDirectoryPath = path;
        }

        /**
         * Is Called on any passed event in a new thread
         * Must call startWatching ONCE to start listening to events
         *
         * If a new Directory created start watching it and its children
         * else if the observed directory deleted stop watching it and its children
         *
         * @param event
         * @param path
         */
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
                    RecursiveDirectoryObserver.this.stopWatching(mDirectoryPath);
                    break;
                case CREATE:
                case MOVED_TO:
                    if (isUserDirectory(file)) {
                        RecursiveDirectoryObserver.this.startWatching(file.getAbsolutePath());
                    }
                    break;
            }

            RecursiveDirectoryObserver.this.notify(event, file);
        }
    }
}