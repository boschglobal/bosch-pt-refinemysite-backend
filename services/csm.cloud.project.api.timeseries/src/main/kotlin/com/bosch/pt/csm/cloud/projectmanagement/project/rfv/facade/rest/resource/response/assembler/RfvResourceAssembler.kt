/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.rfv.facade.rest.resource.response.assembler

import com.bosch.pt.csm.cloud.projectmanagement.common.utils.toLanguage
import com.bosch.pt.csm.cloud.projectmanagement.project.dayCard.query.model.DayCardReasonEnum
import com.bosch.pt.csm.cloud.projectmanagement.project.rfv.domain.asRfvId
import com.bosch.pt.csm.cloud.projectmanagement.project.rfv.facade.rest.resource.response.RfvResource
import com.bosch.pt.csm.cloud.projectmanagement.project.rfv.query.model.ProjectRfvs
import com.bosch.pt.csm.cloud.projectmanagement.project.rfv.query.model.RfvVersion
import java.util.Locale
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.MessageSource
import org.springframework.stereotype.Component

@Component
class RfvResourceAssembler(
    private val messageSource: MessageSource,
    @Value("\${locale.supported}") private val supportedLocales: List<Locale>
) {

  fun assembleLatest(projectRfvs: ProjectRfvs): List<RfvResource> {
    val missingRfvs = getMissingRfvs(projectRfvs)

    return missingRfvs +
        projectRfvs.rfvs.flatMap { rfv ->
          rfv.history.last().let {
            supportedLocales.map { locale ->
              val translatedName = translate(it.reason, it, locale)
              RfvResourceMapper.INSTANCE.fromRfvVersion(
                  it, projectRfvs.projectId, rfv.identifier, locale.toLanguage(), translatedName)
            }
          }
        }
  }

  fun assemble(projectRfvs: ProjectRfvs): List<RfvResource> {
    val missingRfvs = getMissingRfvs(projectRfvs)

    return missingRfvs +
        projectRfvs.rfvs.flatMap { rfv ->
          rfv.history.flatMap {
            supportedLocales.map { locale ->
              val translatedName = translate(it.reason, it, locale)
              RfvResourceMapper.INSTANCE.fromRfvVersion(
                  it, projectRfvs.projectId, rfv.identifier, locale.toLanguage(), translatedName)
            }
          }
        }
  }

  private fun getMissingRfvs(projectRfvs: ProjectRfvs): List<RfvResource> {
    val missingRfvs =
        DayCardReasonEnum.values().toSet() - projectRfvs.rfvs.map { it.reason }.toSet()

    return missingRfvs.flatMap {
      supportedLocales.map { locale ->
        RfvResource(
            id = it.id.asRfvId(),
            version = -1L,
            project = projectRfvs.projectId,
            reason = it.key,
            active = !it.isCustom,
            language = locale.toLanguage(),
            name = translate(it, null, locale),
            deleted = false,
            eventTimestamp = it.timestamp)
      }
    }
  }

  private fun translate(reason: DayCardReasonEnum, rfv: RfvVersion?, locale: Locale): String =
      rfv?.name ?: messageSource.getMessage(reason.messageKey, null, locale)
}
