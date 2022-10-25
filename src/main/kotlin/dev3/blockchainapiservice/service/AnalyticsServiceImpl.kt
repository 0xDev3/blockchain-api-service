package dev3.blockchainapiservice.service

import com.facebook.ads.sdk.APIContext
import com.facebook.ads.sdk.APIException
import com.facebook.ads.sdk.serverside.CustomData
import com.facebook.ads.sdk.serverside.Event
import com.facebook.ads.sdk.serverside.EventRequest
import com.facebook.ads.sdk.serverside.UserData
import dev3.blockchainapiservice.config.ApplicationProperties
import dev3.blockchainapiservice.model.result.UserIdentifier
import mu.KLogging
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class AnalyticsServiceImpl(
    private val applicationProperties: ApplicationProperties
) : AnalyticsService {

    companion object : KLogging() {
        private const val SECONDS_IN_MILLISECOND = 1_000L
    }

    private val context: APIContext? = createApiContext(applicationProperties)

    override fun postApiKeyCreatedEvent(
        userIdentifier: UserIdentifier,
        projectId: UUID,
        origin: String?,
        userAgent: String?,
        remoteAddr: String?
    ) {
        if (context === null) {
            logger.warn { "Failed to post 'API Key Created' event to Meta Pixel. API not initialized properly!" }
            return
        }

        val pixelId = applicationProperties.metaPixelProperties.pixelId

        if (pixelId === null) {
            logger.warn { "Failed to post 'API Key Created' event to Meta Pixel. Missing pixelId configuration!" }
            return
        }

        logger.info {
            "Posting 'API Key Created' event to Meta Pixel for userIdentifier: $userIdentifier, " +
                "projectId: $projectId, origin: $origin, userAgent: $userAgent, remoteAddr: $remoteAddr"
        }

        val event = Event()
            .eventName("Login")
            .eventTime(System.currentTimeMillis() / SECONDS_IN_MILLISECOND)
            .userData(
                UserData()
                    .externalId(userIdentifier.id.toString())
                    .clientUserAgent(userAgent)
                    .clientIpAddress(remoteAddr)
            )
            .eventSourceUrl(origin)
            .customData(
                CustomData().customProperties(
                    hashMapOf(
                        "wallet" to userIdentifier.userIdentifier,
                        "projectId" to projectId.toString()
                    )
                )
            )
        try {
            val eventRequest = EventRequest(pixelId, context).addDataItem(event)
            val response = eventRequest.execute()
            logger.debug { "'API Key Created' event posted successfully to Meta Pixel. Response: $response" }
        } catch (e: APIException) {
            logger.warn { "Failed to post 'API Key Created' event to Meta Pixel. Exception: $e" }
        }
    }

    private fun createApiContext(applicationProperties: ApplicationProperties): APIContext? {
        return applicationProperties.metaPixelProperties.accessToken?.let { APIContext(it) }
    }
}
