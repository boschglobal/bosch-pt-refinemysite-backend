/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.task.rest

import com.bosch.pt.iot.smartsite.dataimport.task.api.resource.request.CreateDayCardResource
import com.bosch.pt.iot.smartsite.dataimport.task.api.resource.response.CancelDayCardResource
import com.bosch.pt.iot.smartsite.dataimport.task.api.resource.response.DayCardResource
import com.bosch.pt.iot.smartsite.dataimport.task.api.resource.response.TaskScheduleResource
import java.util.UUID
import org.springframework.http.HttpHeaders.IF_MATCH
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface DayCardRestClient {

  @POST("v5/projects/tasks/{taskId}/schedule/daycards/{dayCardId}")
  fun create(
      @Path("taskId") taskId: UUID,
      @Path("dayCardId") dayCardId: UUID,
      @Header(IF_MATCH) etag: String?,
      @Body createDayCardResource: CreateDayCardResource
  ): Call<TaskScheduleResource>

  @POST("v5/projects/tasks/schedule/daycards/{dayCardId}/cancel")
  fun cancel(
      @Path("dayCardId") dayCardId: UUID,
      @Header(IF_MATCH) etag: String?,
      @Body reason: CancelDayCardResource
  ): Call<DayCardResource>

  @POST("v5/projects/tasks/schedule/daycards/{dayCardId}/complete")
  fun complete(
      @Path("dayCardId") dayCardId: UUID,
      @Header(IF_MATCH) etag: String?
  ): Call<DayCardResource>

  @POST("v5/projects/tasks/schedule/daycards/{dayCardId}/approve")
  fun approve(
      @Path("dayCardId") dayCardId: UUID,
      @Header(IF_MATCH) etag: String?
  ): Call<DayCardResource>
}
