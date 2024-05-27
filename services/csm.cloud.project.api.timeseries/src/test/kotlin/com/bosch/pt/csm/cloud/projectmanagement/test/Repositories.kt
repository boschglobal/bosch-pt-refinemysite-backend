/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.test

import com.bosch.pt.csm.cloud.projectmanagement.company.company.query.repository.CompanyRepository
import com.bosch.pt.csm.cloud.projectmanagement.project.craft.query.repository.ProjectCraftRepository
import com.bosch.pt.csm.cloud.projectmanagement.project.dayCard.query.repository.DayCardRepository
import com.bosch.pt.csm.cloud.projectmanagement.project.milestone.query.repository.MilestoneRepository
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.query.repository.ParticipantRepository
import com.bosch.pt.csm.cloud.projectmanagement.project.project.query.repository.ProjectRepository
import com.bosch.pt.csm.cloud.projectmanagement.project.workarea.query.repository.WorkAreaRepository
import com.bosch.pt.csm.cloud.projectmanagement.project.workarealist.query.repository.WorkAreaListRepository
import com.bosch.pt.csm.cloud.projectmanagement.user.pat.query.repository.PatProjectionRepository
import com.bosch.pt.csm.cloud.projectmanagement.user.user.query.repository.UserProjectionRepository
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service

@Service
@Lazy
class Repositories(
    val companyRepository: CompanyRepository,
    val dayCardRepository: DayCardRepository,
    val milestoneRepository: MilestoneRepository,
    val participantRepository: ParticipantRepository,
    val patRepository: PatProjectionRepository,
    val projectCraftRepository: ProjectCraftRepository,
    val projectRepository: ProjectRepository,
    val userRepository: UserProjectionRepository,
    val workAreaListRepository: WorkAreaListRepository,
    val workAreaRepository: WorkAreaRepository,
) {

  fun truncateDatabase() {
    companyRepository.deleteAll()
    dayCardRepository.deleteAll()
    milestoneRepository.deleteAll()
    participantRepository.deleteAll()
    patRepository.deleteAll()
    projectCraftRepository.deleteAll()
    projectRepository.deleteAll()
    userRepository.deleteAll()
    workAreaListRepository.deleteAll()
    workAreaRepository.deleteAll()
  }
}
