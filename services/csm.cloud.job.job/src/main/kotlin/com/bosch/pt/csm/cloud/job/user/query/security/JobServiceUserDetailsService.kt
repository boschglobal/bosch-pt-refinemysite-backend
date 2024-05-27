/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.job.user.query.security

import com.bosch.pt.csm.cloud.job.user.query.ExternalUserIdentifier
import com.bosch.pt.csm.cloud.job.user.query.UserProjectionRepository
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class JobServiceUserDetailsService(private val userProjectionRepository: UserProjectionRepository) :
    UserDetailsService {

  override fun loadUserByUsername(externalUserIdentifier: String): UserDetails {
    return userProjectionRepository.findByExternalUserIdentifier(
            ExternalUserIdentifier(externalUserIdentifier)
    )
        ?.let { JobServiceUserDetails(it.userIdentifier, it.locale) }
        ?: throw UsernameNotFoundException(
            "Unknown external user identifier: $externalUserIdentifier")
  }
}
