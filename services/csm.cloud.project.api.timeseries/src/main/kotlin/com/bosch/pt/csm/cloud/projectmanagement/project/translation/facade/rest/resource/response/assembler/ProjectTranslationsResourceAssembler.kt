/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.translation.facade.rest.resource.response.assembler

import com.bosch.pt.csm.cloud.projectmanagement.common.utils.toLanguage
import com.bosch.pt.csm.cloud.projectmanagement.project.dayCard.query.model.DayCardStatusEnum
import com.bosch.pt.csm.cloud.projectmanagement.project.milestone.query.model.MilestoneTypeEnum
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.query.model.ParticipantRoleEnum
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.query.model.ParticipantStatusEnum
import com.bosch.pt.csm.cloud.projectmanagement.project.project.query.model.ProjectCategoryEnum
import com.bosch.pt.csm.cloud.projectmanagement.project.relation.query.model.RelationTypeEnum
import com.bosch.pt.csm.cloud.projectmanagement.project.task.query.model.TaskStatusEnum
import com.bosch.pt.csm.cloud.projectmanagement.project.topic.query.model.TopicCriticalityEnum
import com.bosch.pt.csm.cloud.projectmanagement.project.workday.query.model.DayEnum
import com.bosch.pt.csm.cloud.projectmanagement.translation.facade.rest.resource.response.TranslationResource
import com.bosch.pt.csm.cloud.projectmanagement.translation.shared.model.TranslatableEnum
import java.util.Locale
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.MessageSource
import org.springframework.stereotype.Component

@Component
class ProjectTranslationsResourceAssembler(
    private val messageSource: MessageSource,
    @Value("\${locale.supported}") private val supportedLocales: List<Locale>
) {

  fun assemble(): List<TranslationResource> =
      DayCardStatusEnum.values().flatMap(::translate) +
          MilestoneTypeEnum.values().flatMap(::translate) +
          ParticipantRoleEnum.values().flatMap(::translate) +
          ParticipantStatusEnum.values().flatMap(::translate) +
          ProjectCategoryEnum.values().flatMap(::translate) +
          RelationTypeEnum.values().flatMap(::translate) +
          TaskStatusEnum.values().flatMap(::translate) +
          TopicCriticalityEnum.values().flatMap(::translate) +
          DayEnum.values().flatMap(::translate)

  private fun translate(enumType: TranslatableEnum): List<TranslationResource> =
      supportedLocales.map { locale ->
        TranslationResource(
            enumType.key,
            locale.toLanguage(),
            messageSource.getMessage(enumType.messageKey, emptyArray(), locale))
      }
}
