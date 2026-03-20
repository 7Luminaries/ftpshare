package com.github.ghmxr.ftpshare.ftpserver.saf;

import android.content.ContentResolver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

import com.github.ghmxr.ftpshare.utils.StorageAccessUtil;

import org.apache.ftpserver.ftplet.FtpFile;
import org.apache.ftpserver.usermanager.impl.WriteRequest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class TreeUriFtpFile implements FtpFile {
    private static final int BUFFER_SIZE = 8192;
    private static final String DEFAULT_MIME_TYPE = "application/octet-stream";

    private final TreeUriFileSystemView fileSystemView;
    private final String absolutePath;

    TreeUriFtpFile(@NonNull TreeUriFileSystemView fileSystemView, @NonNull String absolutePath) {
        this.fileSystemView = fileSystemView;
        this.absolutePath = absolutePath;
    }

    @Override
    @NonNull
    public String getName() {
        return fileSystemView.getName(absolutePath);
    }

    @Override
    public boolean isHidden() {
        return !"/".equals(absolutePath) && getName().startsWith(".");
    }

    @Override
    public boolean doesExist() {
        return resolveDocument() != null;
    }

    @Override
    public boolean isDirectory() {
        DocumentFile document = resolveDocument();
        return document != null && document.isDirectory();
    }

    @Override
    public boolean isFile() {
        DocumentFile document = resolveDocument();
        return document != null && document.isFile();
    }

    @Override
    public boolean isReadable() {
        DocumentFile document = resolveDocument();
        if (document == null) {
            return false;
        }
        return document.canRead() || document.canWrite();
    }

    @Override
    public boolean isWritable() {
        if (!hasWriteAuthorization()) {
            return false;
        }
        DocumentFile document = resolveDocument();
        if (document != null) {
            return document.canWrite();
        }
        DocumentFile parent = resolveParentDocument();
        return parent != null && parent.canWrite();
    }

    @Override
    public boolean delete() {
        if ("/".equals(absolutePath) || !hasWriteAuthorization()) {
            return false;
        }
        DocumentFile document = resolveDocument();
        return document != null && document.delete();
    }

    @Override
    public boolean move(FtpFile destination) {
        if (!(destination instanceof TreeUriFtpFile) || !hasWriteAuthorization()) {
            return false;
        }
        TreeUriFtpFile target = (TreeUriFtpFile) destination;
        if (absolutePath.equals(target.absolutePath)) {
            return true;
        }
        if (target.doesExist()) {
            return false;
        }
        DocumentFile sourceDocument = resolveDocument();
        DocumentFile sourceParent = resolveParentDocument();
        DocumentFile targetParent = target.resolveParentDocument();
        if (sourceDocument == null || sourceParent == null || targetParent == null || !targetParent.canWrite()) {
            return false;
        }
        if (fileSystemView.getParentPath(absolutePath).equals(fileSystemView.getParentPath(target.absolutePath))) {
            return sourceDocument.renameTo(target.getName());
        }
        try {
            if (!copyDocument(sourceDocument, targetParent, target.getName())) {
                return false;
            }
            return sourceDocument.delete();
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public boolean mkdir() {
        if ("/".equals(absolutePath) || !hasWriteAuthorization()) {
            return false;
        }
        if (doesExist()) {
            return isDirectory();
        }
        DocumentFile parent = resolveParentDocument();
        if (parent == null || !parent.canWrite()) {
            return false;
        }
        return StorageAccessUtil.ensureChildDirectory(parent, getName()) != null;
    }

    @Override
    @NonNull
    public List<FtpFile> listFiles() {
        DocumentFile document = resolveDocument();
        if (document == null || !document.isDirectory()) {
            return Collections.emptyList();
        }
        DocumentFile[] children = document.listFiles();
        ArrayList<FtpFile> list = new ArrayList<>(children.length);
        for (DocumentFile child : children) {
            String childName = child.getName();
            if (childName == null || childName.trim().length() == 0) {
                continue;
            }
            list.add(new TreeUriFtpFile(fileSystemView, fileSystemView.buildChildPath(absolutePath, childName)));
        }
        return list;
    }

    @Override
    public boolean setLastModified(long time) {
        return false;
    }

    @Override
    public long getSize() {
        DocumentFile document = resolveDocument();
        if (document == null || !document.isFile()) {
            return 0L;
        }
        return document.length();
    }

    @Override
    @NonNull
    public String getAbsolutePath() {
        return absolutePath;
    }

    @Override
    public long getLastModified() {
        DocumentFile document = resolveDocument();
        return document == null ? 0L : document.lastModified();
    }

    @Override
    @NonNull
    public String getOwnerName() {
        return fileSystemView.getUser().getName();
    }

    @Override
    @NonNull
    public String getGroupName() {
        return fileSystemView.getUser().getName();
    }

    @Override
    public int getLinkCount() {
        return 1;
    }

    @Override
    @Nullable
    public Object getPhysicalFile() {
        return resolveDocument();
    }

    @Override
    @NonNull
    public InputStream createInputStream(long offset) throws IOException {
        DocumentFile document = resolveDocument();
        if (document == null || !document.isFile()) {
            throw new IOException("File not found: " + absolutePath);
        }
        InputStream inputStream = fileSystemView.getContext().getContentResolver().openInputStream(document.getUri());
        if (inputStream == null) {
            throw new IOException("Can not open input stream: " + absolutePath);
        }
        skipFully(inputStream, offset);
        return inputStream;
    }

    @Override
    @NonNull
    public OutputStream createOutputStream(long offset) throws IOException {
        if ("/".equals(absolutePath) || !hasWriteAuthorization()) {
            throw new IOException("File is not writable: " + absolutePath);
        }
        DocumentFile parent = resolveParentDocument();
        if (parent == null || !parent.canWrite()) {
            throw new IOException("Parent directory is not writable: " + absolutePath);
        }
        DocumentFile target = resolveDocument();
        if (target != null && target.isDirectory()) {
            throw new IOException("Destination is a directory: " + absolutePath);
        }
        if (target == null) {
            target = StorageAccessUtil.ensureFile(parent, getName(), DEFAULT_MIME_TYPE);
        }
        if (target == null) {
            throw new IOException("Can not create file: " + absolutePath);
        }
        if (offset > 0 && target.length() != offset) {
            throw new IOException("Random access write is not supported: " + absolutePath);
        }
        String mode = offset > 0 ? "wa" : "wt";
        OutputStream outputStream = fileSystemView.getContext().getContentResolver().openOutputStream(target.getUri(), mode);
        if (outputStream == null) {
            throw new IOException("Can not open output stream: " + absolutePath);
        }
        return outputStream;
    }

    @Override
    public boolean isRemovable() {
        return !"/".equals(absolutePath);
    }

    @Nullable
    private DocumentFile resolveDocument() {
        return fileSystemView.findDocument(absolutePath);
    }

    @Nullable
    private DocumentFile resolveParentDocument() {
        return fileSystemView.findParentDocument(absolutePath);
    }

    private boolean hasWriteAuthorization() {
        return fileSystemView.getUser().authorize(new WriteRequest(absolutePath)) != null;
    }

    private boolean copyDocument(@NonNull DocumentFile source, @NonNull DocumentFile targetParent, @NonNull String targetName) throws IOException {
        if (source.isDirectory()) {
            DocumentFile destinationDirectory = StorageAccessUtil.ensureChildDirectory(targetParent, targetName);
            if (destinationDirectory == null) {
                return false;
            }
            for (DocumentFile child : source.listFiles()) {
                String childName = child.getName();
                if (childName == null || childName.trim().length() == 0) {
                    continue;
                }
                if (!copyDocument(child, destinationDirectory, childName)) {
                    return false;
                }
            }
            return true;
        }
        String mimeType = source.getType();
        if (mimeType == null || mimeType.trim().length() == 0) {
            mimeType = DEFAULT_MIME_TYPE;
        }
        DocumentFile destinationFile = StorageAccessUtil.ensureFile(targetParent, targetName, mimeType);
        if (destinationFile == null) {
            return false;
        }
        ContentResolver contentResolver = fileSystemView.getContext().getContentResolver();
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            inputStream = contentResolver.openInputStream(source.getUri());
            outputStream = contentResolver.openOutputStream(destinationFile.getUri(), "wt");
            if (inputStream == null || outputStream == null) {
                return false;
            }
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            outputStream.flush();
            return true;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ignored) {
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private void skipFully(@NonNull InputStream inputStream, long offset) throws IOException {
        long remaining = offset;
        while (remaining > 0) {
            long skipped = inputStream.skip(remaining);
            if (skipped <= 0) {
                if (inputStream.read() == -1) {
                    throw new IOException("Reached EOF while skipping to offset " + offset);
                }
                skipped = 1;
            }
            remaining -= skipped;
        }
    }
}
