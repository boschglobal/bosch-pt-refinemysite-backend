/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.daycard.command.handler.status

import com.bosch.pt.iot.smartsite.common.authorization.AbstractAuthorizationIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.UserTypeAccess
import com.bosch.pt.iot.smartsite.common.facade.rest.UserTypeAccess.Companion.createGrantedGroup
import java.util.stream.Stream

@Suppress("UnnecessaryAbstractClass")
abstract class AbstractDayCardAuthorizationTest : AbstractAuthorizationIntegrationTestV2() {

  companion object {
    @JvmStatic
    protected fun dayCardReviewPermissionGroupWithAccess(): Stream<UserTypeAccess> =
        createGrantedGroup(userTypes, setOf(CSM, OTHER_CSM))

    @JvmStatic
    protected fun dayCardUpdatePermissionGroupWithAccess(): Stream<UserTypeAccess> =
        createGrantedGroup(userTypes, setOf(FM_ASSIGNEE, CSM, OTHER_CSM, CR))
  }
}
