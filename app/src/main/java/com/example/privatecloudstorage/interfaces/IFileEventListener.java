package com.example.privatecloudstorage.interfaces;

import java.io.File;

public interface IFileEventListener extends IEventListener {
    void onFileAdded(File file);
    void onFileRemoved(File file);
    void onFileChanged(File file);
    void onFileRenamed(File file, String oldName);
}
