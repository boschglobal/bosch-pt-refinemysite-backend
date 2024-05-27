/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.copy.boundary

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitEmployee
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeRoleEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.iot.smartsite.application.security.AuthorizationTestUtils.authorizeWithUser
import com.bosch.pt.iot.smartsite.common.authorization.AbstractAuthorizationIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.UserTypeAccess
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.AccessDeniedException

class ProjectCopyServiceAuthorizationIntegrationTest : AbstractAuthorizationIntegrationTestV2() {

  @Autowired private lateinit var cut: ProjectCopyService

  private val projectId by lazy { getIdentifier("project").asProjectId() }

  @BeforeEach
  fun init() {
    eventStreamGenerator.setupDatasetTestData()
    projectEventStoreUtils.reset()
  }

  @ParameterizedTest
  @MethodSource("csmWithAccess")
  fun `verify copy authorized`(userType: UserTypeAccess) {
    checkAccessWith(userType) {
      cut.copy(projectId, ProjectCopyParameters("My new copied project"))
    }
  }

  @Test
  fun `verify copy authorized for CSM Employee who is a CSM Participant`() {
    eventStreamGenerator
        .submitUser(asReference = "copyUser")
        .submitEmployee(asReference = "copyEmployeeCsm") {
          it.roles = listOf(EmployeeRoleEnumAvro.CSM)
        }
        .submitParticipantG3(asReference = "copyParticipantFm") {
          it.role = ParticipantRoleEnumAvro.CSM
        }

    authorizeWithUser(repositories.findUser(getIdentifier("copyUser"))!!)

    cut.copy(projectId, ProjectCopyParameters("My new copied project"))
  }

  @Test
  fun `verify copy authorized for admin who is a CSM Employee and CSM participant`() {
    eventStreamGenerator
        .submitEmployee(asReference = "copyAdminEmployee") {
          it.user = getByReference("admin")
          it.roles = listOf(EmployeeRoleEnumAvro.CSM)
        }
        .submitParticipantG3(asReference = "copyAdminParticipant") {
          it.user = getByReference("admin")
          it.role = ParticipantRoleEnumAvro.CSM
        }

    authorizeWithUser(repositories.findUser(getIdentifier("admin"))!!)

    cut.copy(projectId, ProjectCopyParameters("My new copied project"))
  }

  @Test
  fun `verify copy unauthorized for CSM Employee who is not a CSM Participant`() {
    eventStreamGenerator
        .submitUser(asReference = "copyUser")
        .submitEmployee(asReference = "copyEmployeeCsm") {
          it.roles = listOf(EmployeeRoleEnumAvro.CSM)
        }
        .submitParticipantG3(asReference = "copyParticipantFm") {
          it.role = ParticipantRoleEnumAvro.FM
        }

    authorizeWithUser(repositories.findUser(getIdentifier("copyUser"))!!)

    assertThatExceptionOfType(AccessDeniedException::class.java).isThrownBy {
      cut.copy(projectId, ProjectCopyParameters("My new copied project"))
    }
  }

  @Test
  fun `verify copy unauthorized for CSM Participant who is not a CSM Employee`() {
    eventStreamGenerator
        .submitUser(asReference = "copyUser")
        .submitEmployee(asReference = "copyEmployeeCr") {
          it.roles = listOf(EmployeeRoleEnumAvro.CR)
        }
        .submitParticipantG3(asReference = "copyParticipantCsm") {
          it.role = ParticipantRoleEnumAvro.CSM
        }

    authorizeWithUser(repositories.findUser(getIdentifier("copyUser"))!!)

    assertThatExceptionOfType(AccessDeniedException::class.java).isThrownBy {
      cut.copy(projectId, ProjectCopyParameters("My new copied project"))
    }
  }
}
