/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.facade.rest.resource.response.assembler

import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.projectmanagement.common.utils.toLanguage
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.domain.asTaskConstraintId
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.facade.rest.resource.response.TaskConstraintResource
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.query.model.ProjectTaskConstraints
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.query.model.TaskConstraintEnum
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.query.model.TaskConstraintVersion
import java.time.LocalDateTime
import java.util.Locale
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.MessageSource
import org.springframework.stereotype.Component

@Component
class TaskConstraintResourceAssembler(
    private val messageSource: MessageSource,
    @Value("\${locale.supported}") private val supportedLocales: List<Locale>
) {

  fun assembleLatest(projectTaskConstraints: ProjectTaskConstraints): List<TaskConstraintResource> {
    val missingConstraints = getMissingTaskConstraints(projectTaskConstraints)

    val latestConstraints = mutableMapOf<String, LocalDateTime>()
    return (missingConstraints +
            projectTaskConstraints.constraints.flatMap { constraint ->
              constraint.history.last().let {
                // Find the latest constraint per type.
                // There can be multiple constraints of the same type with the same version
                // as they can be deleted and re-created.
                val currentLatest = latestConstraints[it.key.key]
                if (currentLatest != null && currentLatest.isAfter(it.eventDate)) {
                  return emptyList()
                }
                latestConstraints[it.key.key] = it.eventDate

                // Translate the constraint
                supportedLocales.map { locale ->
                  val translatedName = translate(it.key, it, locale)
                  TaskConstraintResourceMapper.INSTANCE.fromTaskConstraintVersion(
                      it,
                      projectTaskConstraints.projectId,
                      constraint.identifier,
                      locale.toLanguage(),
                      translatedName)
                }
              }
            })
        .filter {
          // Keep only the latest customized per type (or the un-customized)
          val latest = latestConstraints[it.key]
          if (latest == null) {
            true
          } else latest.toEpochMilli() == it.eventTimestamp
        }
  }

  fun assemble(projectTaskConstraints: ProjectTaskConstraints): List<TaskConstraintResource> {
    val missingConstraints = getMissingTaskConstraints(projectTaskConstraints)

    return missingConstraints +
        projectTaskConstraints.constraints.flatMap { constraint ->
          constraint.history.flatMap {
            supportedLocales.map { locale ->
              val translatedName = translate(it.key, it, locale)
              TaskConstraintResourceMapper.INSTANCE.fromTaskConstraintVersion(
                  it,
                  projectTaskConstraints.projectId,
                  constraint.identifier,
                  locale.toLanguage(),
                  translatedName)
            }
          }
        }
  }

  private fun getMissingTaskConstraints(
      projectTaskConstraints: ProjectTaskConstraints
  ): List<TaskConstraintResource> {
    val missingConstraints =
        TaskConstraintEnum.values().toSet() -
            projectTaskConstraints.constraints.map { it.key }.toSet()

    return missingConstraints.flatMap {
      supportedLocales.map { locale ->
        TaskConstraintResource(
            id = it.id.asTaskConstraintId(),
            version = -1L,
            project = projectTaskConstraints.projectId,
            key = it.key,
            active = !it.isCustom,
            language = locale.toLanguage(),
            name = translate(it, null, locale),
            deleted = false,
            eventTimestamp = it.timestamp)
      }
    }
  }

  private fun translate(
      key: TaskConstraintEnum,
      constraint: TaskConstraintVersion?,
      locale: Locale
  ): String = constraint?.name ?: messageSource.getMessage(key.messageKey, null, locale)
}
