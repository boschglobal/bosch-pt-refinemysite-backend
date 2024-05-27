/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.pat.pat.command.handler

import com.bosch.pt.csm.cloud.common.api.asUserId
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.command.api.asPatId
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.command.handler.CreatePatCommandHandler.GeneratedPat
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.shared.model.PatTypeEnum
import java.util.UUID
import java.util.UUID.randomUUID
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.springframework.security.crypto.bcrypt.BCrypt

class GeneratedPatTest {

  @Test
  fun `token contains type as first segment`() {
    val cut =
        GeneratedPat(
            patId = randomUUID().asPatId(),
            type = PatTypeEnum.RMSPAT1,
            impersonatedUser = randomUUID().asUserId(),
        )

    val segments = cut.value.split(".")
    assertEquals("RMSPAT1", segments[0])
  }

  @Test
  fun `token contains PAT ID stripped of its dashes as second segment`() {
    val cut =
        GeneratedPat(
            patId = randomUUID().asPatId(),
            type = PatTypeEnum.RMSPAT1,
            impersonatedUser = randomUUID().asUserId(),
        )

    val segments = cut.value.split(".")
    val patIdSegment = segments[1]
    val patIdSegmentWithDashes =
        patIdSegment.replaceFirst(
            Regex("([0-9a-fA-F]{8})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]+)"),
            "$1-$2-$3-$4-$5")

    assertEquals(cut.patId, UUID.fromString(patIdSegmentWithDashes).asPatId())
  }

  @Test
  fun `token contains a secret portion of at least 32 characters as third segment`() {
    val cut =
        GeneratedPat(
            patId = randomUUID().asPatId(),
            type = PatTypeEnum.RMSPAT1,
            impersonatedUser = randomUUID().asUserId(),
        )

    val segments = cut.value.split(".")
    val secretSegment = segments[2]
    assertEquals(32, secretSegment.length)

    assertTrue(BCrypt.checkpw(cut.value, cut.hash))
  }

  @Test
  fun `token is verifiable using a bcrypt hash of the whole token value`() {
    val cut =
        GeneratedPat(
            patId = randomUUID().asPatId(),
            type = PatTypeEnum.RMSPAT1,
            impersonatedUser = randomUUID().asUserId(),
        )

    assertTrue(BCrypt.checkpw(cut.value, cut.hash))
  }
}
