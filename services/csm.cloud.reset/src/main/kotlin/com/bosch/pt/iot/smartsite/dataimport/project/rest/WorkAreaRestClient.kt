/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.project.rest

import com.bosch.pt.iot.smartsite.dataimport.project.api.resource.request.CreateWorkAreaResource
import com.bosch.pt.iot.smartsite.dataimport.project.api.resource.response.WorkAreaListResource
import com.google.common.net.HttpHeaders.IF_MATCH
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface WorkAreaRestClient {

  @POST("v5/projects/workareas")
  fun create(
      @Header(IF_MATCH) etag: String,
      @Body createWorkAreaResource: CreateWorkAreaResource
  ): Call<WorkAreaListResource>
}
