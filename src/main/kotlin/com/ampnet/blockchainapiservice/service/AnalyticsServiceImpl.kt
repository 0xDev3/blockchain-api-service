package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.config.ApplicationProperties
import com.ampnet.blockchainapiservice.model.result.UserIdentifier
import com.facebook.ads.sdk.APIContext
import com.facebook.ads.sdk.APIException
import com.facebook.ads.sdk.serverside.CustomData
import com.facebook.ads.sdk.serverside.Event
import com.facebook.ads.sdk.serverside.EventRequest
import com.facebook.ads.sdk.serverside.UserData
import mu.KLogging
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class AnalyticsServiceImpl(
    private val applicationProperties: ApplicationProperties
) : AnalyticsService {

    val context: APIContext? = createApiContext(applicationProperties)

    companion object : KLogging()

    override fun postApiKeyCreatedEvent(
        userIdentifier: UserIdentifier,
        projectId: UUID,
        origin: String?,
        userAgent: String?,
        remoteAddr: String?
    ) {
        val event = Event()
        logger.info {
            "Posting 'API Key Created' event to Meta Pixel for userIdentifier: $userIdentifier, " +
                "projectId: $projectId, origin: $origin, userAgent: $userAgent, remoteAddr: $remoteAddr"
        }
        event.eventName("Login")
            .eventTime(System.currentTimeMillis() / 1000)
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

        if (context === null) {
            logger.warn { "Failed to post 'API Key Created' event to Meta Pixel. API not initialized properly!" }
            return
        }
        val pixelId = applicationProperties.metaPixelProperties.pixelId
        if (pixelId === null) {
            logger.warn { "Failed to post 'API Key Created' event to Meta Pixel. Missing pixelId configuration!" }
            return
        }

        try {
            val eventRequest = EventRequest(pixelId, context)
            eventRequest.addDataItem(event)
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
