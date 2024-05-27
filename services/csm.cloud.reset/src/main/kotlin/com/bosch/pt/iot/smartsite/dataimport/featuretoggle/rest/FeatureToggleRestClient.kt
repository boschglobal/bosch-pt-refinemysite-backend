/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.dataimport.featuretoggle.rest

import com.bosch.pt.iot.smartsite.dataimport.featuretoggle.api.resource.request.CreateFeatureCommand
import com.bosch.pt.iot.smartsite.dataimport.featuretoggle.api.resource.response.FeatureToggleListResource
import com.bosch.pt.iot.smartsite.dataimport.featuretoggle.api.resource.response.FeatureToggleResource
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface FeatureToggleRestClient {

  @GET("v1/features") fun existingFeatureToggles(): Call<FeatureToggleListResource>

  @POST("v1/features") fun create(@Body createFeatureCommand: CreateFeatureCommand): Call<FeatureToggleResource>

  @POST("v1/features/{name}/enable") fun enableFeature(@Path("name") featureName: String): Call<FeatureToggleResource>
}
