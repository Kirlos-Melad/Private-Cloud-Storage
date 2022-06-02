package com.example.privatecloudstorage.interfaces;

import java.io.File;

/**
 * Event listener for files
 */
public interface IFileEventListener extends IEventListener {
    void onFileAdded(File file, byte mode);
    void onFileRemoved(File file, byte mode);
    void onFileChanged(File file, byte mode);
    void onFileRenamed(File file, String oldName, byte mode);
}