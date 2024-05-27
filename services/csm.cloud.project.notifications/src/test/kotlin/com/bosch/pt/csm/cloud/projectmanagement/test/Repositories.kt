/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.test

import com.bosch.pt.csm.cloud.projectmanagement.company.repository.CompanyRepository
import com.bosch.pt.csm.cloud.projectmanagement.company.repository.EmployeeRepository
import com.bosch.pt.csm.cloud.projectmanagement.notification.repository.NotificationRepository
import com.bosch.pt.csm.cloud.projectmanagement.project.craft.repository.ProjectCraftRepository
import com.bosch.pt.csm.cloud.projectmanagement.project.daycard.repository.DayCardRepository
import com.bosch.pt.csm.cloud.projectmanagement.project.message.repository.MessageRepository
import com.bosch.pt.csm.cloud.projectmanagement.project.milestone.repository.MilestoneRepository
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.repository.ParticipantRepository
import com.bosch.pt.csm.cloud.projectmanagement.project.project.repository.ProjectRepository
import com.bosch.pt.csm.cloud.projectmanagement.project.rfv.repository.RfvCustomizationRepository
import com.bosch.pt.csm.cloud.projectmanagement.project.task.repository.TaskRepository
import com.bosch.pt.csm.cloud.projectmanagement.project.taskattachment.repository.TaskAttachmentRepository
import com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.repository.TaskScheduleRepository
import com.bosch.pt.csm.cloud.projectmanagement.project.topic.repository.TopicRepository
import com.bosch.pt.csm.cloud.projectmanagement.project.workarea.repository.WorkAreaListRepository
import com.bosch.pt.csm.cloud.projectmanagement.project.workarea.repository.WorkAreaRepository
import com.bosch.pt.csm.cloud.projectmanagement.user.repository.UserRepository
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service

@Service
@Lazy
class Repositories(
    val userRepository: UserRepository,
    val companyRepository: CompanyRepository,
    val employeeRepository: EmployeeRepository,
    val projectRepository: ProjectRepository,
    val participantRepository: ParticipantRepository,
    val projectCraftRepository: ProjectCraftRepository,
    val rfvCustomizationRepository: RfvCustomizationRepository,
    val workAreaRepository: WorkAreaRepository,
    val workAreaListRepository: WorkAreaListRepository,
    val taskRepository: TaskRepository,
    val taskAttachmentRepository: TaskAttachmentRepository,
    val taskScheduleRepository: TaskScheduleRepository,
    val dayCardRepository: DayCardRepository,
    val topicRepository: TopicRepository,
    val topicAttachmentRepository: TaskAttachmentRepository,
    val messageRepository: MessageRepository,
    val messageAttachmentRepository: TaskAttachmentRepository,
    val milestoneRepository: MilestoneRepository,
    val notificationRepository: NotificationRepository
) {

  fun truncateDatabase() {
    notificationRepository.deleteAll()
    milestoneRepository.deleteAll()
    messageAttachmentRepository.deleteAll()
    messageRepository.deleteAll()
    topicAttachmentRepository.deleteAll()
    topicRepository.deleteAll()
    dayCardRepository.deleteAll()
    taskScheduleRepository.deleteAll()
    taskAttachmentRepository.deleteAll()
    taskRepository.deleteAll()
    workAreaListRepository.deleteAll()
    workAreaRepository.deleteAll()
    rfvCustomizationRepository.deleteAll()
    projectCraftRepository.deleteAll()
    participantRepository.deleteAll()
    projectRepository.deleteAll()
    employeeRepository.deleteAll()
    companyRepository.deleteAll()
    userRepository.deleteAll()
  }
}
