/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.facade.rest.resource.request

import java.util.UUID.randomUUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class UpdateBatchRequestResourceTest {
  @Test
  fun verifyGetIdentifiers() {
    val identifierOne = VersionedIdentifier(randomUUID(), 0L)
    val identifierTwo = VersionedIdentifier(randomUUID(), 0L)

    val cut = UpdateBatchRequestResource(setOf(identifierOne, identifierTwo))

    assertThat(cut.getIdentifiers()).containsExactlyInAnyOrder(identifierOne.id, identifierTwo.id)
  }

  @Test
  fun verifyGetIdentifiersNotNullForNullItems() {
    val cut = UpdateBatchRequestResource<VersionedIdentifier>()

    assertThat(cut.getIdentifiers()).isNotNull
    assertThat(cut.getIdentifiers()).isEmpty()
  }

  @Test
  fun verifyGetIdentifiersNotNullForEmptyItems() {
    val cut = UpdateBatchRequestResource(emptyList<VersionedIdentifier>())

    assertThat(cut.getIdentifiers()).isNotNull
    assertThat(cut.getIdentifiers()).isEmpty()
  }
}
