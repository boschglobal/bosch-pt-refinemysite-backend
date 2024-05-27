/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.common.oauth.utilities.provider

import com.nimbusds.jose.JOSEException
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.PrivateKey
import java.time.Duration
import java.time.Instant
import java.util.Date

@Suppress("unused")
object JwtCreator {

  private val KEYSTORE_PASSWORD = "sCLR6NctziiIHO04w".toCharArray()
  private var SIGNER: RSASSASigner

  init {
    try {
      val keystore = KeyStore.getInstance(KeyStore.getDefaultType())
      keystore.load(ClassLoader.getSystemResourceAsStream("keystore.jks"), KEYSTORE_PASSWORD)
      SIGNER = RSASSASigner(keystore.getKey("smartsite", KEYSTORE_PASSWORD) as PrivateKey)
    } catch (e: IOException) {
      throw IllegalStateException("Could not initialize SIGNER for token creation.", e)
    } catch (e: GeneralSecurityException) {
      throw IllegalStateException("Could not initialize SIGNER for token creation.", e)
    }
  }

  @Suppress("unused")
  fun generateAuthHeader(subject: String): String = "Bearer " + generateToken(subject).serialize()

  private fun generateToken(subject: String): SignedJWT {
    val now = Instant.now()
    val expiration = now.plus(Duration.ofDays(1))
    val claims =
        JWTClaimsSet.Builder()
            .audience("oauth2-resource")
            .expirationTime(Date.from(expiration))
            .issueTime(Date.from(now))
            .issuer("https://jwtcreator.example.com")
            .subject(subject)
            .build()

    val signedJwt = SignedJWT(JWSHeader(JWSAlgorithm.RS256), claims)
    try {
      signedJwt.sign(SIGNER)
    } catch (e: JOSEException) {
      throw IllegalStateException("Failed to sign token.", e)
    }

    return signedJwt
  }
}
