/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.test

import com.bosch.pt.csm.cloud.projectmanagement.activity.repository.ActivityRepository
import com.bosch.pt.csm.cloud.projectmanagement.company.repository.CompanyRepository
import com.bosch.pt.csm.cloud.projectmanagement.project.craft.repository.ProjectCraftRepository
import com.bosch.pt.csm.cloud.projectmanagement.project.daycard.repository.DayCardRepository
import com.bosch.pt.csm.cloud.projectmanagement.project.message.repository.MessageRepository
import com.bosch.pt.csm.cloud.projectmanagement.project.milestone.repository.MilestoneRepository
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.repository.ParticipantRepository
import com.bosch.pt.csm.cloud.projectmanagement.project.project.repository.ProjectRepository
import com.bosch.pt.csm.cloud.projectmanagement.project.rfv.repository.RfvCustomizationRepository
import com.bosch.pt.csm.cloud.projectmanagement.project.task.repository.TaskRepository
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.repository.TaskConstraintCustomizationRepository
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.repository.TaskConstraintSelectionRepository
import com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.repository.TaskScheduleRepository
import com.bosch.pt.csm.cloud.projectmanagement.project.topic.repository.TopicRepository
import com.bosch.pt.csm.cloud.projectmanagement.project.workarea.repository.WorkAreaListRepository
import com.bosch.pt.csm.cloud.projectmanagement.project.workarea.repository.WorkAreaRepository
import com.bosch.pt.csm.cloud.projectmanagement.user.repository.UserRepository
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserAggregateAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.getIdentifier
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service

@Service
@Lazy
class Repositories(
    val userRepository: UserRepository,
    val companyRepository: CompanyRepository,
    val projectRepository: ProjectRepository,
    val participantRepository: ParticipantRepository,
    val projectCraftRepository: ProjectCraftRepository,
    val rfvCustomizationRepository: RfvCustomizationRepository,
    val taskConstraintCustomizationRepository: TaskConstraintCustomizationRepository,
    val workAreaRepository: WorkAreaRepository,
    val workAreaListRepository: WorkAreaListRepository,
    val taskRepository: TaskRepository,
    val taskConstraintSelectionRepository: TaskConstraintSelectionRepository,
    val taskScheduleRepository: TaskScheduleRepository,
    val dayCardRepository: DayCardRepository,
    val topicRepository: TopicRepository,
    val messageRepository: MessageRepository,
    val milestoneRepository: MilestoneRepository,
    val activityRepository: ActivityRepository
) {
  fun truncateDatabase() {
    activityRepository.deleteAll()
    milestoneRepository.deleteAll()
    messageRepository.deleteAll()
    topicRepository.deleteAll()
    dayCardRepository.deleteAll()
    taskScheduleRepository.deleteAll()
    taskRepository.deleteAll()
    taskConstraintSelectionRepository.deleteAll()
    workAreaListRepository.deleteAll()
    workAreaRepository.deleteAll()
    taskConstraintCustomizationRepository.deleteAll()
    rfvCustomizationRepository.deleteAll()
    projectCraftRepository.deleteAll()
    participantRepository.deleteAll()
    projectRepository.deleteAll()
    companyRepository.deleteAll()
    userRepository.deleteAll()
  }

  fun findUser(user: UserAggregateAvro) =
      userRepository.findOneCachedByIdentifier(user.getIdentifier())!!
}
