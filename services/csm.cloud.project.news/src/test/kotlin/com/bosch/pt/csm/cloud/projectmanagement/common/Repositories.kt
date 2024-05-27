/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.common

import com.bosch.pt.csm.cloud.projectmanagement.common.repository.ObjectRelationRepository
import com.bosch.pt.csm.cloud.projectmanagement.news.repository.NewsRepository
import com.bosch.pt.csm.cloud.projectmanagement.project.repository.ParticipantMappingRepository
import com.bosch.pt.csm.cloud.projectmanagement.user.repository.UserRepository
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service

@Service
@Lazy
class Repositories(
    val userRepository: UserRepository,
    val newsRepository: NewsRepository,
    val objectRelationRepository: ObjectRelationRepository,
    val participantMappingRepository: ParticipantMappingRepository
) {
  fun truncateDatabase() {
    newsRepository.deleteAll()
    objectRelationRepository.deleteAll()
    participantMappingRepository.deleteAll()
    userRepository.deleteAll()
  }
}
