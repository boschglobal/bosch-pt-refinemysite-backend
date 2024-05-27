/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.project.facade.rest.resource.response.factory

import com.bosch.pt.csm.cloud.common.facade.rest.CustomLinkBuilderFactory
import com.bosch.pt.iot.smartsite.featuretoggle.query.FeatureEnum.CALENDAR_CUSTOM_SORT
import com.bosch.pt.iot.smartsite.featuretoggle.query.FeatureQueryService
import com.bosch.pt.iot.smartsite.project.copy.boundary.ProjectCopyService
import com.bosch.pt.iot.smartsite.project.copy.facade.rest.ProjectCopyController.Companion.COPY_BY_PROJECT_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.copy.facade.rest.ProjectCopyController.Companion.PATH_VARIABLE_PROJECT_ID as COPY_PATH_VARIABLE_PROJECT_ID
import com.bosch.pt.iot.smartsite.project.exporter.boundary.ProjectExportService
import com.bosch.pt.iot.smartsite.project.exporter.facade.rest.ProjectExportController.Companion.EXPORT_BY_PROJECT_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.exporter.facade.rest.ProjectExportController.Companion.PATH_VARIABLE_PROJECT_ID as EXPORT_PATH_VARIABLE_PROJECT_ID
import com.bosch.pt.iot.smartsite.project.importer.boundary.ProjectImportService
import com.bosch.pt.iot.smartsite.project.importer.facade.rest.ProjectImportController.Companion.IMPORT_BY_PROJECT_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.importer.facade.rest.ProjectImportController.Companion.PATH_VARIABLE_PROJECT_ID as IMPORT_PATH_VARIABLE_PROJECT_ID
import com.bosch.pt.iot.smartsite.project.project.authorization.ProjectAuthorizationComponent
import com.bosch.pt.iot.smartsite.project.project.facade.rest.ProjectController.Companion.PATH_VARIABLE_PROJECT_ID
import com.bosch.pt.iot.smartsite.project.project.facade.rest.ProjectController.Companion.PROJECT_BY_PROJECT_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.project.facade.rest.resource.response.ProjectResource
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Transactional(readOnly = true)
@Component
open class ProjectResourceFactory(
    private val featureQueryService: FeatureQueryService,
    private val projectResourceFactoryHelper: ProjectResourceFactoryHelper,
    private val projectExportService: ProjectExportService,
    private val projectImportService: ProjectImportService,
    private val projectCopyService: ProjectCopyService,
    private val projectAuthorizationComponent: ProjectAuthorizationComponent,
    private val linkFactory: CustomLinkBuilderFactory
) {

  open fun build(project: Project): ProjectResource =
      projectResourceFactoryHelper.build(listOf(project), true).first().also {
        if (projectImportService.isImportPossible(project)) {
          it.add(
              linkFactory
                  .linkTo(IMPORT_BY_PROJECT_ID_ENDPOINT)
                  .withParameters(mapOf(IMPORT_PATH_VARIABLE_PROJECT_ID to project.identifier))
                  .withRel(LINK_IMPORT))
        }
        if (projectExportService.isExportPossible(project)) {
          it.add(
              linkFactory
                  .linkTo(EXPORT_BY_PROJECT_ID_ENDPOINT)
                  .withParameters(mapOf(EXPORT_PATH_VARIABLE_PROJECT_ID to project.identifier))
                  .withRel(LINK_EXPORT))
        }
        if (projectCopyService.isCopyPossible(project) &&
            projectAuthorizationComponent.hasCopyPermissionOnProject(project.identifier)) {
          it.add(
              linkFactory
                  .linkTo(COPY_BY_PROJECT_ID_ENDPOINT)
                  .withParameters(mapOf(COPY_PATH_VARIABLE_PROJECT_ID to project.identifier))
                  .withRel(LINK_COPY))
        }
        if (featureQueryService.isFeatureEnabled(CALENDAR_CUSTOM_SORT, project.identifier)) {
          it.add(
              linkFactory
                  // there is no dedicated backend endpoint for custom calendar sorting. Therefore,
                  // we use the project endpoint instead, but this is arbitrary.
                  .linkTo(PROJECT_BY_PROJECT_ID_ENDPOINT)
                  .withParameters(mapOf(PATH_VARIABLE_PROJECT_ID to project.identifier))
                  .withRel(LINK_CALENDAR_CUSTOM_SORT))
        }
      }

  companion object {
    const val LINK_CALENDAR_CUSTOM_SORT = "calendarCustomSort"
    const val LINK_COPY = "copy"
    const val LINK_EXPORT = "export"
    const val LINK_IMPORT = "import"
  }
}
