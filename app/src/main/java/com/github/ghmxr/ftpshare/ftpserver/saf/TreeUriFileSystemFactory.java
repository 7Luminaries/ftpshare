package com.github.ghmxr.ftpshare.ftpserver.saf;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;

import com.github.ghmxr.ftpshare.utils.StorageAccessUtil;

import org.apache.ftpserver.ftplet.FileSystemFactory;
import org.apache.ftpserver.ftplet.FileSystemView;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.User;

public class TreeUriFileSystemFactory implements FileSystemFactory {
    private final Context appContext;

    public TreeUriFileSystemFactory(@NonNull Context context) {
        appContext = context.getApplicationContext();
    }

    @Override
    public FileSystemView createFileSystemView(User user) throws FtpException {
        String homeDirectory = user.getHomeDirectory();
        if (homeDirectory == null || homeDirectory.trim().length() == 0) {
            throw new FtpException("Missing home directory");
        }
        if (!StorageAccessUtil.canAccessTree(appContext, homeDirectory)) {
            throw new FtpException("Tree URI permission missing: " + homeDirectory);
        }
        DocumentFile root = StorageAccessUtil.getTreeDocument(appContext, homeDirectory);
        if (root == null) {
            throw new FtpException("Can not resolve tree URI: " + homeDirectory);
        }
        return new TreeUriFileSystemView(appContext, user, root);
    }
}
