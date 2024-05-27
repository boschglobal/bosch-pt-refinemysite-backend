/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.project.shared.boundary

import com.bosch.pt.iot.smartsite.common.authorization.AbstractAuthorizationIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.UserTypeAccess
import com.bosch.pt.iot.smartsite.common.facade.rest.UserTypeAccess.Companion.createGrantedGroup
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import java.util.stream.Stream
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("Test authorization in Project Delete Service")
class ProjectDeleteServiceAuthorizationIntegrationTest : AbstractAuthorizationIntegrationTestV2() {

  @Autowired private lateinit var cut: ProjectDeleteService

  @ParameterizedTest
  @MethodSource("projectDeletePermissionGroup")
  @DisplayName("delete permission is granted")
  fun verifyDeleteProjectAuthorized(userType: UserTypeAccess) {
    checkAccessWith(userType) { cut.markAsDeletedAndSendEvent(project.identifier) }
  }

  @ParameterizedTest
  @MethodSource("noOneWithoutAdminAccess")
  @DisplayName("delete permission is denied in case invalid project")
  fun verifyDeleteProjectNotAuthorizedForInvalidProject(userType: UserTypeAccess) {
    checkAccessWith(userType) { cut.markAsDeletedAndSendEvent(ProjectId()) }
  }

  companion object {

    @JvmStatic
    fun projectDeletePermissionGroup(): Stream<UserTypeAccess> =
        createGrantedGroup(userTypes, setOf(CSM, OTHER_CSM, ADMIN))
  }
}
