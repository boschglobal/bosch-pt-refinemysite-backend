/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.relation.boundary

import com.bosch.pt.iot.smartsite.common.authorization.AbstractAuthorizationIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.UserTypeAccess
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Pageable.unpaged

class RelationSearchServiceAuthorizationIntegrationTest : AbstractAuthorizationIntegrationTestV2() {

  @Autowired private lateinit var cut: RelationSearchService

  @ParameterizedTest
  @MethodSource("allActiveParticipantsWithAccess")
  fun `verify search relations is authorized for`(userType: UserTypeAccess) =
      checkAccessWith(userType) {
        cut.search(project.identifier, emptySet(), emptySet(), emptySet(), unpaged())
      }

  @ParameterizedTest
  @MethodSource("noOneWithAccess")
  fun `verify search relations for non-existing project is not authorized for`(
      userType: UserTypeAccess
  ) =
      checkAccessWith(userType) {
        cut.search(ProjectId(), emptySet(), emptySet(), emptySet(), unpaged())
      }
}
