package com.mab.documentuploader

import android.content.ContentResolver
import android.net.Uri
import android.provider.MediaStore
import java.util.regex.Pattern

fun String.isEmailValid(): Boolean {
    val emailPattern = Pattern.compile("[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]{2}+[a-z]*")
    return emailPattern.matcher(this.trim()).matches() && this.trim().isNotEmpty()
}


fun Uri.getRealPathFromURI(contentResolver: ContentResolver): String? {
    var res: String? = null
    val proj = arrayOf(MediaStore.Images.Media.DATA)
    val cursor = contentResolver.query(this, proj, null, null, null)
    cursor?.let {
        if (cursor.moveToFirst()) {
            res = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA))
        }
        cursor.close()
        return res
    } ?: return null


}