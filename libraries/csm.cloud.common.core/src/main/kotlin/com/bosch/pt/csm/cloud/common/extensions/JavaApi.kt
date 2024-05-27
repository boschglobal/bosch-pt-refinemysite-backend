/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.extensions

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset.UTC
import java.util.Date
import java.util.UUID

fun <K, V> Map<K, V>.firstKey() = entries.first().toPair().first

fun <K, V> Map<K, V>.firstValue() = entries.first().toPair().second

fun String.toUUID(): UUID = UUID.fromString(this)

fun Long.toInstantByMillis(): Instant = Instant.ofEpochMilli(this)

fun Long.toLocalDateByMillis(): LocalDate = LocalDate.ofInstant(toInstantByMillis(), UTC)

fun Long.toLocalDateTimeByMillis(): LocalDateTime =
    LocalDateTime.ofInstant(toInstantByMillis(), UTC)

fun Date.toLocalDate(): LocalDate = toInstant().atZone(ZoneId.systemDefault()).toLocalDate()

fun LocalDate.toEpochMilli(): Long = atStartOfDay(UTC).toInstant().toEpochMilli()

fun LocalDateTime.toEpochMilli(): Long = toInstant(UTC).toEpochMilli()

fun LocalDateTime.toDate(): Date = Date.from(this.toInstant(UTC))

fun Instant.toDate(): Date = Date.from(this)

fun <T> T.toList(): List<T> = listOf(this)
