package com.nothingwear.app.core.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

interface BluetoothPermissionChecker {
    fun requiredPermissions(): List<String>
    fun hasScanPermission(): Boolean
    fun hasConnectPermission(): Boolean
    fun hasAllBlePermissions(): Boolean = hasScanPermission() && hasConnectPermission()
}

@Singleton
class AndroidBluetoothPermissionChecker @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : BluetoothPermissionChecker {

    override fun requiredPermissions(): List<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
            )
        } else {
            listOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

    override fun hasScanPermission(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Manifest.permission.BLUETOOTH_SCAN
        } else {
            Manifest.permission.ACCESS_FINE_LOCATION
        }
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    override fun hasConnectPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT,
        ) == PackageManager.PERMISSION_GRANTED
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class BluetoothPermissionModule {
    @Binds
    @Singleton
    abstract fun bindBluetoothPermissionChecker(
        impl: AndroidBluetoothPermissionChecker,
    ): BluetoothPermissionChecker
}
