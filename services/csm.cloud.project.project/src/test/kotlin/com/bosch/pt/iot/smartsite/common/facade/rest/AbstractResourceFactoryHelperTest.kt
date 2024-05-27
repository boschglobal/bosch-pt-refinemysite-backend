/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.common.facade.rest

import com.bosch.pt.csm.cloud.common.SmartSiteMockKTest
import com.bosch.pt.csm.cloud.common.util.HttpTestUtils.setFakeUrlWithApiVersion
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.factory.AbstractResourceFactoryHelper
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.context.MessageSource

@SmartSiteMockKTest
class AbstractResourceFactoryHelperTest {

  @MockK private lateinit var messageSource: MessageSource

  @InjectMockKs private lateinit var cut: TestResourceFactoryHelper

  @BeforeEach
  fun setup() {
    setFakeUrlWithApiVersion()
    every { messageSource.getMessage(any(), any(), any()) } returns DELETED_USER_NAME
  }

  @Test
  fun verifyDeletedUserIdentifierIsRandom() {
    val deletedUserReference = cut.deletedUserReference

    val userIdentifierFirstCall = deletedUserReference.get().identifier
    val userIdentifierSecondCall = deletedUserReference.get().identifier

    assertThat(userIdentifierFirstCall).isNotEqualTo(userIdentifierSecondCall)
  }

  @Test
  fun verifyDeletedUserName() {
    val deletedUserReference = cut.deletedUserReference

    val deletedUserDisplayName = deletedUserReference.get().displayName

    assertThat(deletedUserDisplayName).isEqualTo(DELETED_USER_NAME)
  }

  class TestResourceFactoryHelper(messageSource: MessageSource) :
      AbstractResourceFactoryHelper(messageSource)

  companion object {
    private const val DELETED_USER_NAME = "Deleted User"
  }
}
