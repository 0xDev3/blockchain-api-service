package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.generated.jooq.tables.ClientInfoTable
import com.ampnet.blockchainapiservice.model.result.ClientInfo
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress
import mu.KLogging
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class JooqClientInfoRepository(private val dslContext: DSLContext) : ClientInfoRepository {

    companion object : KLogging()

    override fun getById(clientId: String): ClientInfo? {
        logger.debug { "Get client info by clientId: $clientId" }
        return dslContext.selectFrom(ClientInfoTable.CLIENT_INFO)
            .where(ClientInfoTable.CLIENT_INFO.CLIENT_ID.eq(clientId))
            .fetchOne {
                ClientInfo(
                    clientId = it.clientId!!,
                    chainId = it.chainId?.let(::ChainId),
                    sendRedirectUrl = it.sendRedirectUrl,
                    balanceRedirectUrl = it.balanceRedirectUrl,
                    tokenAddress = it.tokenAddress?.let(::ContractAddress)
                )
            }
    }
}
