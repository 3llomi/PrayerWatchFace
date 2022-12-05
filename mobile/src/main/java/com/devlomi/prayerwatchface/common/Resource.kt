package com.devlomi.prayerwatchface.common

data class Resource<out T>(val status: Status, val data: T?, val message: String?) {

    companion object {

        fun <T> success(data: T?): Resource<T> {
            return Resource(Status.SUCCESS, data, null)
        }

        fun <T> error(msg: String, data: T? = null): Resource<T> {
            return Resource(Status.ERROR, data, msg)
        }

        fun <T> loading(data: T? = null): Resource<T> {
            return Resource(Status.LOADING, data, null)
        }

        fun <T> initial(): Resource<T> {
            return Resource(Status.INITIAL, null, null)
        }


    }

}

enum class Status {
    INITIAL,
    SUCCESS,
    ERROR,
    LOADING;


}

fun Status.isLoading() = this == Status.LOADING