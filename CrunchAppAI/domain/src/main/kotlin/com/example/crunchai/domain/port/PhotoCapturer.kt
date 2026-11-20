package com.example.crunchai.domain.port

import com.example.crunchai.domain.common.DomainResult

/** Камера. Реализация на CameraX лежит в :data. Фото возвращается как JPEG-байты. */
interface PhotoCapturer {
    suspend fun capture(): DomainResult<ByteArray>
}