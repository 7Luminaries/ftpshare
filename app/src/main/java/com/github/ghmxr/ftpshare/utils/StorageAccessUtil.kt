package com.github.ghmxr.ftpshare.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import com.github.ghmxr.ftpshare.R

object StorageAccessUtil {
    private const val DEFAULT_MIME_TYPE = "application/octet-stream"

    @JvmStatic
    fun createOpenDocumentTreeIntent(currentTreeUri: String?): Intent =
        Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !currentTreeUri.isNullOrBlank()) {
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, Uri.parse(currentTreeUri))
            }
        }

    @JvmStatic
    fun persistTreePermission(context: Context, uri: Uri, flags: Int) {
        val permissionFlags = flags and
            (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        context.contentResolver.takePersistableUriPermission(uri, permissionFlags)
    }

    @JvmStatic
    fun getTreeDocument(context: Context, treeUri: String?): DocumentFile? {
        if (treeUri.isNullOrBlank()) {
            return null
        }
        return try {
            DocumentFile.fromTreeUri(context, Uri.parse(treeUri))
        } catch (e: Exception) {
            null
        }
    }

    @JvmStatic
    fun hasPersistedTreePermission(context: Context, treeUri: String?): Boolean {
        if (treeUri.isNullOrBlank()) {
            return false
        }
        return context.contentResolver.persistedUriPermissions.any { permission ->
            permission.uri.toString() == treeUri &&
                (permission.isReadPermission || permission.isWritePermission)
        }
    }

    @JvmStatic
    fun canAccessTree(context: Context, treeUri: String?): Boolean {
        if (!hasPersistedTreePermission(context, treeUri)) {
            return false
        }
        val document = getTreeDocument(context, treeUri) ?: return false
        return document.canRead() || document.canWrite()
    }

    @JvmStatic
    fun getDirectorySummary(context: Context, treeUri: String?, legacyPath: String?): String {
        if (!treeUri.isNullOrBlank()) {
            val document = getTreeDocument(context, treeUri)
            if (document == null || !canAccessTree(context, treeUri)) {
                return context.getString(R.string.storage_directory_permission_lost)
            }
            return document.name?.takeIf { it.isNotBlank() }
                ?: context.getString(R.string.storage_directory_selected)
        }
        if (!legacyPath.isNullOrBlank()) {
            return context.getString(R.string.storage_directory_migration_needed)
        }
        return context.getString(R.string.storage_directory_not_selected)
    }

    @JvmStatic
    fun ensureDirectory(root: DocumentFile, relativePath: String): DocumentFile? {
        if (relativePath.isBlank()) {
            return root
        }
        var current: DocumentFile? = root
        for (segment in relativePath.split('/').filter { it.isNotBlank() }) {
            current = current?.let { ensureChildDirectory(it, segment) } ?: return null
        }
        return current
    }

    @JvmStatic
    fun ensureChildDirectory(parent: DocumentFile, name: String): DocumentFile? {
        val existing = parent.findFile(name)
        if (existing != null) {
            return if (existing.isDirectory) existing else null
        }
        return parent.createDirectory(name)
    }

    @JvmStatic
    fun ensureFile(parent: DocumentFile, name: String, mimeType: String = DEFAULT_MIME_TYPE): DocumentFile? {
        val existing = parent.findFile(name)
        if (existing != null) {
            return if (existing.isFile) existing else null
        }
        return parent.createFile(mimeType, name)
    }
}
