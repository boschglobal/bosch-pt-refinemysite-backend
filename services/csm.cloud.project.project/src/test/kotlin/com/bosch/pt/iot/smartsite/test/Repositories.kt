/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.test

import com.bosch.pt.csm.cloud.common.businesstransaction.jpa.JpaEventOfBusinessTransactionRepository
import com.bosch.pt.iot.smartsite.company.repository.CompanyRepository
import com.bosch.pt.iot.smartsite.company.repository.EmployeeRepository
import com.bosch.pt.iot.smartsite.craft.repository.CraftRepository
import com.bosch.pt.iot.smartsite.project.daycard.domain.DayCardId
import com.bosch.pt.iot.smartsite.project.daycard.shared.repository.DayCardRepository
import com.bosch.pt.iot.smartsite.project.external.model.ExternalIdType
import com.bosch.pt.iot.smartsite.project.external.repository.ExternalIdRepository
import com.bosch.pt.iot.smartsite.project.importer.repository.ProjectImportRepository
import com.bosch.pt.iot.smartsite.project.message.domain.MessageId
import com.bosch.pt.iot.smartsite.project.message.shared.repository.MessageRepository
import com.bosch.pt.iot.smartsite.project.messageattachment.repository.MessageAttachmentRepository
import com.bosch.pt.iot.smartsite.project.milestone.domain.MilestoneId
import com.bosch.pt.iot.smartsite.project.milestone.domain.MilestoneListId
import com.bosch.pt.iot.smartsite.project.milestone.shared.repository.MilestoneListRepository
import com.bosch.pt.iot.smartsite.project.milestone.shared.repository.MilestoneRepository
import com.bosch.pt.iot.smartsite.project.participant.InvitationId
import com.bosch.pt.iot.smartsite.project.participant.ParticipantId
import com.bosch.pt.iot.smartsite.project.participant.asParticipantId
import com.bosch.pt.iot.smartsite.project.participant.shared.repository.InvitationRepository
import com.bosch.pt.iot.smartsite.project.participant.shared.repository.ParticipantRepository
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.shared.repository.ProjectRepository
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.ProjectCraftId
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.ProjectCraftListId
import com.bosch.pt.iot.smartsite.project.projectcraft.shared.repository.ProjectCraftListRepository
import com.bosch.pt.iot.smartsite.project.projectcraft.shared.repository.ProjectCraftRepository
import com.bosch.pt.iot.smartsite.project.projectpicture.repository.ProjectPictureRepository
import com.bosch.pt.iot.smartsite.project.quickfilter.domain.QuickFilterId
import com.bosch.pt.iot.smartsite.project.quickfilter.repository.QuickFilterRepository
import com.bosch.pt.iot.smartsite.project.relation.repository.RelationRepository
import com.bosch.pt.iot.smartsite.project.rfv.repository.RfvCustomizationRepository
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.shared.repository.TaskRepository
import com.bosch.pt.iot.smartsite.project.taskattachment.repository.TaskAttachmentRepository
import com.bosch.pt.iot.smartsite.project.taskconstraint.repository.TaskConstraintCustomizationRepository
import com.bosch.pt.iot.smartsite.project.taskconstraint.repository.TaskConstraintSelectionRepository
import com.bosch.pt.iot.smartsite.project.taskschedule.domain.TaskScheduleId
import com.bosch.pt.iot.smartsite.project.taskschedule.shared.repository.TaskScheduleRepository
import com.bosch.pt.iot.smartsite.project.topic.domain.TopicId
import com.bosch.pt.iot.smartsite.project.topic.shared.repository.TopicRepository
import com.bosch.pt.iot.smartsite.project.topicattachment.repository.TopicAttachmentRepository
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaId
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaListId
import com.bosch.pt.iot.smartsite.project.workarea.shared.repository.WorkAreaListRepository
import com.bosch.pt.iot.smartsite.project.workarea.shared.repository.WorkAreaRepository
import com.bosch.pt.iot.smartsite.project.workday.domain.WorkdayConfigurationId
import com.bosch.pt.iot.smartsite.project.workday.shared.repository.WorkdayConfigurationRepository
import com.bosch.pt.iot.smartsite.user.repository.ProfilePictureRepository
import com.bosch.pt.iot.smartsite.user.repository.UserRepository
import io.mockk.MockKGateway
import java.sql.ResultSet
import java.util.UUID
import javax.sql.DataSource
import org.springframework.context.annotation.Lazy
import org.springframework.context.annotation.Profile
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service

@Profile("test")
@Service
@Lazy
class Repositories(
    val userRepository: UserRepository,
    val companyRepository: CompanyRepository,
    val craftRepository: CraftRepository,
    val dayCardRepository: DayCardRepository,
    val employeeRepository: EmployeeRepository,
    val externalIdRepository: ExternalIdRepository,
    val invitationRepository: InvitationRepository,
    val messageAttachmentRepository: MessageAttachmentRepository,
    val messageRepository: MessageRepository,
    val milestoneListRepository: MilestoneListRepository,
    val milestoneRepository: MilestoneRepository,
    val participantRepository: ParticipantRepository,
    val profilePictureRepository: ProfilePictureRepository,
    val projectCraftRepository: ProjectCraftRepository,
    val projectCraftListRepository: ProjectCraftListRepository,
    val projectImportRepository: ProjectImportRepository,
    val projectPictureRepository: ProjectPictureRepository,
    val projectRepository: ProjectRepository,
    val relationRepository: RelationRepository,
    val rfvCustomizationRepository: RfvCustomizationRepository,
    val taskAttachmentRepository: TaskAttachmentRepository,
    val taskConstraintCustomizationRepository: TaskConstraintCustomizationRepository,
    val taskConstraintSelectionRepository: TaskConstraintSelectionRepository,
    val quickFilterRepository: QuickFilterRepository,
    val taskRepository: TaskRepository,
    val taskScheduleRepository: TaskScheduleRepository,
    val topicAttachmentRepository: TopicAttachmentRepository,
    val topicRepository: TopicRepository,
    val workAreaListRepository: WorkAreaListRepository,
    val workAreaRepository: WorkAreaRepository,
    val workdayConfigurationRepository: WorkdayConfigurationRepository,
    val eventOfBusinessTransactionRepository: JpaEventOfBusinessTransactionRepository,
    val dataSource: DataSource,
    val mongoTemplate: MongoTemplate,
    private val repositories: List<JpaRepository<*, *>>
) {

  fun truncateDatabase() {
    truncateMongoDatabase(mongoTemplate)
    truncateDatabaseFromSource(dataSource)
    repositories.forEach {
      if (!MockKGateway.implementation().mockFactory.isMock(it)) it.deleteAllInBatch()
    }
  }

  fun findCompany(identifier: UUID) = companyRepository.findOneWithDetailsByIdentifier(identifier)

  fun findDayCard(identifier: DayCardId) = dayCardRepository.findEntityByIdentifier(identifier)

  fun findEmployee(identifier: UUID) = employeeRepository.findOneWithDetailsByIdentifier(identifier)

  fun findInvitation(identifier: InvitationId) =
      invitationRepository.findOneByIdentifier(identifier)

  fun findExternalIds(projectId: ProjectId, type: ExternalIdType) =
      externalIdRepository.findAllByProjectIdAndIdType(projectId, type)

  fun findInvitation(identifier: UUID) =
      invitationRepository.findOneByIdentifier(InvitationId(identifier))

  fun findMessage(identifier: MessageId) =
      messageRepository.findOneWithDetailsByIdentifier(identifier)

  fun findMessageAttachment(identifier: UUID) =
      messageAttachmentRepository.findOneWithDetailsByIdentifier(identifier)

  fun findMilestone(identifier: MilestoneId) =
      milestoneRepository.findWithDetailsByIdentifier(identifier)

  fun findMilestoneWithDetails(identifier: MilestoneId) =
      milestoneRepository.findWithDetailsByIdentifier(identifier)!!

  fun findMilestoneList(identifier: MilestoneListId) =
      milestoneListRepository.findOneByIdentifier(identifier)

  fun findMilestoneListWithDetails(identifier: MilestoneListId) =
      milestoneListRepository.findOneWithDetailsByIdentifier(identifier)!!

  fun findParticipant(identifier: ParticipantId) =
      participantRepository.findOneByIdentifier(identifier)

  fun findParticipant(identifier: UUID) =
      participantRepository.findOneWithDetailsByIdentifier(identifier.asParticipantId())

  fun findProject(identifier: ProjectId) = projectRepository.findOneByIdentifier(identifier)

  fun findProjectCraft(identifier: ProjectCraftId) =
      projectCraftRepository.findOneWithDetailsByIdentifier(identifier)

  fun findProjectCraftList(identifier: ProjectCraftListId) =
      projectCraftListRepository.findOneWithDetailsByIdentifier(identifier)

  fun findProjectImport(projectIdentifier: ProjectId) =
      projectImportRepository.findOneByProjectIdentifier(projectIdentifier)

  fun findProjectPicture(identifier: UUID) =
      projectPictureRepository.findOneByIdentifier(identifier)

  fun findRelation(identifier: UUID, projectIdentifier: ProjectId) =
      relationRepository.findOneWithDetailsByIdentifierAndProjectIdentifier(
          identifier, projectIdentifier)

  fun findRfvWithDetails(identifier: UUID) =
      rfvCustomizationRepository.findOneWithDetailsByIdentifier(identifier)

  fun findTaskAttachment(identifier: UUID) =
      taskAttachmentRepository.findOneWithDetailsByIdentifier(identifier)

  fun findTaskConstraintSelectionByIdentifier(identifier: UUID) =
      taskConstraintSelectionRepository.findOneWithDetailsByIdentifier(identifier)

  fun findTaskConstraintWithDetails(identifier: UUID) =
      taskConstraintCustomizationRepository.findOneWithDetailsByIdentifier(identifier)

  fun findQuickFilter(projectIdentifier: ProjectId, identifier: QuickFilterId) =
      quickFilterRepository.findOneByIdentifierAndProjectIdentifier(identifier, projectIdentifier)

  fun findTaskScheduleWithDetails(identifier: TaskScheduleId) =
      taskScheduleRepository.findWithDetailsByIdentifier(identifier)

  fun findTaskWithDetails(identifier: TaskId) =
      taskRepository.findOneWithDetailsByIdentifier(identifier)

  fun findTopic(identifier: TopicId) = topicRepository.findOneWithDetailsByIdentifier(identifier)

  fun findTopicAttachment(identifier: UUID) =
      topicAttachmentRepository.findOneWithDetailsByIdentifier(identifier)

  fun findUser(identifier: UUID) = userRepository.findOneByIdentifier(identifier)

  fun findWorkArea(identifier: WorkAreaId) = workAreaRepository.findOneByIdentifier(identifier)

  fun findWorkAreaList(identifier: WorkAreaListId) =
      workAreaListRepository.findOneWithDetailsByIdentifier(identifier)

  fun findWorkdayConfiguration(identifier: WorkdayConfigurationId) =
      workdayConfigurationRepository.findOneWithDetailsByIdentifier(identifier)

  fun findWorkdayConfiguration(projectIdentifier: ProjectId) =
      workdayConfigurationRepository.findOneWithDetailsByProjectIdentifier(projectIdentifier)

  companion object {
    fun truncateMongoDatabase(mongoTemplate: MongoTemplate) {
      mongoTemplate.db.drop()
    }

    fun truncateDatabaseFromSource(source: DataSource) {
      val jdbcTemplate = JdbcTemplate(source)
      jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE")
      jdbcTemplate
          .query("SELECT * FROM information_schema.tables where table_schema = 'PUBLIC'") {
              rs: ResultSet,
              _: Int ->
            rs.getString("table_name")
          }
          .filter { table -> table != "flyway_schema_history" }
          .forEach { table -> jdbcTemplate.execute("truncate table $table") }
      jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE")
    }
  }
}
