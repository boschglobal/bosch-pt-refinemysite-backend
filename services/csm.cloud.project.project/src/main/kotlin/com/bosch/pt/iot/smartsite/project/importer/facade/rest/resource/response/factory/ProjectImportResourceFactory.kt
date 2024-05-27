/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.importer.facade.rest.resource.response.factory

import com.bosch.pt.csm.cloud.common.facade.rest.CustomLinkBuilderFactory
import com.bosch.pt.iot.smartsite.project.importer.boundary.assembler.ColumnUtils.shouldBeSkipped
import com.bosch.pt.iot.smartsite.project.importer.boundary.dto.AnalysisResult
import com.bosch.pt.iot.smartsite.project.importer.boundary.dto.UploadResult
import com.bosch.pt.iot.smartsite.project.importer.facade.rest.ProjectImportController.Companion.IMPORT_BY_PROJECT_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.importer.facade.rest.ProjectImportController.Companion.PATH_VARIABLE_PROJECT_ID
import com.bosch.pt.iot.smartsite.project.importer.facade.rest.resource.response.ProjectImportAnalysisResource
import com.bosch.pt.iot.smartsite.project.importer.facade.rest.resource.response.ProjectImportAnalysisResource.Companion.LINK_IMPORT
import com.bosch.pt.iot.smartsite.project.importer.facade.rest.resource.response.ProjectImportColumnResource
import com.bosch.pt.iot.smartsite.project.importer.facade.rest.resource.response.ProjectImportUploadResource
import com.bosch.pt.iot.smartsite.project.importer.facade.rest.resource.response.ProjectImportValidationResult
import com.bosch.pt.iot.smartsite.project.importer.facade.rest.resource.response.factory.dto.TranslatedValidationResultDto
import com.bosch.pt.iot.smartsite.project.importer.facade.rest.resource.response.factory.dto.ValidationResultTypeDto
import com.bosch.pt.iot.smartsite.project.importer.validation.ValidationResultType.ERROR
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import net.sf.mpxj.FieldType
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.stereotype.Component

@Component
open class ProjectImportResourceFactory(
    private val messageSource: MessageSource,
    private val linkFactory: CustomLinkBuilderFactory
) {

  open fun build(
      projectIdentifier: ProjectId,
      analysisResult: AnalysisResult
  ): ProjectImportAnalysisResource {
    val locale = LocaleContextHolder.getLocale()

    val translatedValidationResults =
        analysisResult.validationResults
            .map {
              TranslatedValidationResultDto(
                  type = it.type,
                  summary = messageSource.getMessage(it.messageKey, it.messageArguments, locale),
                  element = it.element)
            }
            .distinct()

    return ProjectImportAnalysisResource(
            projectIdentifier,
            analysisResult.version,
            translatedValidationResults
                .groupBy { ValidationResultTypeDto(it.type, it.summary) }
                .map {
                  ProjectImportValidationResult(
                      type = it.key.type,
                      summary = it.key.summary,
                      elements = it.value.mapNotNull { it.element }.distinct().sorted())
                }
                .sortedWith(compareBy({ it.type }, { it.summary })),
            analysisResult.statistics)
        .apply {
          addIf(translatedValidationResults.firstOrNull { it.type == ERROR } == null) {
            linkFactory
                .linkTo(IMPORT_BY_PROJECT_ID_ENDPOINT)
                .withParameters(mapOf(PATH_VARIABLE_PROJECT_ID to projectIdentifier))
                .withRel(LINK_IMPORT)
          }
        }
  }

  open fun build(
      projectIdentifier: ProjectId,
      uploadResult: UploadResult
  ): ProjectImportUploadResource =
      ProjectImportUploadResource(
          projectIdentifier,
          uploadResult.version,
          uploadResult.columns
              .filter { it.fieldType == null || !shouldBeSkipped(it.fieldType) }
              .map {
                ProjectImportColumnResource(
                    displayName(it.name, it.fieldType),
                    it.columnType,
                    // If P6 -> Write the name into the field type to make
                    // it easier for the client to just copy the column- and
                    // field-type into the analysis request.
                    if (it.fieldType == null) it.name else it.fieldType.name())
              })

  private fun displayName(name: String, fieldType: FieldType?) =
      if (fieldType == null || name.lowercase() == fieldType.name.lowercase()) {
        name
      } else {
        "$name (${fieldType.name.lowercase()})"
      }
}
