/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.importer.model

import com.bosch.pt.iot.smartsite.common.i18n.Key.IMPORT_VALIDATION_HOLIDAYS_MAX_AMOUNT_EXCEEDED
import com.bosch.pt.iot.smartsite.common.i18n.Key.IMPORT_VALIDATION_RELATION_TYPE_UNSUPPORTED
import com.bosch.pt.iot.smartsite.common.i18n.Key.IMPORT_VALIDATION_RELATION_UNSUPPORTED
import com.bosch.pt.iot.smartsite.common.i18n.Key.IMPORT_VALIDATION_WORKDAY_CONFIGURATION_HAS_WORK_ON_NON_WORKDAY
import com.bosch.pt.iot.smartsite.common.i18n.Key.IMPORT_VALIDATION_WORKING_AREAS_MAX_AMOUNT_EXCEEDED
import com.bosch.pt.iot.smartsite.project.importer.boundary.ImportContext
import com.bosch.pt.iot.smartsite.project.importer.model.dio.CraftDio
import com.bosch.pt.iot.smartsite.project.importer.model.dio.MilestoneDio
import com.bosch.pt.iot.smartsite.project.importer.model.dio.RelationDio
import com.bosch.pt.iot.smartsite.project.importer.model.dio.TaskDio
import com.bosch.pt.iot.smartsite.project.importer.model.dio.TaskScheduleDio
import com.bosch.pt.iot.smartsite.project.importer.model.dio.WorkAreaDio
import com.bosch.pt.iot.smartsite.project.importer.model.identifier.ProjectIdentifier
import com.bosch.pt.iot.smartsite.project.importer.validation.ValidationResult
import com.bosch.pt.iot.smartsite.project.importer.validation.ValidationResultType
import com.bosch.pt.iot.smartsite.project.workarea.shared.model.WorkArea.Companion.MAX_WORKAREA_POSITION_VALUE
import com.bosch.pt.iot.smartsite.project.workday.shared.model.Holiday
import com.bosch.pt.iot.smartsite.project.workday.shared.model.Holiday.Companion.MAX_HOLIDAY_AMOUNT
import java.time.DayOfWeek
import net.sf.mpxj.ProjectFile
import net.sf.mpxj.RelationType
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder.getLocale

data class ImportModel(
    val projectIdentifier: ProjectIdentifier,
    val workAreas: List<WorkAreaDio>,
    val crafts: List<CraftDio>,
    val tasks: List<TaskDio>,
    val taskSchedules: List<TaskScheduleDio>,
    val milestones: List<MilestoneDio>,
    val relations: List<RelationDio>,
    val workdays: List<DayOfWeek>,
    val holidays: List<Holiday>,
    val hasWorkOnNonWorkDays: Boolean
) {

  fun validate(
      projectFile: ProjectFile,
      context: ImportContext,
      messageSource: MessageSource
  ): List<ValidationResult> {
    val validationErrors = mutableListOf<ValidationResult>()
    validationErrors.addAll(workAreas.map { it.validate(context) }.flatten())
    validationErrors.addAll(crafts.map { it.validate(context) }.flatten())
    validationErrors.addAll(tasks.map { it.validate(context) }.flatten())
    validationErrors.addAll(taskSchedules.map { it.validate(context) }.flatten())
    validationErrors.addAll(milestones.map { it.validate(context) }.flatten())
    validationErrors.addAll(relations.map { it.validate(context) }.flatten())

    validationErrors.addAll(unsupportedRelationTypes(projectFile, messageSource))
    validationErrors.addAll(unsupportedRelations(projectFile))

    if (workAreas.size > MAX_WORKAREA_POSITION_VALUE) {
      validationErrors.add(
          ValidationResult(
              ValidationResultType.ERROR,
              null,
              IMPORT_VALIDATION_WORKING_AREAS_MAX_AMOUNT_EXCEEDED,
              (MAX_WORKAREA_POSITION_VALUE + 1).toString()))
    }

    if (hasWorkOnNonWorkDays) {
      validationErrors.add(
          ValidationResult(
              ValidationResultType.INFO,
              null,
              IMPORT_VALIDATION_WORKDAY_CONFIGURATION_HAS_WORK_ON_NON_WORKDAY))
    }

    // no need to check for duplicates of holidays and workdays as they cannot occur here ..
    if (holidays.size > MAX_HOLIDAY_AMOUNT) {
      validationErrors.add(
          ValidationResult(
              ValidationResultType.ERROR,
              null,
              IMPORT_VALIDATION_HOLIDAYS_MAX_AMOUNT_EXCEEDED,
              "$MAX_HOLIDAY_AMOUNT"))
    }

    return validationErrors
  }

  private fun unsupportedRelationTypes(
      projectFile: ProjectFile,
      messageSource: MessageSource
  ): List<ValidationResult> =
      projectFile.tasks
          .filter { it.successors.isNotEmpty() }
          .mapNotNull { task -> task.successors.filter { it.type != RelationType.FINISH_START } }
          .flatten()
          .map { relation ->
            ValidationResult(
                type = ValidationResultType.INFO,
                element =
                    messageSource.getMessage(
                        "Import_Validation_RelationType_${relation.type}",
                        arrayOf(relation.sourceTask.name, relation.targetTask.name),
                        getLocale()),
                IMPORT_VALIDATION_RELATION_TYPE_UNSUPPORTED)
          }

  private fun unsupportedRelations(projectFile: ProjectFile): List<ValidationResult> {
    val taskIds = tasks.map { it.id.id }
    val milestoneIds = milestones.map { it.id.id }
    val validRelationIds = taskIds + milestoneIds
    return projectFile.tasks
        .filter { it.successors.isNotEmpty() }
        .mapNotNull { task ->
          task.successors.mapNotNull {
            val sourceId = it.sourceTask.uniqueID
            val targetId = it.targetTask.uniqueID
            if (!validRelationIds.contains(sourceId) || !validRelationIds.contains(targetId)) {
              ValidationResult(
                  ValidationResultType.INFO,
                  "${it.sourceTask.name} → ${it.targetTask.name}",
                  IMPORT_VALIDATION_RELATION_UNSUPPORTED)
            } else null
          }
        }
        .flatten()
  }
}
