/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.common.facade.rest.resource.response

import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReference
import com.bosch.pt.csm.common.model.ResourceReferenceAssembler.referTo
import java.util.UUID.randomUUID
import org.apache.commons.lang3.RandomStringUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ResourceReferenceTest {
  private lateinit var deletedUserReference: ResourceReference

  @BeforeEach
  fun setUp() {
    deletedUserReference = ResourceReference(randomUUID(), RandomStringUtils.random(10))
  }

  @Test
  fun `verify build user resource reference returns deleted user reference`() {
    val cut = referTo(null) { deletedUserReference }
    assertThat(cut).isEqualTo(deletedUserReference)
  }
}
