package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.Retailer
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.util.SecurityUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthManager(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("agro_retail_auth_prefs", Context.MODE_PRIVATE)

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _currentRetailer = MutableStateFlow<Retailer?>(null)
    val currentRetailer: StateFlow<Retailer?> = _currentRetailer.asStateFlow()

    init {
        restoreSession()
    }

    private fun restoreSession() {
        val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        if (isLoggedIn) {
            val roleStr = prefs.getString(KEY_ROLE, UserRole.RETAILER.name) ?: UserRole.RETAILER.name
            val role = try { UserRole.valueOf(roleStr) } catch (e: Exception) { UserRole.RETAILER }
            val userId = prefs.getString(KEY_USER_ID, "") ?: ""
            val name = prefs.getString(KEY_NAME, "") ?: ""
            val mobile = prefs.getString(KEY_MOBILE, "") ?: ""
            val retailerId = prefs.getString(KEY_RETAILER_ID, "") ?: ""

            _currentUser.value = User(
                id = userId,
                name = name,
                mobileNumber = mobile,
                role = role,
                retailerId = retailerId
            )
        }
    }

    fun restoreRetailerObject(retailers: List<Retailer>) {
        val user = _currentUser.value
        if (user != null && user.role == UserRole.RETAILER && _currentRetailer.value == null) {
            val found = retailers.find { it.id == user.retailerId || it.mobileNumber == user.mobileNumber }
            if (found != null) {
                _currentRetailer.value = found
            }
        }
    }

    fun setAdminSession(name: String = "Distributor Admin", email: String = "admin@agroretail.com") {
        val user = User(
            id = "admin_master_1",
            name = name,
            email = email,
            mobileNumber = "9876500000",
            role = UserRole.ADMIN,
            retailerId = ""
        )
        saveUserToPrefs(user)
        _currentUser.value = user
        _currentRetailer.value = null
    }

    fun setRetailerSession(retailer: Retailer) {
        val user = User(
            id = retailer.id,
            name = retailer.retailerName,
            mobileNumber = retailer.mobileNumber,
            role = UserRole.RETAILER,
            retailerId = retailer.id
        )
        saveUserToPrefs(user)
        _currentUser.value = user
        _currentRetailer.value = retailer
    }

    fun updateCurrentRetailer(retailer: Retailer) {
        if (_currentUser.value?.role == UserRole.RETAILER && _currentUser.value?.id == retailer.id) {
            _currentRetailer.value = retailer
        }
    }

    fun logout() {
        prefs.edit().clear().apply()
        _currentUser.value = null
        _currentRetailer.value = null
    }

    private fun saveUserToPrefs(user: User) {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_USER_ID, user.id)
            .putString(KEY_NAME, user.name)
            .putString(KEY_MOBILE, user.mobileNumber)
            .putString(KEY_ROLE, user.role.name)
            .putString(KEY_RETAILER_ID, user.retailerId)
            .apply()
    }

    companion object {
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_NAME = "user_name"
        private const val KEY_MOBILE = "mobile_number"
        private const val KEY_ROLE = "user_role"
        private const val KEY_RETAILER_ID = "retailer_id"
    }
}
