/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.common.facade.rest.resource.response

import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReference
import com.bosch.pt.iot.smartsite.common.model.ResourceReferenceAssembler.referTo
import com.bosch.pt.iot.smartsite.user.model.User.Companion.getDisplayName
import java.util.UUID.randomUUID
import java.util.function.Supplier
import org.apache.commons.lang3.RandomStringUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ResourceReferenceTest {

  private val deletedUserReference = ResourceReference(randomUUID(), RandomStringUtils.random(10))

  @Test
  fun verifyBuildUserResourceReferenceReturnsDeletedUserReference() {
    val cut =
        referTo(
            randomUUID(),
            getDisplayName(FIRST_NAME, LAST_NAME)!!,
            Supplier { deletedUserReference },
            true)

    assertThat(cut).isEqualTo(deletedUserReference)
  }

  @Test
  fun verifyBuildUserResourceReferenceReturnsFirstName() {
    val cut =
        referTo(
            randomUUID(),
            getDisplayName(FIRST_NAME, null)!!,
            Supplier { deletedUserReference },
            false)

    assertThat(cut.displayName).isEqualTo(FIRST_NAME)
  }

  @Test
  fun verifyBuildUserResourceReferenceReturnsLastName() {
    val cut =
        referTo(
            randomUUID(),
            getDisplayName(null, LAST_NAME)!!,
            Supplier { deletedUserReference },
            false)

    assertThat(cut.displayName).isEqualTo(LAST_NAME)
  }

  companion object {
    private const val FIRST_NAME = "Max"
    private const val LAST_NAME = "Musternmann"
  }
}
