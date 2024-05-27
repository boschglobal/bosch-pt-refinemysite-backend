/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.security

import com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication.token.model.PatTokenParser
import java.text.ParseException
import java.util.UUID.randomUUID
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test

class PatTokenParserTest {

  @Test
  fun `empty token`() {
    assertThat(PatTokenParser.parse("")).isNull()
  }

  @Test
  fun `invalid format`() {
    assertThatExceptionOfType(ParseException::class.java)
        .isThrownBy { PatTokenParser.parse("abc") }
        .withMessage("Missing dot delimiter(s)")
  }

  @Test
  fun `wrong number of parts`() {
    assertThatExceptionOfType(IllegalArgumentException::class.java)
        .isThrownBy { PatTokenParser.parse("a.b.c.d") }
        .withMessage("Invalid number of pat components")
  }

  @Test
  fun `pat id to short`() {
    assertThatExceptionOfType(ParseException::class.java)
        .isThrownBy { PatTokenParser.parse("a.b.c") }
        .withMessage("Invalid pat format")
  }

  @Test
  fun `pat id no uuid`() {
    assertThatExceptionOfType(ParseException::class.java)
        .isThrownBy { PatTokenParser.parse("a.xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.c") }
        .withMessage("Invalid pat identifier")
  }

  @Test
  fun `parse valid pat id`() {
    val expectedUuid = randomUUID()
    val patToken = PatTokenParser.parse("a.${expectedUuid.toString().replace("-", "")}.c")
    assertThat(patToken).isNotNull
    assertThat(patToken!!.patId).isEqualTo(expectedUuid)
    assertThat(patToken.type).isEqualTo("a")
    assertThat(patToken.secret).isEqualTo("c")
  }
}
