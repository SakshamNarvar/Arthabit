package com.nstrange.arthabit.data.remote

import com.nstrange.arthabit.data.remote.dto.UserDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface UserApi {

    @GET("/user/v1/getUser")
    suspend fun getUser(
        @Query("userId") userId: String
    ): Response<UserDto>
}

