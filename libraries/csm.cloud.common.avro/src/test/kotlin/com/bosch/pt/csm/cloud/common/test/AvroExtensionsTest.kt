/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.test

import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.messages.AuditingInformationAvro
import java.time.LocalDateTime
import java.util.UUID.randomUUID
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

class AvroExtensionsTest {

  @Test
  fun `non record field on root level`() {
    val expectedIdentifier = randomUUID().toString()
    val record = buildNonNestedTestRecord(expectedIdentifier)

    val identifier: String? = record.getFieldByPath("identifier")
    assertThat(identifier).isEqualTo(expectedIdentifier)
  }

  @Test
  fun `nested field`() {
    val expectedIdentifier = randomUUID().toString()
    val record = buildNestedTestRecord(expectedIdentifier, LocalDateTime.now())

    val createdByIdentifier: String? = record.getFieldByPath("createdBy", "identifier")
    assertThat(createdByIdentifier).isEqualTo(expectedIdentifier)
  }

  @Test
  fun `field on root level not found`() {
    val expectedIdentifier = randomUUID().toString()
    val record = buildNonNestedTestRecord(expectedIdentifier)

    assertThat(record.getFieldByPath<Any?>("unknownField")).isNull()
  }

  @Test
  fun `last nested field not found`() {
    val expectedIdentifier = randomUUID().toString()
    val record = buildNonNestedTestRecord(expectedIdentifier)

    assertThat(record.getFieldByPath<Any?>("identifier", "non-existing-field")).isNull()
  }

  @Test
  fun `root level field with nested field not found`() {
    val expectedIdentifier = randomUUID().toString()
    val record = buildNonNestedTestRecord(expectedIdentifier)

    assertThat(record.getFieldByPath<Any?>("non-existing-field", "identifier")).isNull()
  }

  @Test
  fun `field has unexpected type`() {
    val expectedIdentifier = randomUUID().toString()
    val record = buildNonNestedTestRecord(expectedIdentifier)

    assertThatExceptionOfType(ClassCastException::class.java).isThrownBy {
      // It's required to assign the result to a variable to ensure the cast to fail.
      // Otherwise, the specified generic type is optimized away and the exception is swallowed.
      val result: Long? = record.getFieldByPath("identifier")
      fail("Not expected a result but got: $result")
    }
  }

  private fun buildNestedTestRecord(
      identifier: String,
      date: LocalDateTime
  ): AuditingInformationAvro =
      AuditingInformationAvro.newBuilder()
          .setCreatedBy(buildAggregateIdentifier(identifier))
          .setLastModifiedBy(buildAggregateIdentifier(identifier))
          .setCreatedDate(date.toEpochMilli())
          .setLastModifiedDate(date.toEpochMilli())
          .build()

  private fun buildNonNestedTestRecord(identifier: String) = buildAggregateIdentifier(identifier)

  private fun buildAggregateIdentifier(identifier: String): AggregateIdentifierAvro =
      AggregateIdentifierAvro.newBuilder()
          .setIdentifier(identifier)
          .setVersion(0L)
          .setType("Type")
          .build()
}
