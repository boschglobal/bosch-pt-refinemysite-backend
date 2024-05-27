/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.consents.document.command.mapper

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class DocumentVersionIncrementedAvroEventMapperTest {

  private val mapper = DocumentVersionIncrementedAvroEventMapper()

  @Test
  fun `mapToKey throws IllegalArgumentException for unsupported event type`() {
    assertThrows<IllegalArgumentException> { mapper.mapToKey(UnsupportedEventType(), 1) }
  }

  @Test
  fun `mapToValue throws IllegalArgumentException for unsupported event type`() {
    assertThrows<IllegalArgumentException> { mapper.mapToValue(UnsupportedEventType(), 1) }
  }

  private class UnsupportedEventType
}
