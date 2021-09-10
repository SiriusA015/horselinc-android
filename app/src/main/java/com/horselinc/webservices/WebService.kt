package com.horselinc.webservices

import io.reactivex.Observable
import org.json.JSONObject
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface WebService {

    @FormUrlEncoded
    @POST("/api/auth/login")
    fun signIn (@Field("email") email: String?,
                @Field("password") password: String?): Observable<SignInResponse>
}