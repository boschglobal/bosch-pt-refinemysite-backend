/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.project.rest

import com.bosch.pt.iot.smartsite.dataimport.project.api.resource.request.CreateProjectCraftResource
import com.bosch.pt.iot.smartsite.dataimport.project.api.resource.response.ProjectCraftListResource
import com.google.common.net.HttpHeaders.IF_MATCH
import java.util.UUID
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface ProjectCraftRestClient {

  @POST("v5/projects/{projectId}/crafts")
  fun create(
      @Header(IF_MATCH) etag: String,
      @Path("projectId") projectId: UUID,
      @Body createProjectCraftResource: CreateProjectCraftResource
  ): Call<ProjectCraftListResource>
}
