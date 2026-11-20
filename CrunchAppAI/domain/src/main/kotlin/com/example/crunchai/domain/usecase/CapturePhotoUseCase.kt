package com.example.crunchai.domain.usecase

import com.example.crunchai.domain.common.DomainResult
import com.example.crunchai.domain.port.PhotoCapturer

class CapturePhotoUseCase(
    private val capturer: PhotoCapturer,
) {
    suspend operator fun invoke(): DomainResult<ByteArray> = capturer.capture()
}