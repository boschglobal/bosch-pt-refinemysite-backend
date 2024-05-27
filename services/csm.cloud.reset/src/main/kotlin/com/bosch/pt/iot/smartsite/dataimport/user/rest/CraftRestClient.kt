/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2020
 *
 *  *************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.user.rest

import com.bosch.pt.iot.smartsite.dataimport.user.api.resource.request.CreateCraftResource
import com.bosch.pt.iot.smartsite.dataimport.user.api.resource.response.CraftListResource
import com.bosch.pt.iot.smartsite.dataimport.user.api.resource.response.MultilingualCraftResource
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface CraftRestClient {

  @POST("v1/crafts")
  fun create(@Body createCraftResource: CreateCraftResource): Call<MultilingualCraftResource>

  @GET("v1/crafts") fun existingCrafts(@Query("page") page: Int): Call<CraftListResource>
}
