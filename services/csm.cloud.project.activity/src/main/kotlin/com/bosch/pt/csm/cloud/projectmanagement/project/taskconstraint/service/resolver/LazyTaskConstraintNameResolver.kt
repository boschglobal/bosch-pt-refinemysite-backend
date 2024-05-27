/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.service.resolver

import com.bosch.pt.csm.cloud.projectmanagement.activity.model.LazyValue
import com.bosch.pt.csm.cloud.projectmanagement.common.aggregate.AggregateType
import com.bosch.pt.csm.cloud.projectmanagement.common.service.resolver.LazyValueEvaluator
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.model.TaskConstraintEnum
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.service.TaskConstraintCustomizationService
import java.util.UUID
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.stereotype.Component

@Component
class LazyTaskConstraintNameResolver(
    private val constraintCustomizationService: TaskConstraintCustomizationService,
    private val messageSource: MessageSource
) : LazyValueEvaluator {

  override val type: String = AggregateType.TASKCONSTRAINTCUSTOMIZATION.name

  override fun evaluate(projectIdentifier: UUID, value: LazyValue): String {
    @Suppress("UNCHECKED_CAST") val input = value.value as String

    val key = TaskConstraintEnum.valueOf(input)

    return if (key.isCustom) {
      constraintCustomizationService.findLatestCachedByProjectIdentifierAndKey(
              projectIdentifier, key)
          ?.name
          ?: translateReason(key)
    } else {
      translateReason(key)
    }
  }

  private fun translateReason(value: TaskConstraintEnum): String =
      messageSource.getMessage(
          "TaskConstraintEnum_" + value.name, arrayOf<Any>(), LocaleContextHolder.getLocale())
}
