/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.project.facade.listener

import com.bosch.pt.csm.cloud.common.blob.model.Blob
import com.bosch.pt.csm.cloud.common.blob.model.BlobMetadata
import com.bosch.pt.csm.cloud.common.blob.repository.AzureBlobStorageRepository
import com.bosch.pt.csm.cloud.common.blob.repository.QuarantineBlobStorageRepository
import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.common.test.kafka.TestAcknowledgement
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitCompany
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitEmployee
import com.bosch.pt.csm.cloud.companymanagement.company.messages.StreetAddressAvro
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeRoleEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneTypeEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.event.SubmitMilestoneWithListDto
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitDayCardG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMessage
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMessageAttachment
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMilestonesWithList
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftList
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
import com.bosch.pt.csm.cloud.projectmanagement.project.util.asSlot
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardReasonNotDoneEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionEnumAvro
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitSystemUserAndActivate
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUserAndActivate
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.application.security.AuthorizationTestUtils.simulateKafkaListener
import com.bosch.pt.iot.smartsite.attachment.boundary.BlobStoreService
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractDeleteIntegrationTest
import com.bosch.pt.iot.smartsite.company.model.Company
import com.bosch.pt.iot.smartsite.company.model.Employee
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCard
import com.bosch.pt.iot.smartsite.project.importer.model.ProjectImport
import com.bosch.pt.iot.smartsite.project.importer.model.ProjectImportStatus
import com.bosch.pt.iot.smartsite.project.message.domain.asMessageId
import com.bosch.pt.iot.smartsite.project.message.shared.model.Message
import com.bosch.pt.iot.smartsite.project.messageattachment.model.MessageAttachment
import com.bosch.pt.iot.smartsite.project.milestone.domain.asMilestoneId
import com.bosch.pt.iot.smartsite.project.milestone.domain.asMilestoneListId
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.Milestone
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.MilestoneList
import com.bosch.pt.iot.smartsite.project.participant.shared.model.Participant
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.project.shared.boundary.ProjectDeleteService
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project
import com.bosch.pt.iot.smartsite.project.project.shared.model.ProjectBuilder.Companion.project
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.asProjectCraftId
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.asProjectCraftListId
import com.bosch.pt.iot.smartsite.project.projectcraft.shared.model.ProjectCraft
import com.bosch.pt.iot.smartsite.project.projectcraft.shared.model.ProjectCraftList
import com.bosch.pt.iot.smartsite.project.projectpicture.model.ProjectPicture
import com.bosch.pt.iot.smartsite.project.quickfilter.domain.QuickFilterId
import com.bosch.pt.iot.smartsite.project.quickfilter.model.MilestoneCriteria
import com.bosch.pt.iot.smartsite.project.quickfilter.model.QuickFilter
import com.bosch.pt.iot.smartsite.project.quickfilter.model.TaskCriteria
import com.bosch.pt.iot.smartsite.project.relation.model.Relation
import com.bosch.pt.iot.smartsite.project.rfv.model.RfvCustomization
import com.bosch.pt.iot.smartsite.project.task.command.service.TaskRequestDeleteService
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskId
import com.bosch.pt.iot.smartsite.project.task.shared.model.Task
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskBuilder
import com.bosch.pt.iot.smartsite.project.taskattachment.model.TaskAttachment
import com.bosch.pt.iot.smartsite.project.taskconstraint.model.TaskConstraintCustomization
import com.bosch.pt.iot.smartsite.project.taskconstraint.model.TaskConstraintSelection
import com.bosch.pt.iot.smartsite.project.taskschedule.domain.asTaskScheduleId
import com.bosch.pt.iot.smartsite.project.taskschedule.shared.model.TaskSchedule
import com.bosch.pt.iot.smartsite.project.topic.domain.asTopicId
import com.bosch.pt.iot.smartsite.project.topic.shared.model.Topic
import com.bosch.pt.iot.smartsite.project.topicattachment.model.TopicAttachment
import com.bosch.pt.iot.smartsite.project.workarea.domain.asWorkAreaId
import com.bosch.pt.iot.smartsite.project.workarea.domain.asWorkAreaListId
import com.bosch.pt.iot.smartsite.project.workarea.shared.model.WorkArea
import com.bosch.pt.iot.smartsite.project.workarea.shared.model.WorkAreaList
import com.bosch.pt.iot.smartsite.project.workday.domain.asWorkdayConfigurationId
import com.bosch.pt.iot.smartsite.project.workday.shared.model.WorkdayConfiguration
import com.bosch.pt.iot.smartsite.user.model.User
import io.mockk.every
import io.mockk.impl.annotations.MockK
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.TimeZone
import org.apache.commons.lang3.RandomStringUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@EnableAllKafkaListeners
@DisplayName("check deletion")
class DeleteIntegrationTest : AbstractDeleteIntegrationTest() {

  @MockK(relaxed = true) private lateinit var blob: Blob

  @Autowired private lateinit var azureBlobStorageRepository: AzureBlobStorageRepository

  @Autowired private lateinit var quarantineBlobStorageRepository: QuarantineBlobStorageRepository

  @Autowired private lateinit var blobStoreService: BlobStoreService

  @Autowired private lateinit var projectDeleteService: ProjectDeleteService

  @Autowired private lateinit var taskRequestDeleteService: TaskRequestDeleteService

  private lateinit var user: User
  private lateinit var company: Company
  private lateinit var employee: Employee
  private lateinit var project: Project
  private lateinit var projectImport: ProjectImport
  private lateinit var participant: Participant
  private lateinit var projectCraft: ProjectCraft
  private lateinit var projectCraftList: ProjectCraftList
  private lateinit var workdayConfiguration: WorkdayConfiguration
  private lateinit var projectPicture: ProjectPicture
  private lateinit var workArea: WorkArea
  private lateinit var workAreaList: WorkAreaList
  private lateinit var milestone: Milestone
  private lateinit var milestoneList: MilestoneList
  private lateinit var relation: Relation
  private lateinit var task: Task
  private lateinit var topic: Topic
  private lateinit var topicAttachment: TopicAttachment
  private lateinit var message: Message
  private lateinit var messageAttachment: MessageAttachment
  private lateinit var taskAttachment: TaskAttachment
  private lateinit var taskSchedule: TaskSchedule
  private lateinit var dayCard: DayCard
  private lateinit var taskConstraintCustomization: TaskConstraintCustomization
  private lateinit var taskConstraintSelection: TaskConstraintSelection
  private lateinit var quickFilter: QuickFilter
  private lateinit var rfvCustomization: RfvCustomization
  private val blobNames: MutableList<String> = ArrayList()

  @BeforeEach
  fun beforeEach() {
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
        .submitProjectCraftList()
        .submitRfvCustomization { it.key = DayCardReasonNotDoneEnumAvro.CUSTOM1 }
        .submitWorkArea()
        .submitWorkAreaList()
        .submitMilestonesWithList(
            date = LocalDate.now(),
            workArea = "workArea",
            milestones =
                listOf(
                    SubmitMilestoneWithListDto(type = MilestoneTypeEnumAvro.INVESTOR),
                    SubmitMilestoneWithListDto(
                        type = MilestoneTypeEnumAvro.CRAFT, craft = getByReference("projectCraft")),
                    SubmitMilestoneWithListDto(type = MilestoneTypeEnumAvro.PROJECT),
                ))
        .submitTask()
        .submitRelation()
        .submitTaskSchedule {
          it.start = LocalDate.now().minusDays(2).toEpochMilli()
          it.end = LocalDate.now().plusDays(10).toEpochMilli()
        }
        .submitDayCardG2 { it.reason = DayCardReasonNotDoneEnumAvro.CUSTOM1 }
        .submitTaskSchedule(eventType = TaskScheduleEventEnumAvro.UPDATED) {
          it.slots = listOf(getByReference("dayCard").asSlot(LocalDate.now()))
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

    user = repositories.findUser(getIdentifier("user"))!!
    company = repositories.findCompany(getIdentifier("company"))!!
    employee = repositories.findEmployee(getIdentifier("employee"))!!
    project = repositories.findProject(getIdentifier("project").asProjectId())!!
    projectPicture = repositories.findProjectPicture(getIdentifier("projectPicture"))!!
    participant = repositories.findParticipant(getIdentifier("participant"))!!
    workdayConfiguration =
        repositories.findWorkdayConfiguration(
            getIdentifier("workdayConfiguration").asWorkdayConfigurationId())!!
    projectCraft = repositories.findProjectCraft(getIdentifier("projectCraft").asProjectCraftId())!!
    projectCraftList =
        repositories.findProjectCraftList(
            getIdentifier("projectCraftList").asProjectCraftListId())!!
    rfvCustomization = repositories.findRfvWithDetails(getIdentifier("rfvCustomization"))!!
    workArea = repositories.findWorkArea(getIdentifier("workArea").asWorkAreaId())!!
    workAreaList = repositories.findWorkAreaList(getIdentifier("workAreaList").asWorkAreaListId())!!
    milestone = repositories.findMilestone(getIdentifier("milestoneListM0").asMilestoneId())!!
    milestoneList =
        repositories.findMilestoneList(getIdentifier("milestoneList").asMilestoneListId())!!
    relation = repositories.findRelation(getIdentifier("relation"), project.identifier)!!
    task = repositories.findTaskWithDetails(getIdentifier("task").asTaskId())!!
    taskSchedule =
        repositories.findTaskScheduleWithDetails(getIdentifier("taskSchedule").asTaskScheduleId())!!
    dayCard = taskSchedule.getDayCard(LocalDate.now())!!
    taskConstraintCustomization =
        repositories.findTaskConstraintWithDetails(getIdentifier("taskConstraintCustomization"))!!
    taskConstraintSelection =
        repositories.findTaskConstraintSelectionByIdentifier(getIdentifier("taskAction"))!!
    topic = repositories.findTopic(getIdentifier("topic").asTopicId())!!
    message = repositories.findMessage(getIdentifier("message").asMessageId())!!
    taskAttachment = repositories.findTaskAttachment(getIdentifier("taskAttachment"))!!
    topicAttachment = repositories.findTopicAttachment(getIdentifier("topicAttachment"))!!
    messageAttachment = repositories.findMessageAttachment(getIdentifier("messageAttachment"))!!

    setAuthentication("user")

    // This data is only stored in the database, not in Kafka. Therefore, the data is created
    // using the repositories
    projectImport = repositories.projectImportRepository.save(createProjectImport())
    quickFilter = repositories.quickFilterRepository.save(createQuickFilter())

    every { quarantineBlobStorageRepository.save(any(), any(), any(), any()) } answers
        {
          blobNames.add(it.invocation.args[1] as String)
          blob
        }
    every { azureBlobStorageRepository.deleteBlobsInDirectory(any()) } answers
        {
          val blobName = this.invocation.args[0] as String
          val parentIdentifier = blobName.split("/").dropLastWhile { it.isBlank() }.last()
          blobNames.removeAll(blobNames.filter { it.contains(parentIdentifier) })
        }

    transactionTemplate.executeWithoutResult {
      blobStoreService.saveImage(
          "test".toByteArray(),
          taskAttachment,
          BlobMetadata.from(RandomStringUtils.random(8), TimeZone.getDefault(), taskAttachment))
    }
    transactionTemplate.executeWithoutResult {
      blobStoreService.saveImage(
          "test".toByteArray(),
          topicAttachment,
          BlobMetadata.from(RandomStringUtils.random(8), TimeZone.getDefault(), topicAttachment))
    }
    transactionTemplate.executeWithoutResult {
      blobStoreService.saveImage(
          "test".toByteArray(),
          projectPicture,
          BlobMetadata.from(RandomStringUtils.random(8), TimeZone.getDefault(), projectPicture))
    }
  }

  @Test
  fun `for a project already marked as deleted`() {
    simulateKafkaListener { projectDeleteService.markAsDeleted(project.identifier) }
    deleteProject(project)
    verifyProjectDeletion(project)
  }

  @Test
  fun `for a project not marked as deleted`() {
    deleteProject(project)
    verifyProjectDeletion(project)
  }

  @Test
  fun `for a non-existing project`() {
    deleteProject(project().build())
  }

  @Test
  fun `for a task already marked as deleted`() {
    simulateKafkaListener { taskRequestDeleteService.markAsDeleted(task.identifier) }
    deleteTask(task)
    verifyTaskDeletion(task)
  }

  @Test
  fun `for a task not marked as deleted`() {
    deleteTask(task)
    verifyTaskDeletion(task)
  }

  @Test
  fun `for a non-existing task`() {
    deleteTask(TaskBuilder.task().build())
  }

  @Test
  fun `for a topic already marked as deleted`() {
    simulateKafkaListener { taskRequestDeleteService.markAsDeleted(task.identifier) }
    deleteTopic(topic)
    verifyTopicDeletion(topic)
  }

  @Test
  fun `for a topic not marked as deleted`() {
    deleteTopic(topic)
    verifyTopicDeletion(topic)
  }

  @Test
  fun `for a non-existing topic`() {
    val topic2 = repositories.findTopic(getIdentifier("topic").asTopicId())!!
    deleteTopic(topic2)
  }

  private fun deleteProject(project: Project) {
    val acknowledgment = TestAcknowledgement()

    // Send delete command
    sendDeleteCommand(
        project.identifier.toUuid(),
        project.version,
        ProjectmanagementAggregateTypeEnum.PROJECT,
        user,
        acknowledgment)

    // Verify that processing was successful
    assertThat(acknowledgment.isAcknowledged).isTrue
  }

  private fun verifyProjectDeletion(project: Project) {
    // Verify that expected data is deleted
    assertThat(
            repositories.findTaskConstraintSelectionByIdentifier(
                taskConstraintSelection.identifier!!))
        .isNull()
    assertThat(repositories.findRfvWithDetails(rfvCustomization.identifier!!)).isNull()
    assertThat(repositories.findTaskConstraintWithDetails(taskConstraintCustomization.identifier!!))
        .isNull()
    assertThat(repositories.findTaskScheduleWithDetails(taskSchedule.identifier)).isNull()
    assertThat(repositories.findDayCard(dayCard.identifier)).isNull()
    assertThat(repositories.findTaskAttachment(taskAttachment.identifier!!)).isNull()
    assertThat(repositories.findMessageAttachment(messageAttachment.identifier!!)).isNull()
    assertThat(repositories.findMessage(message.identifier)).isNull()
    assertThat(repositories.findTopicAttachment(topicAttachment.identifier!!)).isNull()
    assertThat(repositories.findTopic(topic.identifier)).isNull()
    assertThat(repositories.findTaskWithDetails(task.identifier)).isNull()
    assertThat(repositories.findQuickFilter(project.identifier, quickFilter.identifier)).isNull()
    assertThat(repositories.findWorkArea(workArea.identifier)).isNull()
    assertThat(repositories.findWorkAreaList(workAreaList.identifier)).isNull()
    assertThat(repositories.findMilestone(milestone.identifier)).isNull()
    assertThat(repositories.findMilestoneList(milestoneList.identifier)).isNull()
    assertThat(repositories.findProjectPicture(projectPicture.identifier!!)).isNull()
    assertThat(repositories.findProjectImport(project.identifier)).isNull()
    assertThat(repositories.findProjectCraft(projectCraft.identifier)).isNull()
    assertThat(repositories.findProjectCraftList(projectCraftList.identifier)).isNull()
    assertThat(repositories.findWorkdayConfiguration(workdayConfiguration.identifier)).isNull()
    assertThat(repositories.findParticipant(participant.identifier)).isNull()
    assertThat(repositories.findProject(project.identifier)).isNull()
    assertThat(repositories.findRelation(relation.identifier!!, project.identifier)).isNull()
    assertThat(blobNames).isEmpty()

    // Verify that expected data still exists after deletion
    assertThat(repositories.findEmployee(employee.identifier!!)).isNotNull
    assertThat(repositories.findCompany(company.identifier!!)).isNotNull
    assertThat(repositories.findUser(user.identifier!!)).isNotNull
  }

  private fun deleteTask(task: Task) {
    val acknowledgment = TestAcknowledgement()

    // Send delete command
    sendDeleteCommand(
        task.identifier.toUuid(),
        task.version,
        ProjectmanagementAggregateTypeEnum.TASK,
        user,
        acknowledgment)

    // Verify that processing was successful
    assertThat(acknowledgment.isAcknowledged).isTrue
  }

  private fun verifyTaskDeletion(task: Task) {
    // Verify that expected data is deleted
    assertThat(
            repositories.findTaskConstraintSelectionByIdentifier(
                taskConstraintSelection.identifier!!))
        .isNull()
    assertThat(repositories.findTaskScheduleWithDetails(taskSchedule.identifier)).isNull()
    assertThat(repositories.findDayCard(dayCard.identifier)).isNull()
    assertThat(repositories.findTaskAttachment(taskAttachment.identifier!!)).isNull()
    assertThat(repositories.findMessageAttachment(messageAttachment.identifier!!)).isNull()
    assertThat(repositories.findMessage(message.identifier)).isNull()
    assertThat(repositories.findTopicAttachment(topicAttachment.identifier!!)).isNull()
    assertThat(repositories.findTopic(topic.identifier)).isNull()
    assertThat(repositories.findTaskWithDetails(task.identifier)).isNull()
    assertThat(repositories.findRelation(relation.identifier!!, project.identifier)).isNull()

    // Verify that expected data still exists after deletion
    assertThat(repositories.findRfvWithDetails(rfvCustomization.identifier!!)).isNotNull
    assertThat(repositories.findTaskConstraintWithDetails(taskConstraintCustomization.identifier!!))
        .isNotNull
    assertThat(repositories.findWorkArea(workArea.identifier)).isNotNull
    assertThat(repositories.findWorkAreaList(workAreaList.identifier)).isNotNull
    assertThat(repositories.findMilestone(milestone.identifier)).isNotNull
    assertThat(repositories.findMilestoneList(milestoneList.identifier)).isNotNull
    assertThat(repositories.findProjectImport(project.identifier)).isNotNull
    assertThat(repositories.findProjectPicture(projectPicture.identifier!!)).isNotNull
    assertThat(repositories.findProjectCraft(projectCraft.identifier)).isNotNull
    assertThat(repositories.findProjectCraftList(projectCraftList.identifier)).isNotNull
    assertThat(repositories.findParticipant(participant.identifier)).isNotNull
    assertThat(repositories.findProject(project.identifier)).isNotNull
    assertThat(repositories.findEmployee(employee.identifier!!)).isNotNull
    assertThat(repositories.findCompany(company.identifier!!)).isNotNull
    assertThat(repositories.findUser(user.identifier!!)).isNotNull
    assertThat(repositories.findQuickFilter(project.identifier, quickFilter.identifier)).isNotNull
    assertThat(blobNames).hasSize(1)
  }

  private fun deleteTopic(topic: Topic) {
    val acknowledgment = TestAcknowledgement()

    // Send delete command
    sendDeleteCommand(
        topic.identifier.toUuid(),
        topic.version,
        ProjectmanagementAggregateTypeEnum.TOPIC,
        user,
        acknowledgment)

    // Verify that processing was successful
    assertThat(acknowledgment.isAcknowledged).isTrue
  }

  private fun verifyTopicDeletion(topic: Topic) {
    // Verify that expected data is deleted
    assertThat(repositories.findMessageAttachment(messageAttachment.identifier!!)).isNull()
    assertThat(repositories.findMessage(message.identifier)).isNull()
    assertThat(repositories.findTopicAttachment(topicAttachment.identifier!!)).isNull()
    assertThat(repositories.findTopic(topic.identifier)).isNull()

    // Verify that expected data still exists after deletion
    assertThat(repositories.findRfvWithDetails(rfvCustomization.identifier!!)).isNotNull
    assertThat(repositories.findTaskConstraintWithDetails(taskConstraintCustomization.identifier!!))
        .isNotNull
    assertThat(repositories.findTaskScheduleWithDetails(taskSchedule.identifier)).isNotNull
    assertThat(repositories.findDayCard(dayCard.identifier)).isNotNull
    assertThat(repositories.findTaskAttachment(taskAttachment.identifier!!)).isNotNull
    assertThat(repositories.findTaskWithDetails(task.identifier)).isNotNull
    assertThat(
            repositories.findTaskConstraintSelectionByIdentifier(
                taskConstraintSelection.identifier!!))
        .isNotNull
    assertThat(repositories.findWorkArea(workArea.identifier)).isNotNull
    assertThat(repositories.findWorkAreaList(workAreaList.identifier)).isNotNull
    assertThat(repositories.findMilestone(milestone.identifier)).isNotNull
    assertThat(repositories.findMilestoneList(milestoneList.identifier)).isNotNull
    assertThat(repositories.findProjectImport(project.identifier)).isNotNull
    assertThat(repositories.findProjectPicture(projectPicture.identifier!!)).isNotNull
    assertThat(repositories.findProjectCraft(projectCraft.identifier)).isNotNull
    assertThat(repositories.findProjectCraftList(projectCraftList.identifier)).isNotNull
    assertThat(repositories.findParticipant(participant.identifier)).isNotNull
    assertThat(repositories.findProject(project.identifier)).isNotNull
    assertThat(repositories.findEmployee(employee.identifier!!)).isNotNull
    assertThat(repositories.findCompany(company.identifier!!)).isNotNull
    assertThat(repositories.findUser(user.identifier!!)).isNotNull
    assertThat(repositories.findQuickFilter(project.identifier, quickFilter.identifier)).isNotNull
    assertThat(blobNames).hasSize(3)
  }

  private fun createProjectImport(): ProjectImport =
      ProjectImport(
          projectIdentifier = project.identifier,
          blobName = "fake-blob-name",
          status = ProjectImportStatus.FAILED,
          createdDate = LocalDateTime.now(),
          readWorkingAreasHierarchically = true,
          craftColumn = null,
          workAreaColumn = null,
          jobId = null)

  private fun createQuickFilter(): QuickFilter =
      QuickFilter(
          identifier = QuickFilterId(),
          name = "Quick Filter 1",
          projectIdentifier = project.identifier,
          participantIdentifier = participant.identifier,
          milestoneCriteria =
              MilestoneCriteria(from = LocalDate.now(), to = LocalDate.now().plusDays(14)),
          taskCriteria = TaskCriteria(from = LocalDate.now(), to = LocalDate.now().plusDays(14)))
}
