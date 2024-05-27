/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.project.rest

import com.bosch.pt.iot.smartsite.dataimport.project.api.resource.request.CreateMilestoneResource
import com.bosch.pt.iot.smartsite.dataimport.project.api.resource.response.MilestoneResource
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface MilestoneRestClient {

  @POST("/v5/projects/milestones")
  fun create(@Body createMilestoneResource: CreateMilestoneResource): Call<MilestoneResource>
}
