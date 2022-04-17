package com.example.privatecloudstorage.interfaces;

import java.io.File;

/**
 * File event notifier
 */
public interface IFileNotify extends INotify{
    /**
     * Called on event
     *
     * @param event event type
     * @param oldFile file before the event
     * @param newFile file after the event (may be null)
     */
    void Notify(int event, File oldFile, File newFile);
}
