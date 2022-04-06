package com.example.privatecloudstorage.interfaces;

import java.io.File;

public interface IFileNotify extends INotify{
    void Notify(int event, File oldFile, File newFile);
}
