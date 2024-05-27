/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.announcement.authorization

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.usermanagement.announcement.model.AnnouncementPermissionBuilder
import com.bosch.pt.csm.cloud.usermanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.usermanagement.common.AbstractApiIntegrationTest
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@SmartSiteSpringBootTest
internal class AnnouncementAuthorizationComponentTest : AbstractApiIntegrationTest() {

  @Autowired private lateinit var cut: AnnouncementAuthorizationComponent

  @BeforeEach
  fun setup() {
    eventStreamGenerator.submitUser("user")
  }

  @Test
  fun verifyMaintainPermissionGrantedForAdminUser() {
    setAuthentication("admin")
    assertThat(cut.hasMaintainAnnouncementsPermission()).isTrue
  }

  @Test
  fun verifyMaintainPermissionGrantedForUserWithAnnouncementPermission() {
    eventStreamGenerator
        .getIdentifier("user")
        .let { repositories.userRepository.findOneByIdentifier(UserId(it))!! }
        .let { AnnouncementPermissionBuilder.announcementPermission().withUser(it).build() }
        .let { repositories.announcementPermissionRepository.save(it) }

    setAuthentication("user")
    assertThat(cut.hasMaintainAnnouncementsPermission()).isTrue
  }

  @Test
  fun verifyMaintainPermissionDeniedForUserWithoutAnnouncementPermission() {
    setAuthentication("user")
    assertThat(cut.hasMaintainAnnouncementsPermission()).isFalse
  }
}
