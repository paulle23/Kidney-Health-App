package com.example.firedatabase_assis

//stores the userSession so that other activities can access it without needing to pass intent
object UserSession {//object so that there is only one instance throughout app
    var name: String? = null//String? means it is nullable
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
