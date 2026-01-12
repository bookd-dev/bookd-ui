package com.bookd.app.data.api

import de.jensklingenberg.ktorfit.http.GET

interface HealthApi {
    
    @GET("api/health")
    suspend fun checkHealth(): String
}
