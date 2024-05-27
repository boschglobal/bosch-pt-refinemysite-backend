/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.milestone.facade.rest

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitEmployee
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeRoleEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro.CSM
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUserTombstones
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.project.milestone.domain.MilestoneId
import com.bosch.pt.iot.smartsite.project.milestone.domain.asMilestoneId
import com.bosch.pt.iot.smartsite.user.facade.rest.resource.factory.ProfilePictureUriBuilder.Companion.buildDefaultProfilePictureUri
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.AccessDeniedException

@EnableAllKafkaListeners
class MilestoneFindIntegrationTest : AbstractMilestoneIntegrationTest() {

  @Autowired lateinit var cut: MilestoneController

  @Test
  fun `verify creator field contains default profile picture for deleted user reference`() {
    eventStreamGenerator
        .submitUser(asReference = "csm-new")
        .submitEmployee(asReference = "csm-new-employee") {
          it.roles = listOf(EmployeeRoleEnumAvro.CSM)
        }
        .submitParticipantG3(asReference = "csm-new-participant") { it.role = CSM }
        .submitUserTombstones(reference = "userCsm1")

    setAuthentication(getIdentifier("csm-new"))

    cut.find(getIdentifier("milestone").asMilestoneId()).apply {
      assertThat(body!!.creator.picture).isEqualTo(buildDefaultProfilePictureUri())
    }
  }

  @Test
  fun `verify access denied if milestone is not found`() {
    assertThatExceptionOfType(AccessDeniedException::class.java).isThrownBy {
      cut.find(MilestoneId())
    }
  }
}
