/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.company.employee.command.handler

import com.bosch.pt.csm.common.AbstractAuthorizationIntegrationTest
import com.bosch.pt.csm.common.facade.rest.UserTypeAccess
import com.bosch.pt.csm.company.employee.command.api.DeleteEmployeeCommand
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("Verify authorization of delete employee command handler")
class DeleteEmployeeCommandAuthorizationIntegrationTest : AbstractAuthorizationIntegrationTest() {

  @Autowired private lateinit var cut: DeleteEmployeeCommandHandler

  @ParameterizedTest
  @MethodSource("authorizedAdminOnly")
  fun `delete employee allowed for authorized admins only`(userType: UserTypeAccess) {
    checkAccessWith(userType) {
      assertThat(cut.handle(DeleteEmployeeCommand(employeeDE.identifier))).isNotNull()
    }
  }
}
