/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.test

import java.time.Instant
import java.util.UUID
import java.util.concurrent.ThreadLocalRandom

fun randomInstant(): Instant = Instant.now()

fun randomLong(): Long = ThreadLocalRandom.current().nextLong()

fun randomUUID(): UUID = UUID.randomUUID()

fun randomString() = UUID.randomUUID().toString()
