/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.user.authorization.model

import com.bosch.pt.csm.cloud.common.api.LocalEntity
import com.bosch.pt.csm.cloud.common.model.IsoCountryCodeEnum
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType.STRING
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.util.UUID

/**
 * This class was introduced to restrict access for admin users to certain countries. When at least
 * one entry is defined here, the restrictions are applied.
 */
@Entity
@Table(
    name = "user_country_restriction",
    indexes =
        [
            Index(
                name = "UK_UserCountryRestriction_User_Country",
                columnList = "userId, countryCode",
                unique = true),
            Index(name = "IX_UserCountryRestriction_User", columnList = "userId", unique = false)])
class UserCountryRestriction(
    // id of the user for which this whitelist is
    @Column(nullable = false) val userId: UUID,

    // code of the country that is whitelisted
    @Column(nullable = false, columnDefinition = "varchar(255)")
    @Enumerated(STRING)
    val countryCode: IsoCountryCodeEnum
) : LocalEntity<Long>()
