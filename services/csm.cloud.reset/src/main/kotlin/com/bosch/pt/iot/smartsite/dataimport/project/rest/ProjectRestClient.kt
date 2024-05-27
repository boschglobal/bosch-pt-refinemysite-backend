/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.project.rest

import com.bosch.pt.iot.smartsite.dataimport.project.api.resource.request.CreateProjectResource
import com.bosch.pt.iot.smartsite.dataimport.project.api.resource.response.ProjectResource
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface ProjectRestClient {

  @POST("v5/projects")
  fun create(@Body createProjectResource: CreateProjectResource): Call<ProjectResource>
}
