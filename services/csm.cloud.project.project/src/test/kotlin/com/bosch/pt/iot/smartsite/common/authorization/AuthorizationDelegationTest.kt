/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.common.authorization

import com.bosch.pt.iot.smartsite.common.authorization.AuthorizationDelegation.delegateAuthorizationForIdentifiers
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.project.shared.model.ProjectBuilder.Companion.project
import com.bosch.pt.iot.smartsite.project.projectcraft.shared.model.ProjectCraftBuilder.Companion.projectCraft
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AuthorizationDelegationTest {

  companion object {
    private val AUTHORIZED_PROJECT = project().build()

    private val UNAUTHORIZED_PROJECT = project().build()

    private val AUTHORIZED_PROJECT_CRAFT = projectCraft().withProject(AUTHORIZED_PROJECT).build()

    private val UNAUTHORIZED_PROJECT_CRAFT =
        projectCraft().withProject(UNAUTHORIZED_PROJECT).build()

    private val AUTHORIZATION_DELEGATIONS =
        setOf(
            AuthorizationDelegationDto(
                AUTHORIZED_PROJECT_CRAFT.identifier.toUuid(),
                AUTHORIZED_PROJECT.identifier.toUuid()),
            AuthorizationDelegationDto(
                UNAUTHORIZED_PROJECT_CRAFT.identifier.toUuid(),
                UNAUTHORIZED_PROJECT.identifier.toUuid()))
  }

  @Test
  fun delegateAuthorizationForIdentifierCollection_returnsOnlyAuthorized() {

    assertThat(
            delegateAuthorizationForIdentifiers(
                listOf(
                    AUTHORIZED_PROJECT_CRAFT.identifier.toUuid(),
                    UNAUTHORIZED_PROJECT_CRAFT.identifier.toUuid()),
                { mapDelegationSourceToDelegationTarget(it) },
                { authorizeProjectIdentifiers(it) }))
        .containsOnly(AUTHORIZED_PROJECT_CRAFT.identifier.toUuid())
  }

  @Test
  fun delegateAuthorizationForIdentifierCollection_emptyArgumentReturnsEmptyCollection() {
    assertThat(
            delegateAuthorizationForIdentifiers(
                emptyList(),
                { mapDelegationSourceToDelegationTarget(it) },
                { authorizeProjectIdentifiers(it) }))
        .isEmpty()
  }

  private fun authorizeProjectIdentifiers(projectIdentifiers: Set<UUID>): Set<UUID> =
      projectIdentifiers.filter { it.asProjectId() == AUTHORIZED_PROJECT.identifier }.toSet()

  private fun mapDelegationSourceToDelegationTarget(
      sourceIdentifiers: Collection<UUID>
  ): Set<AuthorizationDelegationDto> =
      AUTHORIZATION_DELEGATIONS.filter { sourceIdentifiers.contains(it.sourceIdentifier) }.toSet()
}
