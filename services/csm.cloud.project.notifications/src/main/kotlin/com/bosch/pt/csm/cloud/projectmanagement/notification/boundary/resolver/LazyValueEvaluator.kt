/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.notification.boundary.resolver

import com.bosch.pt.csm.cloud.projectmanagement.notification.model.LazyValue
import java.util.UUID

interface LazyValueEvaluator {
  val type: String
  fun evaluate(projectIdentifier: UUID, value: LazyValue): String
}
