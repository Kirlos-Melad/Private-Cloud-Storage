package com.example.privatecloudstorage.model;

import java.util.ArrayList;

public class UserFile {
    public String Id;
    public String mode;
    public String Url;
    public ArrayList<UserFileVersion> VersionInformation = new ArrayList<>();
}