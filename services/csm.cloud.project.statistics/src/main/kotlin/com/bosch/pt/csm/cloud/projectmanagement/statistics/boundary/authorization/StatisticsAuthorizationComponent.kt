/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement.statistics.boundary.authorization

import com.bosch.pt.csm.cloud.projectmanagement.application.security.AuthorizationUtils.getCurrentUser
import com.bosch.pt.csm.cloud.projectmanagement.application.security.AuthorizationUtils.hasRoleAdmin
import com.bosch.pt.csm.cloud.projectmanagement.statistics.repository.ParticipantMappingRepository
import java.util.UUID
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component("statisticsAuthorizationComponent")
class StatisticsAuthorizationComponent
@Autowired
constructor(private val participantMappingRepository: ParticipantMappingRepository) {

  fun hasViewPermissionOnProject(projectIdentifier: UUID) =
      hasRoleAdmin() ||
          participantMappingRepository.findOneByProjectIdentifierAndUserIdentifierAndActiveTrue(
                  projectIdentifier, getCurrentUser().identifier)
              .let {
                return it != null && it.active
              }
}
