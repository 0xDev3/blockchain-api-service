package dev3.blockchainapiservice.features.api.promocodes.controller

import dev3.blockchainapiservice.config.binding.annotation.UserIdentifierBinding
import dev3.blockchainapiservice.features.api.access.model.result.UserIdentifier
import dev3.blockchainapiservice.features.api.promocodes.model.request.GeneratePromoCodeRequest
import dev3.blockchainapiservice.features.api.promocodes.model.response.PromoCodeResponse
import dev3.blockchainapiservice.features.api.promocodes.model.response.PromoCodesResponse
import dev3.blockchainapiservice.features.api.promocodes.service.PromoCodeService
import dev3.blockchainapiservice.service.UtcDateTimeProvider
import dev3.blockchainapiservice.util.UtcDateTime
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.OffsetDateTime
import javax.validation.Valid
import kotlin.time.Duration.Companion.days

@Validated
@RestController
class PromoCodeController(
    private val promoCodeService: PromoCodeService,
    private val utcDateTimeProvider: UtcDateTimeProvider
) {
    companion object {
        private const val DEFAULT_PROMO_CODE_PREFIX = "DEV3-"
        private const val DEFAULT_WRITE_REQUESTS = 5_000L
        private const val DEFAULT_READ_REQUESTS = 1_000_000L
        private val DEFAULT_DURATION = 30.days
    }

    @GetMapping("/v1/promo-codes")
    fun getPromoCodes(
        @UserIdentifierBinding
        userIdentifier: UserIdentifier,

        @RequestParam("from", required = false, defaultValue = "1970-01-01T00:00:00Z")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        from: OffsetDateTime,

        @RequestParam("to", required = false, defaultValue = "9999-12-31T23:59:59Z")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        to: OffsetDateTime
    ): ResponseEntity<PromoCodesResponse> =
        ResponseEntity.ok(
            PromoCodesResponse(
                promoCodeService.getPromoCodes(
                    userIdentifier = userIdentifier,
                    validFrom = UtcDateTime(from),
                    validUntil = UtcDateTime(to)
                )
                    .map(::PromoCodeResponse)
            )
        )

    @PostMapping("/v1/promo-codes")
    fun generatePromoCode(
        @UserIdentifierBinding userIdentifier: UserIdentifier,
        @Valid @RequestBody requestBody: GeneratePromoCodeRequest
    ): ResponseEntity<PromoCodeResponse> =
        ResponseEntity.ok(
            PromoCodeResponse(
                promoCodeService.generatePromoCode(
                    userIdentifier = userIdentifier,
                    prefix = requestBody.prefix ?: DEFAULT_PROMO_CODE_PREFIX,
                    writeRequests = requestBody.writeRequests ?: DEFAULT_WRITE_REQUESTS,
                    readRequests = requestBody.readRequests ?: DEFAULT_READ_REQUESTS,
                    validUntil = utcDateTimeProvider.getUtcDateTime() +
                        (requestBody.validityInDays?.days ?: DEFAULT_DURATION)
                )
            )
        )

    @PostMapping("/v1/use-promo-code/{code}")
    fun usePromoCode(
        @UserIdentifierBinding userIdentifier: UserIdentifier,
        @PathVariable code: String
    ) {
        promoCodeService.usePromoCode(userIdentifier, code)
    }
}
