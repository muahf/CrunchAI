package com.crunchai.presentation.common

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** Время вида «14:05» в часовом поясе телефона. */
fun formatTime(
    instant: Instant,
    zone: ZoneId,
): String = timeFormatter.format(instant.atZone(zone))