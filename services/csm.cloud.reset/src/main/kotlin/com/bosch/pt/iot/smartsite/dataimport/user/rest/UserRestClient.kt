/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.user.rest

import com.bosch.pt.iot.smartsite.dataimport.user.api.resource.request.CreateUserResource
import com.bosch.pt.iot.smartsite.dataimport.user.api.resource.response.UserAdministrationListResource
import com.bosch.pt.iot.smartsite.dataimport.user.api.resource.response.UserResource
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface UserRestClient {

  @POST("v3/users/current")
  fun create(@Body createUserResource: CreateUserResource): Call<UserResource>

  @GET("v3/users") fun existingUsers(@Query("page") page: Int): Call<UserAdministrationListResource>
}
