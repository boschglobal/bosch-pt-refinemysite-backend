/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement

import com.bosch.pt.csm.cloud.projectmanagement.statistics.repository.DayCardRepository
import com.bosch.pt.csm.cloud.projectmanagement.statistics.repository.NamedObjectRepository
import com.bosch.pt.csm.cloud.projectmanagement.statistics.repository.ObjectRelationRepository
import com.bosch.pt.csm.cloud.projectmanagement.statistics.repository.ParticipantMappingRepository
import com.bosch.pt.csm.cloud.projectmanagement.statistics.repository.RfvCustomizationRepository
import com.bosch.pt.csm.cloud.projectmanagement.statistics.repository.UserRepository
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service

@Service
@Lazy
class Repositories(
    val dayCardRepository: DayCardRepository,
    val namesObjectRepository: NamedObjectRepository,
    val objectRelationRepository: ObjectRelationRepository,
    val participantMappingRepository: ParticipantMappingRepository,
    val rfvCustomizationRepository: RfvCustomizationRepository,
    val userRepository: UserRepository
) {
  fun truncateDatabase() {
    dayCardRepository.deleteAll()
    namesObjectRepository.deleteAll()
    objectRelationRepository.deleteAll()
    participantMappingRepository.deleteAll()
    rfvCustomizationRepository.deleteAll()
    userRepository.deleteAll()
  }
}
