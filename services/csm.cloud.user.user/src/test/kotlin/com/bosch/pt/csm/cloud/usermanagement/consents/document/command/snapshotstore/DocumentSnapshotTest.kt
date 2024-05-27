/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.consents.document.command.snapshotstore

import com.bosch.pt.csm.cloud.common.model.IsoCountryCodeEnum.DE
import com.bosch.pt.csm.cloud.usermanagement.consents.document.ClientSet
import com.bosch.pt.csm.cloud.usermanagement.consents.document.DocumentId
import com.bosch.pt.csm.cloud.usermanagement.consents.document.DocumentType.TERMS_AND_CONDITIONS
import com.bosch.pt.csm.cloud.usermanagement.consents.document.DocumentVersion
import com.bosch.pt.csm.cloud.usermanagement.consents.document.DocumentVersionId
import java.net.URL
import java.time.LocalDateTime
import java.time.LocalDateTime.now
import java.util.Locale
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.junit.jupiter.api.Test

class DocumentSnapshotTest {

  @Test
  fun `latestVersion throws IllegalArgumentException if no version exists`() {
    val document = documentWithVersions()

    assertThatIllegalStateException().isThrownBy { document.latestVersion() }
  }

  @Test
  fun `latestVersion returns not null if one version exist`() {
    val document = documentWithVersions(now())

    val latestVersion = document.latestVersion()

    assertThat(latestVersion).isNotNull
  }

  @Test
  fun `latestVersion returns latest version if multiple versions exists`() {
    val lastChanged = now().plusDays(5)
    val document = documentWithVersions(now().minusDays(5), lastChanged, now())

    val latestVersion = document.latestVersion()

    assertThat(latestVersion.lastChanged).isEqualTo(lastChanged)
  }

  private fun documentWithVersions(vararg dates: LocalDateTime) =
      DocumentSnapshot(
          "Terms & Conditions",
          URL("https://test.com/terms"),
          TERMS_AND_CONDITIONS,
          DE,
          Locale.ENGLISH,
          ClientSet.ALL,
          dates.map { DocumentVersion(DocumentVersionId(), it) },
          DocumentId(),
          0)
}
