package com.example.privatecloudstorage.interfaces;

import java.io.File;

/**
 * Event listener for files
 */
public interface IFileEventListener extends IEventListener {
    void onFileAdded(File file, byte mode);
    void onFileRemoved(File file);
    void onFileChanged(File file);
    void onFileRenamed(File file, String oldName);
}
