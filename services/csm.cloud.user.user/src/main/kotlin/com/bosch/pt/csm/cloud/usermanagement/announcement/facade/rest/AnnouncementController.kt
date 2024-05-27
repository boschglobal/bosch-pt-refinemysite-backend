/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.announcement.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersion
import com.bosch.pt.csm.cloud.usermanagement.announcement.boundary.AnnouncementService
import com.bosch.pt.csm.cloud.usermanagement.announcement.facade.rest.resources.AnnouncementListResource
import com.bosch.pt.csm.cloud.usermanagement.announcement.facade.rest.resources.AnnouncementResource
import com.bosch.pt.csm.cloud.usermanagement.announcement.facade.rest.resources.CreateAnnouncementResource
import com.bosch.pt.csm.cloud.usermanagement.announcement.facade.rest.resources.factory.AnnouncementListResourceFactory
import com.bosch.pt.csm.cloud.usermanagement.announcement.model.Announcement
import java.util.Locale
import java.util.UUID
import java.util.UUID.randomUUID
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@ApiVersion(to = 1)
@RestController
class AnnouncementController(
    private val announcementListResourceFactory: AnnouncementListResourceFactory,
    private val announcementService: AnnouncementService,
    @Value("\${locale.default}") private val defaultLocale: Locale
) {

  @GetMapping(ANNOUNCEMENT_RESOURCE_PATH)
  fun findAllAnnouncements(): ResponseEntity<AnnouncementListResource> =
      ResponseEntity.ok(
          announcementListResourceFactory.build(
              announcementService.findAllAnnouncements().map {
                it.getTranslationByLocale(LocaleContextHolder.getLocale())
                    ?: it.getTranslationByLocale(defaultLocale)
                        ?: throw IllegalArgumentException(
                        "No translation found for announcement with identifier: ${it.identifier}")
              }))

  @PostMapping(ANNOUNCEMENT_RESOURCE_PATH)
  fun addAnnouncement(
      @RequestBody @Valid announcementResource: CreateAnnouncementResource
  ): ResponseEntity<AnnouncementResource> {
    val announcement =
        Announcement(
            randomUUID(),
            announcementResource.type,
            announcementResource.translations.map { it.createEntity() })
    announcementService.createAnnouncement(announcement).let {
      return ResponseEntity.status(CREATED)
          .body(
              AnnouncementResource(
                  announcement.identifier,
                  announcement.type,
                  requireNotNull(announcement.getTranslationByLocale(defaultLocale)).value))
    }
  }

  @DeleteMapping("$ANNOUNCEMENT_RESOURCE_PATH/{identifier}")
  fun deleteAnnouncement(@PathVariable identifier: UUID): ResponseEntity<Void> =
      announcementService.deleteAnnouncement(identifier).let { ResponseEntity.noContent().build() }

  companion object {
    const val ANNOUNCEMENT_RESOURCE_PATH = "/announcements"
  }
}
