/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.announcement.model

import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model.User

class AnnouncementPermissionBuilder private constructor() {

  private var user: User? = null

  fun withUser(user: User): AnnouncementPermissionBuilder {
    this.user = user
    return this
  }

  fun build() = AnnouncementPermission(user!!)

  companion object {
    fun announcementPermission() = AnnouncementPermissionBuilder()
  }
}
