/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2017 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.common.model

import com.bosch.pt.iot.smartsite.common.model.Sortable.Companion.get
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantRoleEnum
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantRoleEnum.CSM
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test

class SortableTest {

  @Test
  fun verifyGetAtPosition() {
    val role = get(ParticipantRoleEnum::class.java, 100)
    assertThat(role).isEqualTo(CSM)
  }

  /** Verifies that exception is thrown if value cannot be found. */
  @Test
  fun verifyGetException() {
    assertThatExceptionOfType(IllegalArgumentException::class.java)
        .isThrownBy { get(ParticipantRoleEnum::class.java, -1) }
        .withMessage(
            "Cannot find enum of type " + ParticipantRoleEnum::class.java.name + " for position -1")
  }
}
