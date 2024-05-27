/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.activity.facade.rest.resources.response.factory

import com.bosch.pt.csm.cloud.common.ExcludeFromCodeCoverage
import com.bosch.pt.csm.cloud.common.facade.rest.CustomLinkBuilderFactory
import com.bosch.pt.csm.cloud.projectmanagement.activity.facade.rest.ActivityController
import com.bosch.pt.csm.cloud.projectmanagement.activity.facade.rest.resources.response.ActivityListResource
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.Activity
import java.util.UUID
import org.springframework.data.domain.Slice
import org.springframework.stereotype.Component

@Component
class ActivityListResourceFactory(
    private val activityResourceFactory: ActivityResourceFactory,
    private val linkFactory: CustomLinkBuilderFactory
) {

  fun build(
      activities: Slice<Activity>,
      taskIdentifier: UUID,
      pageLimit: Int
  ): ActivityListResource {

    val activityResources = activities.content.map(activityResourceFactory::build)
    val hasNext = activities.hasNext()
    val nextBeforeIdentifier = getLastIdentifier(activities)

    return ActivityListResource(activityResources).apply {
      // link to previous activities
      addIf(hasNext) {
        linkFactory
            .linkTo(ActivityController.ACTIVITY_BY_TASK_ID_ENDPOINT)
            .withParameters(
                mapOf(
                    "taskId" to taskIdentifier,
                ))
            .withQueryParameters(mapOf("before" to nextBeforeIdentifier!!, "limit" to pageLimit))
            .withRel(ActivityListResource.LINK_PREVIOUS)
      }
    }
  }
  @ExcludeFromCodeCoverage
  private fun getLastIdentifier(activities: Slice<Activity>): UUID? {
    return activities.content.lastOrNull()?.identifier
  }
}
