package com.example.privatecloudstorage.interfaces;

import java.io.File;

public interface IMediatorEventListener extends IEventListener{
    public void onGroupMembersUpdated(String groupId);
    public void onFolderUpdated(File folder);
}
