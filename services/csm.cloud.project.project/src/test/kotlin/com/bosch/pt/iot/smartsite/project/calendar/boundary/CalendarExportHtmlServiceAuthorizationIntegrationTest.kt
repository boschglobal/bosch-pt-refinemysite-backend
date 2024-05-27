/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.calendar.boundary

import com.bosch.pt.iot.smartsite.common.authorization.AbstractAuthorizationIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.UserTypeAccess
import com.bosch.pt.iot.smartsite.project.calendar.api.CalendarExportParameters
import java.time.LocalDate.now
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

class CalendarExportHtmlServiceAuthorizationIntegrationTest :
    AbstractAuthorizationIntegrationTestV2() {

  @Autowired private lateinit var cut: CalendarExportHtmlService

  @ParameterizedTest
  @MethodSource("allActiveParticipantsWithAccess")
  fun `verify export calendar permission is granted to`(userType: UserTypeAccess) {
    checkAccessWith(userType) {
      cut.generateHtml(CalendarExportParameters(from = now(), to = now()), project.identifier)
    }
  }
}
