/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.security

import com.bosch.pt.csm.cloud.projectmanagement.user.model.GenderEnum
import com.bosch.pt.csm.cloud.projectmanagement.user.model.User
import org.apache.commons.lang3.StringUtils
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.test.context.support.WithSecurityContextFactory
import java.util.UUID

internal class WithMockSmartSiteUserSecurityContextFactory : WithSecurityContextFactory<WithMockSmartSiteUser> {

    override fun createSecurityContext(withUser: WithMockSmartSiteUser): SecurityContext {
        val username = getStringOrDefault(withUser.externalIdentifier, withUser.value)

        val principal = User(
            getIdOrDefault(withUser.identifier),
            username,
            username,
            null,
            GenderEnum.FEMALE,
            null,
            admin = withUser.admin
        )

        return SecurityContextHolder.createEmptyContext().apply {
            this.authentication = UsernamePasswordAuthenticationToken(
                principal,
                principal.password,
                principal.authorities
            )
        }
    }

    private fun getStringOrDefault(value: String, default: String): String = if (StringUtils.isBlank(
            value
        )
    ) default else value

    private fun getIdOrDefault(value: String): UUID = if (StringUtils.isBlank(
            value
        )
    ) UUID.randomUUID() else UUID.fromString(value)
}
