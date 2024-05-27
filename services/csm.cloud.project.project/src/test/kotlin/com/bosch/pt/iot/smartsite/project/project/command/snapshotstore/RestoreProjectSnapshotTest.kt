/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.project.command.snapshotstore

import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.common.extensions.toLocalDateByMillis
import com.bosch.pt.csm.cloud.common.streamable.restoredb.RestoreStrategyTest
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator.Companion.newAggregateIdentifier
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.get
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitCompany
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitEmployee
import com.bosch.pt.csm.cloud.companymanagement.company.messages.StreetAddressAvro
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeRoleEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneTypeEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.event.SubmitMilestoneWithListDto
import com.bosch.pt.csm.cloud.projectmanagement.project.event.listener.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitDayCardG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMessage
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMessageAttachment
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMilestonesWithList
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectPicture
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitRelation
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitRfvCustomization
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskAction
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskAttachment
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskConstraintCustomization
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskSchedule
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTopicAttachment
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTopicG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkArea
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkAreaList
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkdayConfiguration
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectAddressAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectCategoryEnumAvro.NB
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectCategoryEnumAvro.OB
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.project.util.asSlot
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardReasonNotDoneEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventEnumAvro.UPDATED
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionEnumAvro
import com.bosch.pt.csm.cloud.usermanagement.common.UsermanagementAggregateTypeEnum.USER
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitSystemUserAndActivate
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUserAndActivate
import com.bosch.pt.iot.smartsite.common.facade.listener.AbstractRestoreIntegrationTestV2
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project
import java.time.LocalDate.now
import java.util.UUID.randomUUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@RestoreStrategyTest
open class RestoreProjectSnapshotTest : AbstractRestoreIntegrationTestV2() {

  @BeforeEach
  fun init() {
    eventStreamGenerator.submitSystemUserAndActivate()
  }

  @Test
  open fun `validate that project created event was processed successfully`() {
    val projectIdentifier = ProjectId()
    eventStreamGenerator.submitUserAndActivate("daniel").submitProject {
      it.aggregateIdentifierBuilder.identifier = projectIdentifier.toString()
    }

    val projectAggregate = get<ProjectAggregateAvro>("project")!!

    transactionTemplate.executeWithoutResult {
      val project = repositories.projectRepository.findOneByIdentifier(projectIdentifier)!!

      validateProjectAggregateAttributes(project, projectAggregate)
      validateAuditingInformationAndIdentifierAndVersion(project, projectAggregate)
    }
  }

  @Test
  open fun `validate that project created event was processed successfully if the creator doesn't exist anymore`() {
    val projectIdentifier = ProjectId()
    eventStreamGenerator.submitUserAndActivate("daniel").submitProject {
      it.aggregateIdentifierBuilder.identifier = projectIdentifier.toString()
      it.auditingInformationBuilder.createdBy =
          newAggregateIdentifier(USER.value, randomUUID()).build()
    }

    val projectAggregate = get<ProjectAggregateAvro>("project")!!

    transactionTemplate.executeWithoutResult {
      val project = repositories.projectRepository.findOneByIdentifier(projectIdentifier)!!

      validateProjectAggregateAttributes(project, projectAggregate)
      validateAuditingInformationAndIdentifierAndVersion(project, projectAggregate)
    }
  }

  @Test
  open fun `validate that project updated event was processed successfully`() {
    val projectIdentifier = ProjectId()
    eventStreamGenerator
        .submitUserAndActivate("daniel")
        .submitProject {
          it.category = OB
          it.aggregateIdentifierBuilder.identifier = projectIdentifier.toString()
          it.auditingInformationBuilder.createdBy =
              newAggregateIdentifier(USER.value, randomUUID()).build()
        }
        .submitProject(eventType = ProjectEventEnumAvro.UPDATED) {
          it.auditingInformationBuilder.lastModifiedDate = 1L
          it.title = "Updated Project"
          it.description = "Updated Description"
          it.category = NB
          it.client = "Updated client"
          it.start = now().plusDays(10).toEpochMilli()
          it.end = now().plusDays(30).toEpochMilli()
          it.projectNumber = "Updated number"
          it.projectAddress =
              ProjectAddressAvro.newBuilder()
                  .setStreet("New Street")
                  .setHouseNumber("999")
                  .setZipCode("12345")
                  .setCity("New City")
                  .build()
        }

    val projectAggregate = get<ProjectAggregateAvro>("project")!!

    transactionTemplate.executeWithoutResult {
      val project = repositories.projectRepository.findOneByIdentifier(projectIdentifier)!!

      validateProjectAggregateAttributes(project, projectAggregate)
      validateAuditingInformationAndIdentifierAndVersion(project, projectAggregate)
    }
  }

  @Test
  open fun `validate project deleted event deletes a project`() {
    createProjectEventStream()

    validateRepositoryObjectCounts(1)

    eventStreamGenerator.submitProject(eventType = DELETED)

    validateRepositoryObjectCounts(0)

    // Send event again to test idempotency
    eventStreamGenerator.repeat(1)
  }

  private fun createProjectEventStream() {
    eventStreamGenerator
        .submitSystemUserAndActivate()
        .submitUserAndActivate("user")
        .submitCompany {
          it.streetAddress =
              StreetAddressAvro.newBuilder()
                  .setStreet("Teststreet")
                  .setHouseNumber("1")
                  .setZipCode("12345")
                  .setCity("Testtown")
                  .setArea("BW")
                  .setCountry("Germany")
                  .build()
        }
        .submitEmployee { it.roles = listOf(EmployeeRoleEnumAvro.CSM) }
        .submitProject()
        .submitWorkdayConfiguration()
        .submitProjectPicture()
        .submitParticipantG3()
        .submitProjectCraftG2()
        .submitRfvCustomization { it.key = DayCardReasonNotDoneEnumAvro.CUSTOM1 }
        .submitWorkArea()
        .submitWorkAreaList()
        .submitMilestonesWithList(
            date = now(),
            workArea = "workArea",
            milestones =
                listOf(
                    SubmitMilestoneWithListDto(
                        type = MilestoneTypeEnumAvro.CRAFT,
                        craft = getByReference("projectCraft"))))
        .submitTask()
        .submitTaskSchedule {
          it.start = now().minusDays(2).toEpochMilli()
          it.end = now().plusDays(10).toEpochMilli()
        }
        .submitDayCardG2 { it.reason = DayCardReasonNotDoneEnumAvro.CUSTOM1 }
        .submitTaskSchedule(eventType = UPDATED) {
          it.slots = listOf(getByReference("dayCard").asSlot(now()))
        }
        .submitTopicG2()
        .submitMessage()
        .submitTaskAttachment()
        .submitTopicAttachment()
        .submitMessageAttachment()
        .submitTaskConstraintCustomization { it.key = TaskActionEnumAvro.CUSTOM1 }
        .submitTaskAction {
          it.actions = listOf(TaskActionEnumAvro.MATERIAL, TaskActionEnumAvro.CUSTOM1)
        }
        .submitRelation()
  }

  private fun validateRepositoryObjectCounts(expectedCount: Int) {
    assertThat(repositories.projectRepository.findAll()).hasSize(expectedCount)
    assertThat(repositories.participantRepository.findAll()).hasSize(expectedCount)
    assertThat(repositories.projectPictureRepository.findAll()).hasSize(expectedCount)
    assertThat(repositories.projectCraftRepository.findAll()).hasSize(expectedCount)
    assertThat(repositories.workdayConfigurationRepository.findAll()).hasSize(expectedCount)
    assertThat(repositories.milestoneRepository.findAll()).hasSize(expectedCount)
    assertThat(repositories.milestoneListRepository.findAll()).hasSize(expectedCount)
    assertThat(repositories.workAreaRepository.findAll()).hasSize(expectedCount)
    assertThat(repositories.workAreaListRepository.findAll()).hasSize(expectedCount)
    assertThat(repositories.taskRepository.findAll()).hasSize(expectedCount)
    assertThat(repositories.taskConstraintCustomizationRepository.findAll()).hasSize(expectedCount)
    assertThat(repositories.taskConstraintSelectionRepository.findAll()).hasSize(expectedCount)
    assertThat(repositories.taskScheduleRepository.findAll()).hasSize(expectedCount)
    assertThat(repositories.dayCardRepository.findAll()).hasSize(expectedCount)
    assertThat(repositories.topicRepository.findAll()).hasSize(expectedCount)
    assertThat(repositories.messageRepository.findAll()).hasSize(expectedCount)
    assertThat(repositories.taskAttachmentRepository.findAll()).hasSize(expectedCount)
    assertThat(repositories.topicAttachmentRepository.findAll()).hasSize(expectedCount)
    assertThat(repositories.messageAttachmentRepository.findAll()).hasSize(expectedCount)
    assertThat(repositories.rfvCustomizationRepository.findAll()).hasSize(expectedCount)
  }

  private fun validateProjectAggregateAttributes(
      project: Project,
      projectAggregate: ProjectAggregateAvro
  ) {
    assertThat(project.identifier).isEqualTo(projectAggregate.getIdentifier().asProjectId())
    assertThat(project.version).isEqualTo(projectAggregate.aggregateIdentifier.version)
    if (projectAggregate.category == null) {
      assertThat(project.category).isNull()
    } else {
      assertThat(project.category!!.name).isEqualTo(projectAggregate.category.name)
    }
    assertThat(project.client).isEqualTo(projectAggregate.client)
    assertThat(project.description).isEqualTo(projectAggregate.description)
    assertThat(project.end).isEqualTo(projectAggregate.end.toLocalDateByMillis())
    assertThat(project.projectAddress!!.city).isEqualTo(projectAggregate.projectAddress.city)
    assertThat(project.projectAddress!!.houseNumber)
        .isEqualTo(projectAggregate.projectAddress.houseNumber)
    assertThat(project.projectAddress!!.street).isEqualTo(projectAggregate.projectAddress.street)
    assertThat(project.projectAddress!!.zipCode).isEqualTo(projectAggregate.projectAddress.zipCode)
    assertThat(project.projectNumber).isEqualTo(projectAggregate.projectNumber)
    assertThat(project.start).isEqualTo(projectAggregate.start.toLocalDateByMillis())
    assertThat(project.title).isEqualTo(projectAggregate.title)
  }
}
