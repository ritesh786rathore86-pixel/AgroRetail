package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

data class SharedChatFileInfo(
    val file: File,
    val contentUri: String,
    val fileName: String,
    val fileSize: Long,
    val mimeType: String
)

object AgroFileSaveHelper {

    /**
     * Copies a selected document/PDF/photo from system picker into app's persistent chat storage.
     * Ensures files shared in chat remain permanently available in the conversation even if original cache is purged.
     */
    fun copyUriToChatStorage(
        context: Context,
        sourceUri: Uri,
        fallbackPrefix: String = "attachment"
    ): SharedChatFileInfo? {
        return try {
            val contentResolver = context.contentResolver
            var displayName = "$fallbackPrefix"
            var fileSize = 0L

            contentResolver.query(sourceUri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) {
                        displayName = cursor.getString(nameIndex) ?: "$fallbackPrefix"
                    }
                    if (sizeIndex != -1) {
                        fileSize = cursor.getLong(sizeIndex)
                    }
                }
            }

            val mimeType = contentResolver.getType(sourceUri) ?: when {
                displayName.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
                displayName.endsWith(".jpg", ignoreCase = true) || displayName.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
                displayName.endsWith(".png", ignoreCase = true) -> "image/png"
                displayName.endsWith(".doc", ignoreCase = true) || displayName.endsWith(".docx", ignoreCase = true) -> "application/msword"
                else -> "application/octet-stream"
            }

            val chatDir = File(context.filesDir, "chat_attachments")
            if (!chatDir.exists()) {
                chatDir.mkdirs()
            }

            // Clean sanitized file name
            val safeFileName = displayName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
            val targetFile = File(chatDir, "${System.currentTimeMillis()}_$safeFileName")

            contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }

            if (fileSize <= 0) {
                fileSize = targetFile.length()
            }

            SharedChatFileInfo(
                file = targetFile,
                contentUri = targetFile.absolutePath,
                fileName = displayName,
                fileSize = fileSize,
                mimeType = mimeType
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Resolves either an absolute file path or a content:// URI to a shareable FileProvider URI.
     */
    private fun getShareableUri(context: Context, pathOrUri: String): Uri? {
        return try {
            if (pathOrUri.startsWith("content://")) {
                Uri.parse(pathOrUri)
            } else {
                val file = File(pathOrUri)
                if (file.exists()) {
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                } else {
                    Uri.parse(pathOrUri)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Saves/Downloads a file permanently to the device's public Downloads directory.
     * Guaranteed to save real file on device, not just open a temporary preview.
     */
    fun saveFileToDeviceDownloads(
        context: Context,
        pathOrUri: String,
        suggestedFileName: String,
        mimeType: String
    ): Boolean {
        return try {
            val fileName = if (suggestedFileName.isNotBlank()) suggestedFileName else "document_${System.currentTimeMillis()}.pdf"
            val safeMime = if (mimeType.isNotBlank()) mimeType else "application/octet-stream"

            val sourceBytes = if (pathOrUri.startsWith("content://")) {
                context.contentResolver.openInputStream(Uri.parse(pathOrUri))?.use { it.readBytes() }
            } else {
                val f = File(pathOrUri)
                if (f.exists()) f.readBytes() else null
            }

            if (sourceBytes == null || sourceBytes.isEmpty()) {
                Toast.makeText(context, "Could not locate source file data to save.", Toast.LENGTH_SHORT).show()
                return false
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, safeMime)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/SVAgroMart")
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(sourceBytes)
                    }
                    Toast.makeText(context, "File saved to Downloads/SVAgroMart: $fileName", Toast.LENGTH_LONG).show()
                    return true
                }
            }

            // Fallback for older APIs or if MediaStore insert failed
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val agroDir = File(downloadDir, "SVAgroMart")
            if (!agroDir.exists()) agroDir.mkdirs()

            val destFile = File(agroDir, fileName)
            FileOutputStream(destFile).use { it.write(sourceBytes) }
            Toast.makeText(context, "File saved to: ${destFile.absolutePath}", Toast.LENGTH_LONG).show()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error saving file: ${e.message}", Toast.LENGTH_SHORT).show()
            false
        }
    }

    /**
     * Opens PDF or document in system-registered viewer.
     */
    fun openFile(context: Context, pathOrUri: String, mimeType: String, fileName: String = "") {
        try {
            val uri = getShareableUri(context, pathOrUri)
            if (uri == null) {
                Toast.makeText(context, "Unable to access file.", Toast.LENGTH_SHORT).show()
                return
            }
            val resolvedMime = if (mimeType.isNotBlank()) mimeType else {
                if (fileName.endsWith(".pdf", ignoreCase = true) || pathOrUri.endsWith(".pdf", ignoreCase = true)) "application/pdf"
                else "*/*"
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, resolvedMime)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(Intent.createChooser(intent, "Open $fileName"))
        } catch (e: Exception) {
            Toast.makeText(context, "No app found to open this document: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Shares file via system share sheet (WhatsApp, Email, Drive, etc.)
     */
    fun shareFile(context: Context, pathOrUri: String, fileName: String, mimeType: String) {
        try {
            val uri = getShareableUri(context, pathOrUri)
            if (uri == null) {
                Toast.makeText(context, "File not available for sharing.", Toast.LENGTH_SHORT).show()
                return
            }
            val resolvedMime = if (mimeType.isNotBlank()) mimeType else "*/*"

            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = resolvedMime
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, fileName)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(Intent.createChooser(sendIntent, "Share $fileName"))
        } catch (e: Exception) {
            Toast.makeText(context, "Could not share file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val kb = bytes / 1024.0
        if (kb < 1024) return "%.1f KB".format(kb)
        val mb = kb / 1024.0
        return "%.1f MB".format(mb)
    }
}
