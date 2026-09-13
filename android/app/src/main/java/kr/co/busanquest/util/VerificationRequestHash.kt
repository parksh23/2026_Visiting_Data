package kr.co.busanquest.util

import java.security.MessageDigest
import kr.co.busanquest.data.remote.MissionVerifyRequestDto

object VerificationRequestHash {
    fun bytes(request: MissionVerifyRequestDto): ByteArray {
        val fields = listOf(request.protocolVersion.toString(), request.challengeId.orEmpty(),
            request.missionId.toString(), request.missionType, request.localPassed.toString(),
            request.imageUrl.orEmpty(), request.receiptImageUrl.orEmpty())
        require(fields.none { '\n' in it || '\r' in it })
        return MessageDigest.getInstance("SHA-256").digest(fields.joinToString("\n").toByteArray(Charsets.UTF_8))
    }
}
