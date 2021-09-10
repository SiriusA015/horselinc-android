package com.horselinc.webservices

import com.google.gson.Gson
import com.horselinc.firebase.ResponseCallback
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory


object WebServiceAPI {
    private var service: WebService



    init {
        val retrofit = Retrofit.Builder()
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl("https://horse-linc.herokuapp.com")
            .build()
        service = retrofit.create(WebService::class.java)
    }

    private fun getErrorMessage(e: HttpException): String {
        e.response()?.errorBody()?.run {
            return try {
                Gson().fromJson<MessageResponse>(this.string(), MessageResponse::class.java).message
                    ?: "Unknown Error_1!"
            } catch (e: Exception) {
                e.printStackTrace()
                e.localizedMessage ?: "Unknown Error_2!"
            }

        } ?: return "Unknown Error_3!"
    }

    fun signIn (email: String, password: String, callback: ResponseCallback<String>): Disposable {
        return service.signIn(email, password)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { result ->
                    result?.apply {
                        if (token != null) {
                            callback.onSuccess(token!!)
                        } else {
                            callback.onFailure("Unknown ErrorResponse From Server_1")
                        }
                    } ?: callback.onFailure("Unknown ErrorResponse From Server_2")
                },
                { e ->
                    when (e) {
                        is HttpException -> callback.onFailure(getErrorMessage(e))
                        else -> callback.onFailure(e?.localizedMessage ?: "Unknown ErrorResponse From Server_3")
                    }
                }
            )
    }
}