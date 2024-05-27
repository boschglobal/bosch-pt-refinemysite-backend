/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.exporter.boundary

import com.bosch.pt.csm.cloud.common.extensions.toLocalDateByMillis
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.get
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleAggregateAvro
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2
import com.bosch.pt.iot.smartsite.project.importer.boundary.ImportContext
import com.bosch.pt.iot.smartsite.project.importer.control.ProjectReader
import com.bosch.pt.iot.smartsite.project.importer.model.DataImportObject
import com.bosch.pt.iot.smartsite.project.importer.model.ImportModel
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project
import java.time.LocalDateTime
import java.util.UUID
import net.sf.mpxj.ProjectCalendarDays.DEFAULT_WORKING_AFTERNOON
import net.sf.mpxj.ProjectCalendarDays.DEFAULT_WORKING_MORNING
import net.sf.mpxj.ProjectFile
import net.sf.mpxj.reader.UniversalProjectReader
import org.assertj.core.api.Assertions.assertThat
import org.springframework.beans.factory.annotation.Autowired

@Suppress("UnnecessaryAbstractClass")
abstract class AbstractExportIntegrationTest : AbstractIntegrationTestV2() {

  @Autowired private lateinit var projectReader: ProjectReader

  fun readExportedFile(
      exportedFile: ByteArray,
      project: Project,
      craftColumnName: String? = null
  ): ImportModel {
    val projectFile = parseExportedFile(exportedFile)
    return readExportedFile(projectFile, project, craftColumnName)
  }

  fun readExportedFile(
      projectFile: ProjectFile,
      project: Project,
      craftColumnName: String? = null
  ): ImportModel {
    val importContext = ImportContext(mutableMapOf(), mutableMapOf())
    return projectReader.read(
        project, projectFile, importContext, true, craftColumnName, null, null, null)
  }

  fun parseExportedFile(exportedFile: ByteArray): ProjectFile =
      UniversalProjectReader().read(exportedFile.inputStream())

  // Validate additional data from the imported file to see that this constellation of data in the
  // exported-file looks as expected
  fun assertContainsOnlyRmsPlaceholderCraft(importModel: ImportModel) {
    assertThat(importModel.crafts).hasSize(1)
    assertThat(importModel.crafts[0].name).isEqualTo("RmS-Placeholder")
  }

  fun getEventStartDate(reference: String): LocalDateTime? =
      get<TaskScheduleAggregateAvro>(reference)
          ?.start
          ?.toLocalDateByMillis()
          ?.atStartOfDay()
          ?.plusHours(DEFAULT_WORKING_MORNING.start.hour.toLong())

  fun getEventEndDate(reference: String): LocalDateTime? =
      get<TaskScheduleAggregateAvro>(reference)
          ?.end
          ?.toLocalDateByMillis()
          ?.atStartOfDay()
          ?.plusHours(DEFAULT_WORKING_AFTERNOON.end.hour.toLong())

  fun <T> validateExportedIds(
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

  fun <T> validateExportedIds(
      dio: DataImportObject<T>,
      expectedAggregateRef: String,
      expectedUniqueId: Int,
      expectedFileId: Int
  ) {
    validateExportedIds(
        dio,
        EventStreamGeneratorStaticExtensions.getIdentifier(expectedAggregateRef),
        expectedUniqueId,
        expectedFileId)
  }
}
