package com.example.myapplication.permissions

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat


class PermissionUtilities {

    fun checkPermissions(activity: Activity, permissions: Array<out String>, requestCode: Int) {
        val notGrantedPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
        }
        if (notGrantedPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(activity, notGrantedPermissions.toTypedArray(), requestCode)
        }
    }

    fun checkPermissionsGranted(context: Context, permissions: Array<out String>): Boolean {
        return permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun dispatchOnRequestPermissionsResult(
        requestCode: Int, grantResults: IntArray,
        onGrantedMap: Map<Int, () -> Unit>, onDeniedMap: Map<Int, () -> Unit>
    ) {
        if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            onGrantedMap[requestCode]?.invoke()
        } else {
            onDeniedMap[requestCode]?.invoke()
        }
    }

    private fun checkPermissionGranted(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkGrantResults(grantResults: IntArray): Boolean {
        return grantResults.all { it == PackageManager.PERMISSION_GRANTED }
    }
}