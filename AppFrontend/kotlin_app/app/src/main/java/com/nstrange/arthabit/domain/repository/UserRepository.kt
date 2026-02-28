package com.nstrange.arthabit.domain.repository

import com.nstrange.arthabit.domain.model.User
import com.nstrange.arthabit.util.Resource

interface UserRepository {
    suspend fun getUser(userId: String): Resource<User>
}

