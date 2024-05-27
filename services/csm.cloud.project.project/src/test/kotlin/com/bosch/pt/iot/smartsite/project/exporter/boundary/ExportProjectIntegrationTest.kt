/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.exporter.boundary

import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskSchedule
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkArea
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkAreaList
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.project.exporter.api.ProjectExportFormatEnum.MS_PROJECT_XML
import com.bosch.pt.iot.smartsite.project.exporter.api.ProjectExportFormatEnum.PRIMAVERA_P6_XML
import com.bosch.pt.iot.smartsite.project.exporter.api.ProjectExportParameters
import com.bosch.pt.iot.smartsite.project.importer.submitProjectImportFeatureToggle
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import java.time.LocalDate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@EnableAllKafkaListeners
class ExportProjectIntegrationTest : AbstractExportIntegrationTest() {

  @Autowired private lateinit var projectExportService: ProjectExportService

  private val projectIdentifier by lazy { getIdentifier("p2").asProjectId() }
  private val project by lazy { checkNotNull(repositories.findProject(projectIdentifier)) }

  @BeforeEach
  fun init() {
    eventStreamGenerator
        .setupDatasetTestData()
        .submitProjectImportFeatureToggle()
        .submitProject("p2")
        .submitProjectCraftG2("pc1") { it.name = "pc1" }
        .submitParticipantG3("p2Csm1") {
          it.user = getByReference("userCsm1")
          it.role = ParticipantRoleEnumAvro.CSM
        }
        .submitWorkArea(asReference = "w1") { it.name = "w1" }
        .submitWorkAreaList(asReference = "wal") {
          it.workAreas = listOf(getByReference("w1"))
          it.project = getByReference("p2")
        }
        .submitTask(asReference = "t1") {
          it.project = getByReference("p2")
          it.workarea = getByReference("w1")
          it.craft = getByReference("pc1")
          it.name = "t1"
          it.description = "description"
        }
        .submitProjectCraftG2("pc1") { it.name = "pc1" }
        .submitTask(asReference = "t2") {
          it.project = getByReference("p2")
          it.workarea = getByReference("w1")
          it.craft = getByReference("pc1")
          it.name = "t2"
        }
        .submitTaskSchedule("s1") {
          it.task = getByReference("t1")
          it.start = LocalDate.now().toEpochMilli()
          it.end = LocalDate.now().plusDays(1).toEpochMilli()
        }
        .submitTaskSchedule("s2") {
          it.task = getByReference("t2")
          it.start = LocalDate.now().toEpochMilli()
          it.end = LocalDate.now().plusDays(1).toEpochMilli()
        }
    setAuthentication("userCsm1")

    projectEventStoreUtils.reset()
  }

  @Test
  fun exportContainsRootNodeInMsProjectXml() {

    val exportedFile =
        projectExportService.export(
            project,
            ProjectExportParameters(
                format = MS_PROJECT_XML, includeMilestones = true, includeComments = false))

    verifyExportContainsRootNodeInMsProjectXml(exportedFile)
  }

  @Test
  fun reExportContainsRootNodeInMsProjectXml() {

    // First export
    exportContainsRootNodeInMsProjectXml()

    // Re-export
    val exportedFile =
        projectExportService.export(
            project,
            ProjectExportParameters(
                format = MS_PROJECT_XML, includeMilestones = true, includeComments = false))

    verifyExportContainsRootNodeInMsProjectXml(exportedFile)
  }

  private fun verifyExportContainsRootNodeInMsProjectXml(exportedFile: ByteArray) {
    val projectFile = parseExportedFile(exportedFile)
    val rootNode = projectFile.tasks[0]
    assertThat(rootNode.name).isEqualTo(project.title)
    assertThat(rootNode.id).isEqualTo(0)
    assertThat(rootNode.uniqueID).isEqualTo(0)
    assertThat(rootNode.wbs).isEqualTo("0")
    assertThat(rootNode.outlineLevel).isEqualTo(0)
    assertThat(rootNode.outlineNumber).isEqualTo("0")
  }

  @Test
  fun exportContainsNoRootNodeInP6Xml() {

    val exportedFile =
        projectExportService.export(
            project,
            ProjectExportParameters(
                format = PRIMAVERA_P6_XML, includeMilestones = true, includeComments = false))

    verifyExportContainsNoRootNodeInP6Xml(exportedFile)
  }

  @Test
  fun reExportContainsNoRootNodeInP6Xml() {

    // First export
    exportContainsNoRootNodeInP6Xml()

    // Re-export
    val exportedFile =
        projectExportService.export(
            project,
            ProjectExportParameters(
                format = PRIMAVERA_P6_XML, includeMilestones = true, includeComments = false))

    verifyExportContainsNoRootNodeInP6Xml(exportedFile)
  }

  private fun verifyExportContainsNoRootNodeInP6Xml(exportedFile: ByteArray) {
    val projectFile = parseExportedFile(exportedFile)
    val rootNode = projectFile.tasks[0]
    assertThat(rootNode.name).isEqualTo("w1")
    assertThat(rootNode.id).isEqualTo(1)
    assertThat(rootNode.uniqueID).isEqualTo(1)
    assertThat(rootNode.wbs).isNotEqualTo("0")
    assertThat(rootNode.outlineLevel).isEqualTo(1)
    assertThat(rootNode.outlineNumber).isEqualTo("1")
  }
}
