/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.importer.boundary

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2
import com.bosch.pt.iot.smartsite.project.external.model.ExternalId
import com.bosch.pt.iot.smartsite.project.external.model.ExternalIdType
import com.bosch.pt.iot.smartsite.project.importer.control.ProjectReader
import com.bosch.pt.iot.smartsite.project.importer.model.DataImportObject
import com.bosch.pt.iot.smartsite.project.importer.model.ImportModel
import com.bosch.pt.iot.smartsite.project.project.facade.rest.resource.request.SaveProjectResource
import com.bosch.pt.iot.smartsite.project.project.facade.rest.resource.response.ProjectAddressDto
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project
import com.bosch.pt.iot.smartsite.project.project.shared.model.ProjectCategoryEnum
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID
import net.sf.mpxj.ProjectFile
import net.sf.mpxj.reader.UniversalProjectReader
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.Resource

@Suppress("UnnecessaryAbstractClass")
abstract class AbstractImportIntegrationTest : AbstractIntegrationTestV2() {

  @Autowired protected lateinit var projectReader: ProjectReader

  fun readProject(file: Resource): ProjectFile = UniversalProjectReader().read(file.inputStream)

  fun readFile(
      projectFile: ProjectFile,
      project: Project,
      craftColumnName: String? = null
  ): ImportModel {
    val importContext = ImportContext(mutableMapOf(), mutableMapOf())
    return projectReader.read(
        project, projectFile, importContext, true, craftColumnName, null, null, null)
  }

  fun getImportIds(project: Project, projectFile: ProjectFile): List<ExternalId> {
    val idType =
        when (projectFile.projectProperties.fileType) {
          SupportedFileTypes.MSPDI.name,
          SupportedFileTypes.MPP.name -> ExternalIdType.MS_PROJECT
          SupportedFileTypes.PMXML.name,
          SupportedFileTypes.XER.name -> ExternalIdType.P6
          SupportedFileTypes.PP.name -> ExternalIdType.PP
          else -> error("Unsupported file type: ${projectFile.projectProperties.fileType}")
        }

    return repositories
        .findExternalIds(project.identifier, idType)
        .sortedWith(compareBy({ it.fileId }, { it.fileUniqueId }))
  }

  fun <T> validateImportIds(
      dio: DataImportObject<T>,
      expectedGuid: UUID,
      expectedUniqueId: Int,
      expectedFileId: Int
  ) {
    require(dio.guid == expectedGuid) {
      "Expected guid to be \"$expectedGuid”, but was \"${dio.guid}\""
    }
    require(dio.uniqueId == expectedUniqueId) {
      "Expected uniqueId to be \"$expectedUniqueId”, but was \"${dio.uniqueId}\""
    }
    require(dio.fileId == expectedFileId) {
      "Expected fileId to be \"$expectedFileId”, but was \"${dio.fileId}\""
    }
  }

  fun <T> validateImportIds(
      dio: DataImportObject<T>,
      expectedAggregateRef: String,
      expectedUniqueId: Int,
      expectedFileId: Int
  ) {
    validateImportIds(
        dio,
        EventStreamGeneratorStaticExtensions.getIdentifier(expectedAggregateRef),
        expectedUniqueId,
        expectedFileId)
  }

  fun createDefaultSaveProjectResource() =
      SaveProjectResource(
          client = "client",
          description = "description",
          start = LocalDate.now(),
          end = LocalDate.now().plus(1, ChronoUnit.DAYS),
          projectNumber = "projectNumber",
          title = "p2",
          category = ProjectCategoryEnum.OB,
          address = ProjectAddressDto("city", "HN", "street", "ZC"))
}
