package com.bookd.app.data.api

import com.bookd.app.data.model.LoginRequest
import com.bookd.app.data.model.LoginResponse
import com.bookd.app.data.model.RegisterRequest
import com.bookd.app.data.model.User
import de.jensklingenberg.ktorfit.http.Body
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.POST

interface AuthApi {
    
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse
    
    @POST("api/auth/logout")
    suspend fun logout()
    
    @POST("api/auth/register/guest")
    suspend fun registerGuest(@Body request: RegisterRequest): User
    
    @POST("api/auth/register/user")
    suspend fun registerWithInvite(@Body request: RegisterRequest): User
    
    @GET("api/auth/me")
    suspend fun getCurrentUser(): User
}
