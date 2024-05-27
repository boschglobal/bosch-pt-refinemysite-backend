/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

@file:JvmName("RandomUtils")

package com.bosch.pt.csm.cloud.common.test

import java.util.UUID
import java.util.concurrent.ThreadLocalRandom

fun randomLong(): Long = ThreadLocalRandom.current().nextLong()

fun randomLong(minInclusive: Long, maxExclusive: Long) =
    ThreadLocalRandom.current().nextLong(minInclusive, maxExclusive)

fun randomString() = UUID.randomUUID().toString()
