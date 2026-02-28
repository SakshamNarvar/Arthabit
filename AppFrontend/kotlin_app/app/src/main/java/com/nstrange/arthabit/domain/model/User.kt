package com.nstrange.arthabit.domain.model

data class User(
    val userId: String,
    val firstName: String,
    val lastName: String,
    val phoneNumber: Long,
    val email: String,
    val profilePic: String?
) {
    val fullName: String get() = "$firstName $lastName"

    val formattedPhone: String
        get() {
            val digits = phoneNumber.toString()
            return if (digits.length == 10) {
                "(${digits.substring(0, 3)}) ${digits.substring(3, 6)}-${digits.substring(6)}"
            } else {
                digits
            }
        }
}

