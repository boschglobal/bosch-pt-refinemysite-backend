/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.rfv.boundary.resolver

import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.RFVCUSTOMIZATION
import com.bosch.pt.csm.cloud.projectmanagement.notification.boundary.resolver.LazyValueEvaluator
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.LazyValue
import com.bosch.pt.csm.cloud.projectmanagement.project.daycard.model.DayCardReasonEnum
import com.bosch.pt.csm.cloud.projectmanagement.project.rfv.boundary.RfvCustomizationService
import java.util.UUID
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.stereotype.Component

@Component
class LazyRfvNameResolver(
    private val rfvCustomizationService: RfvCustomizationService,
    private val messageSource: MessageSource
) : LazyValueEvaluator {

  override val type: String = RFVCUSTOMIZATION.name

  override fun evaluate(projectIdentifier: UUID, value: LazyValue): String {
    @Suppress("UNCHECKED_CAST") val input = value.value as String

    val reason = DayCardReasonEnum.valueOf(input)

    return rfvCustomizationService.findLatestCachedByProjectIdentifierAndReason(
            projectIdentifier, reason)
        ?.name
        ?: translateReason(reason)
  }

  private fun translateReason(value: DayCardReasonEnum): String =
      messageSource.getMessage(
          "DayCardReasonEnum_" + value.name, arrayOf<Any>(), LocaleContextHolder.getLocale())
}
