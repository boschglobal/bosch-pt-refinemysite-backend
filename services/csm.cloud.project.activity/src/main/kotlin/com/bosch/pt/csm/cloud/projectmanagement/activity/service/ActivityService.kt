/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.activity.service

import com.bosch.pt.csm.cloud.common.model.AggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.Activity
import com.bosch.pt.csm.cloud.projectmanagement.activity.repository.ActivityRepository
import com.bosch.pt.csm.cloud.projectmanagement.common.exceptions.ResourceNotFoundException
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.ACTIVITY_VALIDATION_ERROR_NOT_FOUND
import datadog.trace.api.Trace
import java.util.UUID
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Slice
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Sort.Order.asc
import org.springframework.data.domain.Sort.Order.desc
import org.springframework.security.access.prepost.PostAuthorize
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service

@Service
class ActivityService(private val activityRepository: ActivityRepository) {

  @Trace fun saveAll(activities: Set<Activity>) = activities.forEach(this::save)

  @Trace
  @PreAuthorize("@taskActivityAuthorizationComponent.hasReadPermissionOnTask(#taskIdentifier)")
  fun findAll(taskIdentifier: UUID, beforeIdentifier: UUID?, pageLimit: Int): Slice<Activity> {
    val searchLimit = pageLimit.coerceAtMost(MAX_ACTIVITIES)
    val searchPage = PageRequest.of(0, searchLimit, SORT)

    return if (beforeIdentifier == null) {
      activityRepository.findAllByContextTask(taskIdentifier, searchPage)
    } else {
      val beforeActivity = activityRepository.findOneByIdentifier(beforeIdentifier)
      beforeActivity ?: throw ResourceNotFoundException(ACTIVITY_VALIDATION_ERROR_NOT_FOUND)

      activityRepository.findAllByContextTaskAndEventDateLessThan(
          taskIdentifier, beforeActivity.event.date, searchPage)
    }
  }

  @Trace
  fun deleteAllByContextTask(taskIdentifier: UUID) =
      activityRepository.deleteAllByContextTask(taskIdentifier)

  @Trace
  fun deleteAllByContextProject(projectIdentifier: UUID) =
      activityRepository.deleteAllByContextProject(projectIdentifier)

  @Trace
  private fun save(activity: Activity) {
    if (activityRepository.existsById(activity.aggregateIdentifier))
        resolveAlreadyExistingActivity(activity)
    else activityRepository.insert(activity)
  }

  @Trace
  @PostAuthorize(
      "@taskActivityAuthorizationComponent.hasReadPermissionOnTask(returnObject?.context?.task)")
  fun findActivityByAttachmentIdentifier(attachmentId: UUID): Activity? {
    return activityRepository.findActivityByAttachmentIdentifier(attachmentId)
  }

  @Trace
  fun findOne(aggregateIdentifier: AggregateIdentifier): Activity? =
      activityRepository.findActivityByAggregateIdentifier(aggregateIdentifier)

  fun activityExists(aggregateIdentifier: AggregateIdentifier): Boolean {
    val activity = findOne(aggregateIdentifier)
    return (activity != null)
  }

  /**
   * Upsert logic in case that the activity already exists due to a retry of message processing.
   * This should only happen in edge cases. Therefore, an additional read operation is acceptable
   * here
   */
  private fun resolveAlreadyExistingActivity(activity: Activity) {
    LOGGER.debug("Activity already exists. Updating instead of inserting now ...")

    val existingActivity =
        activityRepository.findById(activity.aggregateIdentifier).orElseThrow {
          IllegalStateException("Could not find a activity for failed insert operation.")
        }
    val updatedActivity = activity.copy(identifier = existingActivity.identifier)

    activityRepository.save(updatedActivity)
  }

  companion object {

    private val LOGGER = LoggerFactory.getLogger(ActivityService::class.java)

    private val SORT = Sort.by(desc("event.date"), asc("_id.type"), asc("_id.version"))

    const val MAX_ACTIVITIES = 50

    const val MAX_ACTIVITIES_STRING = MAX_ACTIVITIES.toString()
  }
}
