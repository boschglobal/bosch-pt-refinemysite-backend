/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.external.boundary

import com.bosch.pt.csm.cloud.common.eventstore.EventStoreUtils
import com.bosch.pt.csm.cloud.common.eventstore.exception.WrongNumberOfEventsException
import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.external.messages.ExternalIdEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.external.messages.ExternalIdEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.external.messages.ExternalIdTypeEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskSchedule
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkArea
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkAreaList
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventEnumAvro
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextKafkaEvent
import com.bosch.pt.iot.smartsite.project.exporter.api.ProjectExportFormatEnum
import com.bosch.pt.iot.smartsite.project.exporter.api.ProjectExportParameters
import com.bosch.pt.iot.smartsite.project.exporter.boundary.AbstractExportIntegrationTest
import com.bosch.pt.iot.smartsite.project.exporter.boundary.ProjectExportService
import com.bosch.pt.iot.smartsite.project.exporter.boundary.ProjectExportService.Companion.FIELD_ALIAS_CRAFT
import com.bosch.pt.iot.smartsite.project.external.model.ObjectType.TASK
import com.bosch.pt.iot.smartsite.project.external.model.ObjectType.WORKAREA
import com.bosch.pt.iot.smartsite.project.importer.submitProjectImportFeatureToggle
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import org.apache.avro.generic.GenericEnumSymbol
import org.apache.avro.specific.SpecificRecordBase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired

@EnableAllKafkaListeners
class ExportExternalIdIntegrationTest : AbstractExportIntegrationTest() {

  @Autowired private lateinit var projectExportService: ProjectExportService

  private val projectIdentifier by lazy { getIdentifier("p2").asProjectId() }
  private val project by lazy { checkNotNull(repositories.findProject(projectIdentifier)) }

  @BeforeEach
  fun init() {
    val nextMonday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY))

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
        }
        .submitTask(asReference = "t2") {
          it.project = getByReference("p2")
          it.workarea = getByReference("w1")
          it.craft = getByReference("pc1")
          it.name = "t2"
        }
        .submitTaskSchedule("s1") {
          it.task = getByReference("t1")
          it.start = nextMonday.toEpochMilli()
          it.end = nextMonday.plusDays(1).toEpochMilli()
        }
        .submitTaskSchedule("s2") {
          it.task = getByReference("t2")
          it.start = nextMonday.toEpochMilli()
          it.end = nextMonday.plusDays(1).toEpochMilli()
        }
    setAuthentication("userCsm1")

    projectEventStoreUtils.reset()
  }

  @ParameterizedTest
  @ValueSource(strings = ["MS_PROJECT_XML"])
  fun reExportProject(format: String) {
    firstExport(format)

    // Delete the second task
    eventStreamGenerator
        .submitTaskSchedule("s2", eventType = TaskScheduleEventEnumAvro.DELETED)
        .submitTask("t2", eventType = TaskEventEnumAvro.DELETED)

    projectEventStoreUtils.reset()

    secondExport(format)
  }

  private fun firstExport(format: String) {
    val exportedFile =
        projectExportService.export(
            project,
            ProjectExportParameters(
                format = ProjectExportFormatEnum.valueOf(format),
                includeMilestones = false,
                includeComments = false))

    val importModel = readExportedFile(exportedFile, project, FIELD_ALIAS_CRAFT)

    assertThat(importModel.workAreas).hasSize(1)
    val w1 = importModel.workAreas[0]
    assertThat(w1.name).isEqualTo("w1")
    validateExportedIds(w1, "w1", 1, 1)

    assertThat(importModel.crafts).hasSize(1)
    val craft1 = importModel.crafts.single { it.name == "pc1" }
    assertThat(craft1).isNotNull
    validateExportedIds(craft1, "t1", 2, 2)

    assertThat(importModel.tasks).hasSize(2)
    val t1 = importModel.tasks.single { it.name == "t1" }
    assertThat(t1.workAreaId).isEqualTo(w1.id)
    validateExportedIds(t1, "t1", 2, 2)
    val t2 = importModel.tasks.single { it.name == "t2" }
    assertThat(t2.workAreaId).isEqualTo(w1.id)
    validateExportedIds(t2, "t2", 3, 3)

    assertThat(importModel.taskSchedules).hasSize(2)
    val s1 = importModel.taskSchedules.single { it.taskId == t1.id }
    assertThat(s1.start).isEqualTo(getEventStartDate("s1"))
    assertThat(s1.end).isEqualTo(getEventEndDate("s1"))
    validateExportedIds(s1, "t1", 2, 2)

    val s2 = importModel.taskSchedules.single { it.taskId == t2.id }
    assertThat(s2.start).isEqualTo(getEventStartDate("s2"))
    assertThat(s2.end).isEqualTo(getEventEndDate("s2"))
    validateExportedIds(s2, "t2", 3, 3)

    // Three create external id events expected
    val externalIds =
        projectEventStoreUtils.verifyContainsAndGet(
            ExternalIdEventAvro::class.java, ExternalIdEventEnumAvro.CREATED, 3)

    // No update event expected
    projectEventStoreUtils.verifyContainsAndGetEvenIfZero(
        ExternalIdEventAvro::class.java, ExternalIdEventEnumAvro.UPDATED, 0)

    // No delete event expected
    projectEventStoreUtils.verifyContainsAndGetEvenIfZero(
        ExternalIdEventAvro::class.java, ExternalIdEventEnumAvro.DELETED, 0)

    // Check events
    val workArea =
        externalIds
            .single { it.aggregate.objectIdentifier.identifier == getIdentifier("w1").toString() }
            .aggregate
    val workAreaFromImportModel = importModel.workAreas.first()

    assertThat(workArea.project.identifier).isEqualTo(getIdentifier("p2").toString())
    assertThat(workArea.objectIdentifier.identifier).isEqualTo(getIdentifier("w1").toString())
    assertThat(workArea.objectIdentifier.type).isEqualTo(WORKAREA.name)
    assertThat(workArea.type).isEqualTo(ExternalIdTypeEnumAvro.MS_PROJECT)
    assertThat(workArea.fileId).isEqualTo(2)
    assertThat(workArea.fileUniqueId).isEqualTo(1)
    assertThat(workArea.guid).isEqualTo(workAreaFromImportModel.guid.toString())
    assertThat(workArea.activityId).isEqualTo(workAreaFromImportModel.activityId)
    assertThat(workArea.wbs).isEqualTo(workAreaFromImportModel.wbs)

    val task1 =
        externalIds
            .single { it.aggregate.objectIdentifier.identifier == getIdentifier("t1").toString() }
            .aggregate
    val task1FromImportModel = importModel.tasks.single { it.guid == getIdentifier("t1") }

    assertThat(task1.project.identifier).isEqualTo(getIdentifier("p2").toString())
    assertThat(task1.objectIdentifier.identifier).isEqualTo(getIdentifier("t1").toString())
    assertThat(task1.objectIdentifier.type).isEqualTo(TASK.name)
    assertThat(task1.type).isEqualTo(ExternalIdTypeEnumAvro.MS_PROJECT)
    assertThat(task1.fileId).isEqualTo(3)
    assertThat(task1.fileUniqueId).isEqualTo(2)
    assertThat(task1.guid).isEqualTo(task1FromImportModel.guid.toString())
    assertThat(task1.activityId).isEqualTo(task1FromImportModel.activityId)
    assertThat(task1.wbs).isEqualTo(task1FromImportModel.wbs)

    val task2 =
        externalIds
            .single { it.aggregate.objectIdentifier.identifier == getIdentifier("t2").toString() }
            .aggregate
    val task2FromImportModel = importModel.tasks.single { it.guid == getIdentifier("t2") }

    assertThat(task2.project.identifier).isEqualTo(getIdentifier("p2").toString())
    assertThat(task2.objectIdentifier.identifier).isEqualTo(getIdentifier("t2").toString())
    assertThat(task2.objectIdentifier.type).isEqualTo(TASK.name)
    assertThat(task2.type).isEqualTo(ExternalIdTypeEnumAvro.MS_PROJECT)
    assertThat(task2.fileId).isEqualTo(4)
    assertThat(task2.fileUniqueId).isEqualTo(3)
    assertThat(task2.guid).isEqualTo(task2FromImportModel.guid.toString())
    assertThat(task2.activityId).isEqualTo(task2FromImportModel.activityId)
    assertThat(task2.wbs).isEqualTo(task2FromImportModel.wbs)
  }

  private fun secondExport(format: String) {
    val exportedFile =
        projectExportService.export(
            project,
            ProjectExportParameters(
                format = ProjectExportFormatEnum.valueOf(format),
                includeMilestones = false,
                includeComments = false))

    val importModel = readExportedFile(exportedFile, project, FIELD_ALIAS_CRAFT)

    assertThat(importModel.workAreas).hasSize(1)
    val w1 = importModel.workAreas[0]
    assertThat(w1.name).isEqualTo("w1")
    validateExportedIds(w1, "w1", 1, 1)

    assertThat(importModel.crafts).hasSize(1)
    val craft1 = importModel.crafts.single { it.name == "pc1" }
    assertThat(craft1).isNotNull
    validateExportedIds(craft1, "t1", 2, 2)

    assertThat(importModel.tasks).hasSize(1)
    val t1 = importModel.tasks.single { it.name == "t1" }
    assertThat(t1.workAreaId).isEqualTo(w1.id)
    validateExportedIds(t1, "t1", 2, 2)

    assertThat(importModel.taskSchedules).hasSize(1)
    val s1 = importModel.taskSchedules.single { it.taskId == t1.id }
    assertThat(s1.start).isEqualTo(getEventStartDate("s1"))
    assertThat(s1.end).isEqualTo(getEventEndDate("s1"))
    validateExportedIds(s1, "t1", 2, 2)

    // No create event expected

    projectEventStoreUtils.verifyContainsAndGetEvenIfZero(
        ExternalIdEventAvro::class.java, ExternalIdEventEnumAvro.CREATED, 0)

    // Theoretically we would expect 2 update events, though hibernate seem to be smart enough to
    // detekt that the entities have not been modified and therefore no event seem to be sent
    projectEventStoreUtils.verifyContainsAndGetEvenIfZero(
        ExternalIdEventAvro::class.java, ExternalIdEventEnumAvro.UPDATED, 0)

    // One delete event expected
    val deletedExternalIds =
        projectEventStoreUtils.verifyContainsAndGet(
            ExternalIdEventAvro::class.java, ExternalIdEventEnumAvro.DELETED, 1)

    // Check events
    val task2 =
        deletedExternalIds
            .single { it.aggregate.objectIdentifier.identifier == getIdentifier("t2").toString() }
            .aggregate

    assertThat(task2.project.identifier).isEqualTo(getIdentifier("p2").toString())
    assertThat(task2.objectIdentifier.identifier).isEqualTo(getIdentifier("t2").toString())
    assertThat(task2.objectIdentifier.type).isEqualTo(TASK.name)
    assertThat(task2.type).isEqualTo(ExternalIdTypeEnumAvro.MS_PROJECT)
    assertThat(task2.fileId).isEqualTo(4)
    assertThat(task2.fileUniqueId).isEqualTo(3)
  }

  private fun <E : SpecificRecordBase> EventStoreUtils<ProjectContextKafkaEvent>
      .verifyContainsAndGetEvenIfZero(
      eventType: Class<E>,
      eventName: GenericEnumSymbol<*>?,
      occurrences: Int,
  ): List<E> {
    val getDeserializedEvents =
        this.javaClass.getDeclaredMethod("getDeserializedEvents").apply { this.trySetAccessible() }
    val hasEventName =
        this.javaClass.declaredMethods
            .single { it.name == "hasEventName" }
            .apply { this.trySetAccessible() }
    val deserializedEvents = getDeserializedEvents.invoke(this) as List<SpecificRecordBase>
    val filteredEvents =
        deserializedEvents
            .filter { eventType.isInstance(it) }
            .filter {
              eventName == null ||
                  hasEventName.invoke(this@verifyContainsAndGetEvenIfZero, it, eventName) as Boolean
            }
            .map(eventType::cast)
            .toList()
    if (filteredEvents.size != occurrences) {
      if (eventName == null) {
        throw WrongNumberOfEventsException(eventType.name, occurrences, filteredEvents.size)
      } else {
        throw WrongNumberOfEventsException(
            eventType.name, eventName.toString(), occurrences, filteredEvents.size)
      }
    }
    return filteredEvents
  }
}
