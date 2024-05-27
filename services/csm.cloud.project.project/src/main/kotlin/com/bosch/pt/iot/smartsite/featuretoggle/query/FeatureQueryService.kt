/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.featuretoggle.query

import com.bosch.pt.csm.cloud.featuretogglemanagement.common.SubjectTypeEnum.COMPANY
import com.bosch.pt.csm.cloud.featuretogglemanagement.common.SubjectTypeEnum.PROJECT
import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.domain.WhitelistedSubject
import com.bosch.pt.iot.smartsite.company.api.CompanyId
import com.bosch.pt.iot.smartsite.company.api.asCompanyId
import com.bosch.pt.iot.smartsite.project.participant.shared.repository.ParticipantAuthorizationRepository
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service

@Service
open class FeatureQueryService(
    private val featureQueryService:
        com.bosch.pt.csm.cloud.featuretogglemanagement.feature.query.FeatureQueryService,
    private val participantAuthorizationRepository: ParticipantAuthorizationRepository
) {

  @PreAuthorize(
      "@projectAuthorizationComponent.hasReadPermissionOnProjectIncludingAdmin(#projectIdentifier)")
  open fun isFeatureEnabled(feature: FeatureEnum, projectIdentifier: ProjectId) =
      isFeatureEnabledForProject(feature, projectIdentifier) ||
          // note: for an admin, there is no participant
          getCurrentParticipant(projectIdentifier)?.let {
            isFeatureEnabledForCompany(feature, it.companyIdentifier.asCompanyId())
          } == true

  private fun isFeatureEnabledForProject(feature: FeatureEnum, projectIdentifier: ProjectId) =
      featureQueryService.isFeatureEnabled(
          feature.name, WhitelistedSubject(projectIdentifier.toString(), PROJECT))

  private fun isFeatureEnabledForCompany(feature: FeatureEnum, companyIdentifier: CompanyId) =
      featureQueryService.isFeatureEnabled(
          feature.name, WhitelistedSubject(companyIdentifier.toString(), COMPANY))

  private fun getCurrentParticipant(projectIdentifier: ProjectId) =
      participantAuthorizationRepository.getParticipantOfCurrentUser(projectIdentifier)
}
