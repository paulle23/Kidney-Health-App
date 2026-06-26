package com.example.firedatabase_assis

//stores the userSession so that other activities can access it without needing to pass intent
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
