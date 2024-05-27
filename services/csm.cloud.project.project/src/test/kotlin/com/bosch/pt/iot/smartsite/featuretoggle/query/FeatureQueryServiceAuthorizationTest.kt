/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.featuretoggle.query

import com.bosch.pt.iot.smartsite.common.authorization.AbstractAuthorizationIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.UserTypeAccess
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

class FeatureQueryServiceAuthorizationTest : AbstractAuthorizationIntegrationTestV2() {

  @Autowired private lateinit var cut: FeatureQueryService

  @ParameterizedTest
  @MethodSource("allActiveParticipantsWithAccessAndAdmin")
  @DisplayName("read permission is granted to")
  fun `verify reading enabled features is authorized`(userType: UserTypeAccess) =
      checkAccessWith(userType) {
        cut.isFeatureEnabled(FeatureEnum.PROJECT_IMPORT, project.identifier)
      }
}
