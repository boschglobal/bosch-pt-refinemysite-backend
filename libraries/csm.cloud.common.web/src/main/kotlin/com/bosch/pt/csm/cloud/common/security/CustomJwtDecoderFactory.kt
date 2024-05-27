/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.security

import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtDecoderFactory
import org.springframework.security.oauth2.jwt.JwtDecoders

/** Resolves the [JwtDecoder] from the actual issuer location via HTTP request. */
class CustomJwtDecoderFactory : JwtDecoderFactory<String> {
  override fun createDecoder(issuer: String): JwtDecoder = JwtDecoders.fromIssuerLocation(issuer)
}
