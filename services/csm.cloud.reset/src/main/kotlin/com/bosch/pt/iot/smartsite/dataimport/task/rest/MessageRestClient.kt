/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.task.rest

import com.bosch.pt.iot.smartsite.dataimport.task.api.resource.request.CreateMessageResource
import com.bosch.pt.iot.smartsite.dataimport.task.api.resource.response.MessageResource
import java.util.UUID
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface MessageRestClient {

  @POST("v5/projects/tasks/topics/{topicId}/messages")
  fun create(
      @Path("topicId") topicId: UUID,
      @Body createMessageResource: CreateMessageResource
  ): Call<MessageResource>
}
