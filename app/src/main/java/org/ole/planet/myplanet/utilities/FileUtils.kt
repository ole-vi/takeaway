package org.ole.planet.myplanet.utilities

import android.app.PendingIntent
import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import android.provider.MediaStore
import android.text.TextUtils
import android.webkit.MimeTypeMap
import androidx.annotation.RequiresApi
import org.ole.planet.myplanet.MainApplication
import android.content.pm.PackageInstaller
import org.ole.planet.myplanet.R
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.util.UUID

object FileUtils {
    private val SD_PATH = MainApplication.context.getExternalFilesDir(null).toString() + "/ole"
    @JvmStatic
    @Throws(IOException::class)
    fun fullyReadFileToBytes(f: File): ByteArray {
        val size = f.length().toInt()
        val bytes = ByteArray(size)
        val tmpBuff = ByteArray(size)
        val fis = FileInputStream(f)
        try {
            var read = fis.read(bytes, 0, size)
            if (read < size) {
                var remain = size - read
                while (remain > 0) {
                    read = fis.read(tmpBuff, 0, remain)
                    System.arraycopy(tmpBuff, 0, bytes, size - remain, read)
                    remain -= read
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            throw e
        } finally {
            fis.close()
        }
        return bytes
    }

    private fun createFilePath(folder: String, filename: String): File {
        val directory = File(MainApplication.context.getExternalFilesDir(null), folder)
        if (!directory.exists()) {
            try {
                if (!directory.mkdirs()) {
                    throw IOException("Failed to create directory: ${directory.absolutePath}")
                }
            } catch (e: IOException) {
                e.printStackTrace()
                throw RuntimeException("Failed to create directory: ${directory.absolutePath}", e)
            }
        }
        return File(directory, filename)
    }

    @JvmStatic
    fun getSDPathFromUrl(url: String?): File {
        return createFilePath("/ole/" + getIdFromUrl(url), getFileNameFromUrl(url))
    }

    @JvmStatic
    fun checkFileExist(url: String?): Boolean {
        if (url.isNullOrEmpty()) return false
        val f = getSDPathFromUrl(url)
        return f.exists()
    }

    @JvmStatic
    fun getFileNameFromUrl(url: String?): String {
        try {
            if (url != null) {
                return url.substring(url.lastIndexOf("/") + 1)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ""
    }

    @JvmStatic
    fun getIdFromUrl(url: String?): String {
        try {
            val sp = url?.substring(url.indexOf("resources/"))?.split("/".toRegex())
                ?.dropLastWhile { it.isEmpty() }
                ?.toTypedArray()
            return sp?.get(1) ?: ""
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ""
    }

    @JvmStatic
    fun getFileExtension(address: String?): String {
        if (TextUtils.isEmpty(address)) return ""
        val filenameArray = address?.split("\\.".toRegex())?.dropLastWhile { it.isEmpty() }?.toTypedArray()
        return filenameArray?.get(filenameArray.size - 1) ?: ""
    }

    @JvmStatic
    fun installApk(activity: Context, file: String?) {
        if (!file?.endsWith("apk")!!) return
        val toInstall = File(file)
        if (!toInstall.exists()) return
        try {
            val packageInstaller = activity.packageManager.packageInstaller
            val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
            val sessionId = packageInstaller.createSession(params)
            val session = packageInstaller.openSession(sessionId)
            addApkToInstallSession(toInstall, session)
            val intent = Intent(activity, activity.javaClass)
            val pendingIntent = PendingIntent.getActivity(activity, 0, intent,
                PendingIntent.FLAG_IMMUTABLE)
            val intentSender = pendingIntent.intentSender
            session.commit(intentSender)
            session.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Throws(IOException::class)
    private fun addApkToInstallSession(apkFile: File, session: PackageInstaller.Session) {
        val out: OutputStream = session.openWrite("my_app_session", 0, -1)
        val fis = FileInputStream(apkFile)
        fis.use { input ->
            out.use { output ->
                val buffer = ByteArray(4096)
                var length: Int
                while (input.read(buffer).also { length = it } != -1) {
                    output.write(buffer, 0, length)
                }
                session.fsync(out)
            }
        }
    }

    private fun getMimeType(url: String): String? {
        var type: String? = null
        val extension = MimeTypeMap.getFileExtensionFromUrl(url)
        if (extension != null) {
            val mime = MimeTypeMap.getSingleton()
            type = mime.getMimeTypeFromExtension(extension)
        }
        return type
    }

    @JvmStatic
    fun copyAssets(context: Context) {
        val tiles = arrayOf("dhulikhel.mbtiles", "somalia.mbtiles")
        val assetManager = context.assets
        try {
            for (s in tiles) {
                var out: OutputStream
                val `in`: InputStream = assetManager.open(s)
                val outFile = File(Environment.getExternalStorageDirectory().toString() + "/osmdroid", s)
                out = FileOutputStream(outFile)
                copyFile(`in`, out)
                out.close()
                `in`.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Throws(IOException::class)
    private fun copyFile(`in`: InputStream, out: OutputStream) {
        val buffer = ByteArray(1024)
        var read: Int
        while (`in`.read(buffer).also { read = it } != -1) {
            out.write(buffer, 0, read)
        }
    }

    @JvmStatic
    fun getRealPathFromURI(context: Context, contentUri: Uri?): String? {
        var cursor: Cursor? = null
        return try {
            val proj = arrayOf(MediaStore.Images.Media.DATA)
            cursor = contentUri?.let { context.contentResolver.query(it, proj, null, null, null) }
            val column_index = cursor?.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            cursor?.moveToFirst()
            cursor?.getString(column_index ?: 0)
        } finally {
            cursor?.close()
        }
    }

    @JvmStatic
    @Throws(Exception::class)
    fun convertStreamToString(`is`: InputStream?): String {
        val reader = BufferedReader(InputStreamReader(`is`))
        val sb = StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            sb.append(line).append("\n")
        }
        reader.close()
        return sb.toString()
    }

    @JvmStatic
    @Throws(Exception::class)
    fun getStringFromFile(fl: File?): String {
        val fin = FileInputStream(fl)
        val ret = convertStreamToString(fin)
        fin.close()
        return ret
    }

    @JvmStatic
    fun openOleFolder(): Intent {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        val uri = Uri.parse(Utilities.SD_PATH)  // Ensure org.ole.planet.myplanet.utilities.Utilities.SD_PATH is the correct path
        intent.setDataAndType(uri, "*/*")
        return Intent.createChooser(intent, "Open folder")
    }

    @JvmStatic
    fun getImagePath(context: Context, uri: Uri?): String? {
        if (uri == null) return null
        val projection = arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DATA)
        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(uri, projection, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                val documentIdIndex = cursor.getColumnIndex(MediaStore.Images.Media._ID)
                if (documentIdIndex >= 0) {
                    val documentId = cursor.getString(documentIdIndex)
                    cursor.close()
                    val selection = "${MediaStore.Images.Media._ID} = ?"
                    val selectionArgs = arrayOf(documentId)
                    cursor = context.contentResolver.query(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        projection,
                        selection,
                        selectionArgs,
                        null
                    )
                    if (cursor != null && cursor.moveToFirst()) {
                        val dataIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
                        if (dataIndex >= 0) {
                            val path = cursor.getString(dataIndex)
                            cursor.close()
                            return path
                        }
                    }
                }
            }
        } finally {
            cursor?.close()
        }
        return null
    }

    @JvmStatic
    fun getMediaType(path: String): String {
        val ext = getFileExtension(path)
        if (ext.equals("jpg", ignoreCase = true) || ext.equals("png", ignoreCase = true))
            return "image"
        else if (ext.equals("mp4", ignoreCase = true))
            return "mp4"
        else if (ext.equals("mp3", ignoreCase = true) || ext.equals("aac", ignoreCase = true))
            return "audio"
        return ""
    }

    // Disk space utilities
    @JvmStatic
    val totalInternalMemorySize: Long
        /**
         * @return Total internal memory capacity.
         */
        get() {
            val path = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            return totalBlocks * blockSize
        }

    @JvmStatic
    val availableInternalMemorySize: Long
        /**
         * Find space left in the internal memory.
         */
        get() {
            val path = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            val blockSize = stat.blockSizeLong
            val availableBlocks = stat.availableBlocksLong
            return availableBlocks * blockSize
        }

    @JvmStatic
    fun externalMemoryAvailable(): Boolean {
        return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    }

    @JvmStatic
    val availableExternalMemorySize: Long
        /**
         * Find space left in the external memory.
         */
        get() =// Not the best way to check, shows internal memory
            // when there is not external memory mounted
            if (externalMemoryAvailable()) {
                val path = Environment.getExternalStorageDirectory()
                val stat = StatFs(path.path)
                val blockSize = stat.blockSizeLong
                val availableBlocks = stat.availableBlocksLong
                availableBlocks * blockSize
            } else {
                0
            }

    @JvmStatic
    val totalExternalMemorySize: Long
        /**
         * @return Total capacity of the external memory
         */
        get() = if (externalMemoryAvailable()) {
            val path = Environment.getExternalStorageDirectory()
            val stat = StatFs(path.path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            totalBlocks * blockSize
        } else {
            0
        }

    /**
     * Coverts Bytes to KB/MB/GB and changes magnitude accordingly.
     *
     * @param size
     * @return A string with size followed by an appropriate suffix
     */
    @JvmStatic
    fun formatSize(size: Long): String {
        var size = size
        var suffix: String? = null
        if (size >= 1024) {
            suffix = "KB"
            size /= 1024
        }
        if (size >= 1024) {
            suffix = "MB"
            size /= 1024
        }
        if (size >= 1024) {
            suffix = "GB"
            size /= 1024
        }
        val resultBuffer = StringBuilder(size.toString())
        var commaOffset = resultBuffer.length - 3
        while (commaOffset > 0) {
            resultBuffer.insert(commaOffset, ',')
            commaOffset -= 3
        }
        if (suffix != null) resultBuffer.append(suffix)
        return resultBuffer.toString()
    }

    @JvmStatic
    val totalMemoryCapacity: Long
        @RequiresApi(Build.VERSION_CODES.O)
        get() = getStorageStats(MainApplication.context).first

    @JvmStatic
    val totalAvailableMemory: Long
        @RequiresApi(Build.VERSION_CODES.O)
        get() = getStorageStats(MainApplication.context).second

    @JvmStatic
    val totalAvailableMemoryRatio: Long
        @RequiresApi(Build.VERSION_CODES.O)
        get() {
            val total = totalMemoryCapacity
            val available = totalAvailableMemory
            return Math.round(available.toDouble() / total.toDouble() * 100)
        }

    @JvmStatic
    val availableOverTotalMemoryFormattedString: String
        @RequiresApi(Build.VERSION_CODES.O)
        get() {
            val context = MainApplication.context
            val available = totalAvailableMemory
            val total = totalMemoryCapacity
            return context.getString(R.string.available_space_colon) + formatSize(available) + "/" + formatSize(total)
        }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getStorageStats(context: Context): Pair<Long, Long> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val storageStatsManager =
                context.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager
            val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
            val storageVolume = storageManager.primaryStorageVolume

            // Get UUID of the internal storage
            val uuid =
                storageVolume.uuid?.let { UUID.fromString(it) } ?: StorageManager.UUID_DEFAULT

            // Get the total bytes and available bytes
            val totalBytes = storageStatsManager.getTotalBytes(uuid)
            val availableBytes = storageStatsManager.getFreeBytes(uuid)

            return Pair(totalBytes, availableBytes)
        } else {
            val path = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availableBlocks = stat.availableBlocksLong

            val totalBytes = blockSize * totalBlocks
            val availableBytes = blockSize * availableBlocks
            return Pair(totalBytes, availableBytes)
        }
    }
    fun extractFileName(filePath: String?): String?{
        if(filePath.isNullOrEmpty()) return null
        val regex = Regex(".+/(.+\\.[a-zA-Z0-9]+)")
        return regex.find(filePath)?.groupValues?.get(1)
    }

    fun nameWithoutExtension(fileName: String?): String?{
        extractFileName(fileName)
        val nameWithExtension = extractFileName(fileName)
        val nameWithoutExtension = nameWithExtension?.substringBeforeLast(".")
        return nameWithoutExtension
    }


}
