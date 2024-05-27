/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.statistics.boundary

import com.bosch.pt.csm.cloud.projectmanagement.rfv.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.rfv.message.getProjectIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.rfv.messages.RfvCustomizationEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.DayCardReasonNotDoneEnum
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.DayCardReasonVarianceEnum
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.RfvCustomization
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.RfvDto
import com.bosch.pt.csm.cloud.projectmanagement.statistics.repository.RfvCustomizationRepository
import datadog.trace.api.Trace
import java.util.UUID
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RfvCustomizationService(
    private val rfvCustomizationRepository: RfvCustomizationRepository,
    private val messageSource: MessageSource
) {

  @Trace
  @Transactional
  fun createOrUpdateRfvCustomizationFromEvent(event: RfvCustomizationEventAvro) {
    val aggregate = event.getAggregate()
    val identifier = aggregate.getIdentifier()
    val projectIdentifier = aggregate.getProjectIdentifier()

    val rfv = rfvCustomizationRepository.findByIdentifier(identifier)

    if (rfv == null) {
      rfvCustomizationRepository.save(
          RfvCustomization(
              identifier,
              projectIdentifier,
              DayCardReasonNotDoneEnum.valueOf(aggregate.getKey().name),
              aggregate.getActive(),
              aggregate.getName()))
    } else {
      rfvCustomizationRepository.save(
          rfv.apply {
            key = DayCardReasonNotDoneEnum.valueOf(aggregate.getKey().name)
            active = aggregate.getActive()
            name = aggregate.getName()
          })
    }
  }

  @Trace
  @Transactional
  fun deleteRfvCustomization(identifier: UUID) {
    val rfv = rfvCustomizationRepository.findByIdentifier(identifier)
    if (rfv != null) {
      rfvCustomizationRepository.delete(rfv)
    }
  }

  @Trace
  @Transactional(readOnly = true)
  fun resolveProjectRfvs(projectIdentifier: UUID): Map<DayCardReasonVarianceEnum, String> =
      findAll(projectIdentifier).associate {
        it.key to
            (it.name
                ?: messageSource.getMessage(
                    "DayCardReasonVarianceEnum_" + it.key.name,
                    null,
                    LocaleContextHolder.getLocale()))
      }

  private fun findAll(projectIdentifier: UUID): List<RfvDto> {
    val rfvs =
        rfvCustomizationRepository.findAllByProjectIdentifier(projectIdentifier).associateBy {
          DayCardReasonVarianceEnum.valueOf(it.key.name)
        }

    return DayCardReasonVarianceEnum.values().map {
      val customization = rfvs[it]
      when (customization == null) {
        true -> RfvDto(it, !it.isCustom())
        else -> RfvDto(it, customization.active, customization.name)
      }
    }
  }
}
