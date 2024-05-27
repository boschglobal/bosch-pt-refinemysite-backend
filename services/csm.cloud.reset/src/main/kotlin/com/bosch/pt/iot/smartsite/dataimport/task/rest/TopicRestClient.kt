/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.task.rest

import com.bosch.pt.iot.smartsite.dataimport.task.api.resource.request.CreateTopicResource
import com.bosch.pt.iot.smartsite.dataimport.task.api.resource.response.TopicResource
import java.util.UUID
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface TopicRestClient {

  @POST("v5/projects/tasks/{taskId}/topics")
  fun create(
      @Path("taskId") taskId: UUID,
      @Body createTopicResource: CreateTopicResource
  ): Call<TopicResource>
}
