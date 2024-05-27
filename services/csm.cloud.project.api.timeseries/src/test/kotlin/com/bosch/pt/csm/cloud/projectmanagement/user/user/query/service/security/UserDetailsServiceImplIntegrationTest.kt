/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.user.user.query.service.security

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.messages.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.get
import com.bosch.pt.csm.cloud.projectmanagement.application.RmsSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.user.user.domain.asUserId
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUserTombstones
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserAggregateAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.getIdentifier
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException

@RmsSpringBootTest
class UserDetailsServiceImplIntegrationTest : AbstractIntegrationTest() {

  @Autowired lateinit var cut: UserDetailsService

  @Test
  fun `throws UsernameNotFoundException for unknown User`() {
    assertThatExceptionOfType(UsernameNotFoundException::class.java).isThrownBy {
      cut.loadUserByUsername("ciamId of unknown User")
    }
  }

  @Test
  fun `provides UserDetails for known User`() {
    val aggregate = get<UserAggregateAvro>("csm-user")!!
    assertThat(cut.loadUserByUsername(aggregate.userId))
        .extracting("identifier")
        .isEqualTo(aggregate.aggregateIdentifier.identifier.toUUID().asUserId())
  }

  @Test
  fun `throws UsernameNotFoundException for deleted User`() {
    val aggregate = get<UserAggregateAvro>("csm-user")!!
    eventStreamGenerator.submitUserTombstones(
        "csm-user",
        AggregateEventMessageKey(
            aggregate.aggregateIdentifier.buildAggregateIdentifier(), aggregate.getIdentifier()))

    assertThatExceptionOfType(UsernameNotFoundException::class.java).isThrownBy {
      cut.loadUserByUsername(aggregate.userId)
    }
  }
}
