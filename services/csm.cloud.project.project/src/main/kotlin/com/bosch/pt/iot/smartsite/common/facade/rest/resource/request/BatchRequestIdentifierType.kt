/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.common.facade.rest.resource.request

/**
 * When introducing a new batch endpoint using [BatchRequestResource] for a new resource, add an
 * identifier type for this new resource. Batch endpoints should accept the resource itself as id
 * type by default, even when it is not supported.
 */
object BatchRequestIdentifierType {
  const val DAYCARD = "DAYCARD"
  const val MESSAGE = "MESSAGE"
  const val TASK = "TASK"
  const val TASKATTACHMENT = "TASKATTACHMENT"
  const val TOPIC = "TOPIC"
  const val TASK_SCHEDULE = "TASKSCHEDULE"
}
