/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.activity.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersion
import com.bosch.pt.csm.cloud.projectmanagement.activity.facade.rest.resources.response.ActivityListResource
import com.bosch.pt.csm.cloud.projectmanagement.activity.facade.rest.resources.response.factory.ActivityListResourceFactory
import com.bosch.pt.csm.cloud.projectmanagement.activity.service.ActivityService
import com.bosch.pt.csm.cloud.projectmanagement.activity.service.ActivityService.Companion.MAX_ACTIVITIES_STRING
import java.util.UUID
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@ApiVersion
@RestController
class ActivityController(
    private val activityListResourceFactory: ActivityListResourceFactory,
    private val activityService: ActivityService
) {

  @GetMapping(ACTIVITY_BY_TASK_ID_ENDPOINT)
  fun findTaskActivities(
      @PathVariable("taskId") taskId: UUID,
      @RequestParam("before", required = false) before: UUID?,
      @RequestParam("limit", required = false, defaultValue = MAX_ACTIVITIES_STRING) limit: Int
  ): ResponseEntity<ActivityListResource> {

    val activities = activityService.findAll(taskId, before, limit)

    return ResponseEntity.ok(activityListResourceFactory.build(activities, taskId, limit))
  }

  companion object {
    const val ACTIVITY_BY_TASK_ID_ENDPOINT = "/projects/tasks/{taskId}/activities"
  }
}
