package com.github.ghmxr.ftpshare.ftpserver.saf;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

import org.apache.ftpserver.ftplet.FileSystemView;
import org.apache.ftpserver.ftplet.FtpFile;
import org.apache.ftpserver.ftplet.User;

import java.util.ArrayList;
import java.util.List;

class TreeUriFileSystemView implements FileSystemView {
    private final Context appContext;
    private final User user;
    private final DocumentFile root;
    private String currentPath = "/";

    TreeUriFileSystemView(@NonNull Context context, @NonNull User user, @NonNull DocumentFile root) {
        this.appContext = context.getApplicationContext();
        this.user = user;
        this.root = root;
    }

    @Override
    public FtpFile getHomeDirectory() {
        return new TreeUriFtpFile(this, "/");
    }

    @Override
    public FtpFile getWorkingDirectory() {
        return new TreeUriFtpFile(this, currentPath);
    }

    @Override
    public boolean changeWorkingDirectory(String path) {
        String absolutePath = resolveAbsolutePath(path);
        TreeUriFtpFile ftpFile = new TreeUriFtpFile(this, absolutePath);
        if (!ftpFile.doesExist() || !ftpFile.isDirectory()) {
            return false;
        }
        currentPath = absolutePath;
        return true;
    }

    @Override
    public FtpFile getFile(String path) {
        return new TreeUriFtpFile(this, resolveAbsolutePath(path));
    }

    @Override
    public boolean isRandomAccessible() {
        return false;
    }

    @Override
    public void dispose() {
    }

    @NonNull
    Context getContext() {
        return appContext;
    }

    @NonNull
    User getUser() {
        return user;
    }

    @NonNull
    DocumentFile getRoot() {
        return root;
    }

    @NonNull
    String resolveAbsolutePath(@Nullable String rawPath) {
        String source = rawPath == null ? "" : rawPath.trim();
        boolean isAbsolute = source.startsWith("/");
        List<String> result = new ArrayList<>();
        if (!isAbsolute) {
            result.addAll(splitPath(currentPath));
        }
        for (String part : splitPath(source)) {
            if (".".equals(part)) {
                continue;
            }
            if ("..".equals(part)) {
                if (!result.isEmpty()) {
                    result.remove(result.size() - 1);
                }
                continue;
            }
            result.add(part);
        }
        if (result.isEmpty()) {
            return "/";
        }
        StringBuilder builder = new StringBuilder();
        for (String part : result) {
            builder.append('/').append(part);
        }
        return builder.toString();
    }

    @NonNull
    String getName(@NonNull String absolutePath) {
        if ("/".equals(absolutePath)) {
            return "/";
        }
        int index = absolutePath.lastIndexOf('/');
        return index < 0 ? absolutePath : absolutePath.substring(index + 1);
    }

    @NonNull
    String getParentPath(@NonNull String absolutePath) {
        if ("/".equals(absolutePath)) {
            return "/";
        }
        int index = absolutePath.lastIndexOf('/');
        if (index <= 0) {
            return "/";
        }
        return absolutePath.substring(0, index);
    }

    @NonNull
    String buildChildPath(@NonNull String parentPath, @NonNull String name) {
        if ("/".equals(parentPath)) {
            return "/" + name;
        }
        return parentPath + "/" + name;
    }

    @Nullable
    DocumentFile findDocument(@NonNull String absolutePath) {
        DocumentFile current = root;
        if ("/".equals(absolutePath)) {
            return current;
        }
        for (String segment : splitPath(absolutePath)) {
            current = current.findFile(segment);
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    @Nullable
    DocumentFile findParentDocument(@NonNull String absolutePath) {
        return findDocument(getParentPath(absolutePath));
    }

    @NonNull
    private List<String> splitPath(@Nullable String path) {
        ArrayList<String> parts = new ArrayList<>();
        if (path == null || path.length() == 0) {
            return parts;
        }
        String normalized = path.replace('\\', '/');
        for (String part : normalized.split("/")) {
            if (part == null) {
                continue;
            }
            String trimmed = part.trim();
            if (trimmed.length() == 0) {
                continue;
            }
            parts.add(trimmed);
        }
        return parts;
    }
}
