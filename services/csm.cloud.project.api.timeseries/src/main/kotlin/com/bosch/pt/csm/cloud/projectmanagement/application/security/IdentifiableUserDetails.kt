/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.security

import java.util.UUID
import org.springframework.security.core.userdetails.UserDetails

interface IdentifiableUserDetails : UserDetails {

  fun userIdentifier(): UUID
}
