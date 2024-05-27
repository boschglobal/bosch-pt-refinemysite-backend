/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.task.rest

import com.bosch.pt.iot.smartsite.dataimport.task.api.resource.request.AssignTaskResource
import com.bosch.pt.iot.smartsite.dataimport.task.api.resource.request.CreateTaskResource
import com.bosch.pt.iot.smartsite.dataimport.task.api.resource.request.CreateTaskScheduleResource
import com.bosch.pt.iot.smartsite.dataimport.task.api.resource.response.TaskResource
import com.bosch.pt.iot.smartsite.dataimport.task.api.resource.response.TaskScheduleResource
import java.util.UUID
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface TaskRestClient {

  @POST("v5/projects/tasks/{taskId}")
  fun create(
      @Path("taskId") taskId: UUID,
      @Body createTaskResource: CreateTaskResource
  ): Call<TaskResource>

  @POST("v5/projects/tasks/{taskId}/assign")
  fun assign(
      @Path("taskId") taskId: UUID,
      @Body assignTaskResource: AssignTaskResource
  ): Call<TaskResource>

  @POST("v5/projects/tasks/{taskId}/start")
  fun start(@Path("taskId") taskId: UUID): Call<TaskResource>

  @POST("v5/projects/tasks/{taskId}/close")
  fun close(@Path("taskId") taskId: UUID): Call<TaskResource>

  @POST("v5/projects/tasks/{taskId}/schedule")
  fun createSchedule(
      @Path("taskId") taskId: UUID,
      @Body createTaskScheduleResource: CreateTaskScheduleResource
  ): Call<TaskScheduleResource>
}
