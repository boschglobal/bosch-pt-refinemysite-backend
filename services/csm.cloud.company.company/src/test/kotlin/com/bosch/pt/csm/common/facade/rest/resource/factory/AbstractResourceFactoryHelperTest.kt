/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.common.facade.rest.resource.factory

import com.bosch.pt.csm.application.SmartSiteMockKTest
import com.bosch.pt.csm.common.i18n.Key.USER_DELETED
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.context.MessageSource

@SmartSiteMockKTest
class AbstractResourceFactoryHelperTest {

  @MockK(relaxed = true) private lateinit var messageSource: MessageSource

  @InjectMockKs private lateinit var cut: TestResourceFactoryHelper

  @BeforeEach
  fun setup() {
    every { messageSource.getMessage(USER_DELETED, arrayOf(), any()) } returns DELETED_USER_NAME
  }

  @Test
  fun verifyDeletedUserIdentifierIsRandom() {
    val deletedUserReference = cut.getDeletedUserReference()
    val userIdentifierFirstCall = deletedUserReference.get().identifier
    val userIdentifierSecondCall = deletedUserReference.get().identifier
    assertThat(userIdentifierFirstCall).isNotEqualTo(userIdentifierSecondCall)
  }

  @Test
  fun verifyDeletedUserName() {
    val deletedUserReference = cut.getDeletedUserReference()
    val deletedUserDisplayName = deletedUserReference.get().displayName
    assertThat(deletedUserDisplayName).isEqualTo(DELETED_USER_NAME)
  }

  class TestResourceFactoryHelper(messageSource: MessageSource) :
      AbstractResourceFactoryHelper(messageSource)

  companion object {
    private const val DELETED_USER_NAME = "Deleted User"
  }
}
