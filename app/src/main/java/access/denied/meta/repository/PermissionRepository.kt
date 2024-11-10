package access.denied.meta.repository

import android.content.Context
import androidx.lifecycle.LiveData
import access.denied.meta.dao.PermissionDao
import access.denied.meta.database.AppDatabase
import access.denied.meta.models.PermissionModel


class PermissionRepository(context: Context) {

    private val permissionDao: PermissionDao
    private val allPermissions: LiveData<List<PermissionModel>>

    init {
        val database: AppDatabase = AppDatabase.getInstance(context)
        permissionDao = database.permissionDao()
        allPermissions = permissionDao.getAll()
    }

    fun insert(permission: PermissionModel) {
        permissionDao.insert(permission)
    }


    fun update(permission: PermissionModel) {
        permissionDao.update(permission)
    }

    fun delete(permission: PermissionModel) {
        permissionDao.delete(permission)
    }

    fun deleteAllPermissions() {
        permissionDao.deleteAll()
    }
}