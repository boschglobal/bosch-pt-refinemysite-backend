/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.featuretoggle.whitelist.shared.model

import com.bosch.pt.csm.cloud.featuretogglemanagement.common.SubjectTypeEnum.COMPANY
import com.bosch.pt.csm.cloud.featuretogglemanagement.common.SubjectTypeEnum.PROJECT
import java.util.UUID.randomUUID
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test

class WhitelistedSubjectTest {

  @Test
  fun `has equality with itself`() {
    val entity = WhitelistedSubject()
    assertThat(entity).isEqualTo(entity)
  }

  @Test
  fun `has no equality if values differ regardless of identifier`() {
    val sameIdentifier = randomUUID()
    val entity1 =
        WhitelistedSubject().apply {
          subjectRef = sameIdentifier
          featureName = "a"
          type = COMPANY
        }
    val entity2 =
        WhitelistedSubject().apply {
          subjectRef = sameIdentifier
          featureName = "b"
          type = PROJECT
        }

    val entity3 =
        WhitelistedSubject().apply {
          subjectRef = sameIdentifier
          featureName = "c"
          type = PROJECT
        }

    assertThat(entity1).isNotEqualTo(entity2)
    assertThat(entity2).isNotEqualTo(entity1)
    assertThat(entity1.hashCode()).isNotEqualTo(entity2.hashCode())
    assertThat(entity2).isNotEqualTo(entity3)
    assertThat(entity2.hashCode()).isNotEqualTo(entity3.hashCode())
  }

  @Test
  fun `has no equality with different identifiers`() {
    val entity1 = WhitelistedSubject().apply { subjectRef = randomUUID() }
    val entity2 = WhitelistedSubject().apply { subjectRef = randomUUID() }

    assertThat(entity1).isNotEqualTo(entity2)
    assertThat(entity2).isNotEqualTo(entity1)
  }

  @Test
  fun `has no equality for different object type`() {
    val entity = WhitelistedSubject()

    assertThat(entity).isNotEqualTo("notAnEntity")
  }

  @Test
  fun `has correctly set properties when populated`() {
    val entity = WhitelistedSubject(randomUUID(), COMPANY, "featureName")
    assertThat(entity.type).isEqualTo(COMPANY)
    assertThat(entity.featureName).isEqualTo("featureName")
  }

  @Test
  fun `throws exception when constructed without mandatory values`() {
    val entity = WhitelistedSubject()
    assertThatExceptionOfType(UninitializedPropertyAccessException::class.java).isThrownBy {
      entity.featureName
    }
    assertThatExceptionOfType(UninitializedPropertyAccessException::class.java).isThrownBy {
      entity.type
    }
    assertThatExceptionOfType(UninitializedPropertyAccessException::class.java).isThrownBy {
      entity.subjectRef
    }
  }
}
