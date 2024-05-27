/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.project.rest

import com.bosch.pt.iot.smartsite.dataimport.project.api.resource.request.AssignProjectParticipantResource
import com.bosch.pt.iot.smartsite.dataimport.project.api.resource.request.SearchParticipantResource
import com.bosch.pt.iot.smartsite.dataimport.project.api.resource.response.ParticipantListResource
import com.bosch.pt.iot.smartsite.dataimport.project.api.resource.response.ProjectParticipantResource
import java.util.UUID
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface ProjectParticipantRestClient {

  @POST("v5/projects/{projectId}/participants")
  fun assign(
      @Path("projectId") projectId: UUID,
      @Body assignProjectParticipantResource: AssignProjectParticipantResource
  ): Call<ProjectParticipantResource>

  @POST("v5/projects/{projectId}/participants/search")
  fun findParticipants(
      @Path("projectId") projectId: UUID,
      @Body searchParticipantResource: SearchParticipantResource
  ): Call<ParticipantListResource>
}
