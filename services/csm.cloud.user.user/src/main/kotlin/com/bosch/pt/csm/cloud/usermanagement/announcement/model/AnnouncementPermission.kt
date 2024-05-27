/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.announcement.model

import com.bosch.pt.csm.cloud.common.api.LocalEntity
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model.User
import jakarta.persistence.Entity
import jakarta.persistence.FetchType.EAGER
import jakarta.persistence.ForeignKey
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull

@Entity
@Table(
    name = "announcement_permission",
    indexes =
        [Index(name = "UK_Announcement_Permission_User", columnList = "user_id", unique = true)])
class AnnouncementPermission(

    // user
    @OneToOne(fetch = EAGER)
    @JoinColumn(foreignKey = ForeignKey(name = "FK_AnnouncementPermission_User"), nullable = false)
    @field:NotNull
    val user: User
) : LocalEntity<Long>()
