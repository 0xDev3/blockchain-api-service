package com.ampnet.blockchainapiservice.blockchain;

import java.math.BigInteger;
import java.util.Arrays;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;

/**
 * <p>Auto generated code.
 * <p><strong>Do not modify!</strong>
 * <p>Please use the <a href="https://docs.web3j.io/command_line.html">web3j command line tools</a>,
 * or the org.web3j.codegen.SolidityFunctionWrapperGenerator in the 
 * <a href="https://github.com/web3j/web3j/tree/master/codegen">codegen module</a> to update.
 *
 * <p>Generated with web3j version 1.4.1.
 */
@SuppressWarnings("rawtypes")
public class ReadonlyFunctionCallsContract extends Contract {
    public static final String BINARY = "6080604052348015600f57600080fd5b50609a8061001e6000396000f3fe6080604052348015600f57600080fd5b506004361060285760003560e01c8063fe0af8ab14602d575b600080fd5b603b6038366004604d565b90565b60405190815260200160405180910390f35b600060208284031215605d578081fd5b503591905056fea2646970667358221220246b24fe3aae4b70e21988d614bd78452d62594fd8451286fb70d7ddbbb2f50464736f6c63430008040033";

    public static final String FUNC_RETURNINGUINT = "returningUint";

    @Deprecated
    protected ReadonlyFunctionCallsContract(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected ReadonlyFunctionCallsContract(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected ReadonlyFunctionCallsContract(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected ReadonlyFunctionCallsContract(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public RemoteFunctionCall<BigInteger> returningUint(BigInteger input) {
        final Function function = new Function(FUNC_RETURNINGUINT, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(input)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    @Deprecated
    public static ReadonlyFunctionCallsContract load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new ReadonlyFunctionCallsContract(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static ReadonlyFunctionCallsContract load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new ReadonlyFunctionCallsContract(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static ReadonlyFunctionCallsContract load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new ReadonlyFunctionCallsContract(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static ReadonlyFunctionCallsContract load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new ReadonlyFunctionCallsContract(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<ReadonlyFunctionCallsContract> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(ReadonlyFunctionCallsContract.class, web3j, credentials, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<ReadonlyFunctionCallsContract> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(ReadonlyFunctionCallsContract.class, web3j, credentials, gasPrice, gasLimit, BINARY, "");
    }

    public static RemoteCall<ReadonlyFunctionCallsContract> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(ReadonlyFunctionCallsContract.class, web3j, transactionManager, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<ReadonlyFunctionCallsContract> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(ReadonlyFunctionCallsContract.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, "");
    }
}
