package com.example.firedatabase_assis

object UserSession {
    var name: String? = null
    var username: String? = null
    var password: String? = null
    var email: String? = null
    var demographic: String? = null

    fun clear() {
        name = null
        username = null
        password = null
        email = null
        demographic = null
    }
}