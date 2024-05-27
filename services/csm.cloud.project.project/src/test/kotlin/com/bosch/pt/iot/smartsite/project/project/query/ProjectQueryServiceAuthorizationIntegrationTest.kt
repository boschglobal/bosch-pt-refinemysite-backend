/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.project.query

import com.bosch.pt.csm.cloud.common.messages.IsoCountryCodeEnumAvro
import com.bosch.pt.csm.cloud.common.model.IsoCountryCodeEnum.US
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitEmployee
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeRoleEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantStatusEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.iot.smartsite.application.security.AuthorizationTestUtils.authorizeWithUser
import com.bosch.pt.iot.smartsite.common.authorization.AbstractAuthorizationIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.UserTypeAccess
import com.bosch.pt.iot.smartsite.user.authorization.model.UserCountryRestriction
import com.bosch.pt.iot.smartsite.user.authorization.repository.UserCountryRestrictionRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Pageable

@DisplayName("Test authorization of Project Query Service")
class ProjectQueryServiceAuthorizationIntegrationTest : AbstractAuthorizationIntegrationTestV2() {

  @Autowired private lateinit var userCountryRestrictionRepository: UserCountryRestrictionRepository

  @Autowired private lateinit var cut: ProjectQueryService

  @ParameterizedTest
  @MethodSource("adminOnly")
  fun `search for all projects only allowed for admin users`(userType: UserTypeAccess) {
    checkAccessWith(userType) {
      cut.searchProjectsForFilters(null, null, null, Pageable.unpaged()).also {
        assertThat(it.content).hasSize(2)
      }
    }
  }

  @Test
  fun `search for all projects returns authorized items only`() {
    eventStreamGenerator
        .submitUser(asReference = "newUser") { it.country = IsoCountryCodeEnumAvro.US }
        .submitEmployee(asReference = "newUserEmployeeOtherCompanyCr") {
          it.user = getByReference("newUser")
          it.roles = listOf(EmployeeRoleEnumAvro.CR)
        }
        .setUserContext(name = "newUser")
        .submitProject(asReference = "newProject")
        .submitParticipantG3(asReference = "newUserParticipantCsm") {
          it.user = getByReference("newUser")
          it.company = getByReference("otherCompany")
          it.role = ParticipantRoleEnumAvro.CSM
          it.status = ParticipantStatusEnumAvro.ACTIVE
        }

    authorizeWithUser(userAdmin, true)
    cut.searchProjectsForFilters(null, null, null, Pageable.unpaged()).also {
      assertThat(it.content).hasSize(3)
    }

    authorizeWithUser(userAdmin, true)
    cut.searchProjectsForFilters(null, null, null, Pageable.unpaged()).also {
      assertThat(it.content).hasSize(3)
    }

    userCountryRestrictionRepository.save(UserCountryRestriction(userAdmin.identifier!!, US))
    cut.searchProjectsForFilters(null, null, null, Pageable.unpaged()).also {
      assertThat(it.content).hasSize(1)
    }
  }
}
