/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.company.employee.command.eventprocessor

import com.bosch.pt.csm.cloud.common.api.asUserId
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitCompany
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitEmployee
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUserTombstones
import com.bosch.pt.csm.common.AbstractRestoreIntegrationTest
import com.bosch.pt.csm.company.employee.asEmployeeId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DeleteEmployeeOnUserDeletedEventListenerTest : AbstractRestoreIntegrationTest() {

  @Test
  fun `validate that employee is deleted after corresponding user deleted event`() {
    eventStreamGenerator.submitUser("daniel").submitCompany().submitEmployee("danielEmployee")

    repositories.employeeRepository
        .findOneWithDetailsByIdentifier(
            eventStreamGenerator.getIdentifier("danielEmployee").asEmployeeId())
        .also { assertThat(it).isNotNull }

    getContext().useOnlineListener()
    eventStreamGenerator.submitUserTombstones("daniel")

    repositories.employeeRepository
        .findOneWithDetailsByIdentifier(
            eventStreamGenerator.getIdentifier("danielEmployee").asEmployeeId())
        .also { assertThat(it).isNull() }
  }

  @Test
  fun `validate that a user can be deleted that is not assigned as employee`() {
    eventStreamGenerator.submitUser("daniel")
    val userId = getIdentifier("daniel").asUserId()

    getContext().useOnlineListener()
    eventStreamGenerator.submitUserTombstones("daniel")

    repositories.userProjectionRepository.findOneById(userId).also { assertThat(it).isNull() }
  }
}
