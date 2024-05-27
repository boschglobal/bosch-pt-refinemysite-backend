/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.consents.consents.command.snapshotstore

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.extensions.toLocalDateTimeByMillis
import com.bosch.pt.csm.cloud.usermanagement.consents.document.DocumentVersionId
import com.bosch.pt.csm.cloud.usermanagement.consents.document.shared.model.DocumentVersion
import java.time.LocalDate
import java.time.LocalDateTime
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ConsentsUserSnapshotTest {
  private var consentsUser =
      ConsentsUserSnapshot(0L.toLocalDateTimeByMillis(), mutableListOf(), UserId(), 0)

  private val latestVersion = DocumentVersion(DocumentVersionId(), LocalDateTime.now())

  @Test
  fun `hasGivenConsent returns false for not given consent`() {
    assertThat(consentsUser.hasGivenConsent(latestVersion.identifier)).isFalse
  }

  @Test
  fun `hasGivenConsent returns true after consent is given`() {
    consentsUser =
        consentsUser.copy(
            consents =
                consentsUser.consents + UserConsent(LocalDate.now(), latestVersion.identifier))

    assertThat(consentsUser.hasGivenConsent(latestVersion.identifier)).isTrue
  }
}
