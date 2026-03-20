package com.github.ghmxr.ftpshare.data;

import androidx.annotation.NonNull;

import java.io.Serializable;

public class AccountItem implements Serializable {
    public long id = -1;
    public String account = "";
    public String password = "";
    public String path = "";
    public String treeUri = "";
    public boolean writable = false;

    @Override
    @NonNull
    public String toString() {
        return "AccountItem{" +
                "id=" + id +
                ", account='" + account + '\'' +
                ", password='" + password + '\'' +
                ", path='" + path + '\'' +
                ", treeUri='" + treeUri + '\'' +
                ", writable=" + writable +
                '}';
    }
}
