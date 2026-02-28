package com.nstrange.arthabit.data.repository

import com.nstrange.arthabit.data.remote.UserApi
import com.nstrange.arthabit.domain.model.User
import com.nstrange.arthabit.domain.repository.UserRepository
import com.nstrange.arthabit.util.Resource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userApi: UserApi
) : UserRepository {

    override suspend fun getUser(userId: String): Resource<User> {
        return try {
            val response = userApi.getUser(userId)
            if (response.isSuccessful) {
                val dto = response.body() ?: return Resource.Error("Empty response")
                Resource.Success(
                    User(
                        userId = dto.userId,
                        firstName = dto.firstName,
                        lastName = dto.lastName,
                        phoneNumber = dto.phoneNumber,
                        email = dto.email,
                        profilePic = dto.profilePic
                    )
                )
            } else {
                Resource.Error("Failed to fetch user: ${response.code()}")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error")
        }
    }
}

