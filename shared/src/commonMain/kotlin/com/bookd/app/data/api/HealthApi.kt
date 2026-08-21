package com.bookd.app.data.api

import com.bookd.app.data.model.HealthResponse
import de.jensklingenberg.ktorfit.http.GET

interface HealthApi {
    
    @GET("api/health")
    suspend fun checkHealth(): HealthResponse
}
