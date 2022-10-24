package com.ampnet.blockchainapiservice.blockchain.properties

import com.ampnet.blockchainapiservice.config.ApplicationProperties
import com.ampnet.blockchainapiservice.config.ChainProperties
import com.ampnet.blockchainapiservice.util.ChainId

@Suppress("MagicNumber")
enum class Chain(
    val id: ChainId,
    val rpcUrl: String,
    val infura: String?,
    val propertiesProvider: (ApplicationProperties) -> ChainProperties
) {
    MATIC_MAIN(
        ChainId(137),
        "https://rpc-mainnet.matic.network/",
        "https://polygon-mainnet.infura.io/v3/",
        { it.chainMatic }
    ),
    MATIC_TESTNET_MUMBAI(
        ChainId(80001),
        "https://rpc-mumbai.matic.today/",
        "https://polygon-mumbai.infura.io/v3/",
        { it.chainMumbai }
    ),
    ETHEREUM_MAIN(
        ChainId(1),
        "https://cloudflare-eth.com/",
        "https://mainnet.infura.io/v3/",
        { it.chainEthereum }
    ),
    GOERLI_TESTNET(
        ChainId(5),
        "https://goerli.prylabs.net/",
        "https://goerli.infura.io/v3/",
        { it.chainGoerli }
    ),
    HARDHAT_TESTNET(
        ChainId(31337),
        "http://hardhat:8545",
        "http://localhost:", // used in tests to inject HARDHAT_PORT via infuraId
        { it.chainHardhatTestnet }
    ),
    BSC(
        ChainId(56),
        "https://bsc-dataseed.binance.org/",
        null,
        { it.chainBsc }
    ),
    XDAI(
        ChainId(100),
        "https://rpc.xdaichain.com/ ",
        null,
        { it.chainXdai }
    ),
    FANTOM(
        ChainId(250),
        "https://rpc.ftm.tools/",
        null,
        { it.chainFantom }
    ),
    MOONRIVER(
        ChainId(1285),
        "https://rpc.moonriver.moonbeam.network/",
        null,
        { it.chainMoonriver }
    ),
    AVAX(
        ChainId(43114),
        "https://api.avax.network/ext/bc/C/rpc",
        null,
        { it.chainAvalanche }
    ),
    AURORA(
        ChainId(1313161554),
        "https://mainnet.aurora.dev/",
        null,
        { it.chainAurora }
    ),
    ARBITRUM(
        ChainId(42161),
        "https://arb1.arbitrum.io/rpc",
        null,
        { it.chainArbitrum }
    ),
    OPTIMISM(
        ChainId(10),
        "https://mainnet.optimism.io",
        null,
        { it.chainOptimism }
    ),
    CELO(
        ChainId(42220),
        "https://forno.celo.org",
        null,
        { it.chainCelo }
    ),
    PARA_TIME(
        ChainId(42262),
        "https://emerald.oasis.dev",
        null,
        { it.chainParaTime }
    ),
    MOONBEAM(
        ChainId(1284),
        "https://moonbeam.public.blastapi.io",
        null,
        { it.chainMoonbeam }
    ),
    POLYGON_ZK_EVM_TESTNET(
        ChainId(1402),
        "https://public.zkevm-test.net:2083",
        null,
        { it.chainPolygonZkEvmTestnet }
    ),
    CELO_ALFAJORES_TESTNET(
        ChainId(44787),
        "https://alfajores-forno.celo-testnet.org",
        null,
        { it.chainCeloAlfajoresTestnet }
    );

    companion object {
        private val map = values().associateBy(Chain::id)
        fun fromId(id: ChainId): Chain? = map[id]
    }
}
