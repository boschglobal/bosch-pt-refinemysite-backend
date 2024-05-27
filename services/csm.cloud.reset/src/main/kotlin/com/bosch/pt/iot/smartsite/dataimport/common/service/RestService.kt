/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.common.service

import java.io.IOException
import java.util.function.Supplier
import retrofit2.Call

interface RestService {

  fun <U> call(callable: Supplier<Call<U>>): U? =
      try {
        val response = callable.get().execute()
        require(response.isSuccessful) {
          "Rest call was not successful. Http-Status: ${response.code()}" +
              ", Message: ${response.errorBody()!!.string()}"
        }
        response.body()
      } catch (e: IOException) {
        throw IllegalArgumentException(e)
      }
}
