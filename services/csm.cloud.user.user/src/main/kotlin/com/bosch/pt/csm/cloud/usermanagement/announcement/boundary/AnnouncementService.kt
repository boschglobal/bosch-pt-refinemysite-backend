/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.announcement.boundary

import com.bosch.pt.csm.cloud.usermanagement.announcement.model.Announcement
import com.bosch.pt.csm.cloud.usermanagement.announcement.repository.AnnouncementRepository
import com.bosch.pt.csm.cloud.usermanagement.application.security.NoPreAuthorize
import java.util.Locale
import java.util.UUID
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AnnouncementService(
    private val announcementRepository: AnnouncementRepository,
    @Value("\${locale.default}") private val defaultLocale: Locale
) {

  @NoPreAuthorize(true)
  @Cacheable(value = ["announcements"], key = "#root.methodName")
  @Transactional(readOnly = true)
  fun findAllAnnouncements(): List<Announcement> = announcementRepository.findAll()

  @PreAuthorize("@announcementAuthorizationComponent.hasMaintainAnnouncementsPermission()")
  @Transactional
  fun createAnnouncement(announcement: Announcement) =
      assertTranslationForDefaultLocaleIsPresent(announcement).also {
        announcementRepository.save(announcement)
      }

  @PreAuthorize("@announcementAuthorizationComponent.hasMaintainAnnouncementsPermission()")
  @Transactional
  fun deleteAnnouncement(identifier: UUID) = announcementRepository.deleteByIdentifier(identifier)

  private fun assertTranslationForDefaultLocaleIsPresent(announcement: Announcement) {
    require(!announcement.translations.none { it.locale == defaultLocale.language }) {
      "Translation for language ${LocaleContextHolder.getLocale().toLanguageTag()} is missing"
    }
  }
}
