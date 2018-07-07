package org.brewchain.manage.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.math.BigInteger;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.brewchain.account.core.AccountHelper;
import org.brewchain.account.core.BlockChainConfig;
import org.brewchain.account.core.TransactionHelper;
import org.brewchain.account.enums.TransTypeEnum;
import org.brewchain.account.util.OEntityBuilder;
import org.brewchain.bcapi.gens.Oentity.KeyStoreValue;
import org.brewchain.bcvm.CodeBuild;
import org.brewchain.core.util.ByteUtil;
import org.brewchain.evmapi.gens.Tx.MultiTransaction;
import org.brewchain.evmapi.gens.Tx.MultiTransactionBody;
import org.brewchain.evmapi.gens.Tx.MultiTransactionInput;
import org.brewchain.evmapi.gens.Tx.MultiTransactionSignature;
import org.brewchain.manage.dao.ManageDaos;
import org.brewchain.manage.gens.Manageimpl.PMANCommand;
import org.brewchain.manage.gens.Manageimpl.PMANModule;
import org.brewchain.manage.gens.Manageimpl.ReqCreateContract;
import org.brewchain.manage.gens.Manageimpl.ReqCreateNewAccount;
import org.brewchain.manage.gens.Manageimpl.RespCreateContract;
import org.brewchain.manage.gens.Manageimpl.RespCreateNewAccount;
import org.fc.brewchain.bcapi.EncAPI;
import org.fc.brewchain.bcapi.KeyStoreHelper;

import com.google.protobuf.ByteString;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.oapi.scala.commons.SessionModules;
import onight.osgi.annotation.NActorProvider;
import onight.tfw.async.CompleteHandler;
import onight.tfw.ntrans.api.annotation.ActorRequire;
import onight.tfw.otransio.api.PacketHelper;
import onight.tfw.otransio.api.beans.FramePacket;

@NActorProvider
@Slf4j
@Data
public class CreateContractImpl extends SessionModules<ReqCreateContract> {
	@ActorRequire(name = "man_Daos", scope = "global")
	ManageDaos dao;
	@ActorRequire(name = "bc_encoder", scope = "global")
	EncAPI encApi;
	@ActorRequire(name = "KeyStore_Helper", scope = "global")
	KeyStoreHelper keyStoreHelper;
	@ActorRequire(name = "Account_Helper", scope = "global")
	AccountHelper oAccountHelper;
	@ActorRequire(name = "BlockChain_Config", scope = "global")
	BlockChainConfig blockChainConfig;
	@ActorRequire(name = "Transaction_Helper", scope = "global")
	TransactionHelper transactionHelper;

	@Override
	public String[] getCmds() {
		return new String[] { PMANCommand.CCT.name() };
	}

	@Override
	public String getModule() {
		return PMANModule.MAN.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqCreateContract pb, final CompleteHandler handler) {
		RespCreateContract.Builder oRespCreateContract = RespCreateContract.newBuilder();

		try {
			if (StringUtils.isBlank(pb.getPwd())) {
				oRespCreateContract.setRetCode("-1");
				oRespCreateContract.setRetMsg("password cannot be empty");
				handler.onFinished(PacketHelper.toPBReturn(pack, oRespCreateContract.build()));
				return;
			}

			FileReader fr = null;
			BufferedReader br = null;
			try {
				fr = new FileReader("keystore" + File.separator + blockChainConfig.getNet() + File.separator
						+ "keystore" + blockChainConfig.getKeystoreNumber() + ".json");
				br = new BufferedReader(fr);
				String keyStoreJsonStr = "";

				String line = br.readLine();
				while (line != null) {
					keyStoreJsonStr += line.trim().replace("\r", "").replace("\t", "");
					line = br.readLine();
				}
				br.close();
				fr.close();

				KeyStoreValue oKeyStoreValue = keyStoreHelper.getKeyStore(keyStoreJsonStr, pb.getPwd());
				if (oKeyStoreValue == null) {
					oRespCreateContract.setRetCode("-1");
					oRespCreateContract.setRetMsg("秘钥文件或者密码错误");
				} else {
					CodeBuild.Build cvm = CodeBuild.newBuild(CodeBuild.Type.SOLIDITY);
					CodeBuild.Result ret = cvm.build(pb.getCode());

					MultiTransaction.Builder oMultiTransaction = MultiTransaction.newBuilder();
					MultiTransactionBody.Builder oMultiTransactionBody = MultiTransactionBody.newBuilder();

					MultiTransactionInput.Builder oMultiTransactionInput4 = MultiTransactionInput.newBuilder();
					oMultiTransactionInput4.setAddress(ByteString.copyFrom(encApi.hexDec(oKeyStoreValue.getAddress())));
					oMultiTransactionInput4.setAmount(ByteString.copyFrom(ByteUtil.bigIntegerToBytes(BigInteger.ZERO)));
					int nonce = oAccountHelper
							.getNonce(ByteString.copyFrom(encApi.hexDec(oKeyStoreValue.getAddress())));
					oMultiTransactionInput4.setNonce(nonce);
					oMultiTransactionBody.addInputs(oMultiTransactionInput4);
					oMultiTransactionBody.setType(TransTypeEnum.TYPE_CreateContract.value());
					oMultiTransactionBody.setData(ByteString.copyFrom(encApi.hexDec(ret.data)));
					oMultiTransactionBody.setExdata(ByteString.copyFromUtf8(ret.exdata));
					oMultiTransaction.clearTxHash();
					oMultiTransactionBody.clearSignatures();
					oMultiTransactionBody.setTimestamp(System.currentTimeMillis());
					// 签名
					MultiTransactionSignature.Builder oMultiTransactionSignature21 = MultiTransactionSignature
							.newBuilder();
					oMultiTransactionSignature21
							.setPubKey(ByteString.copyFrom(encApi.hexDec(oKeyStoreValue.getPubkey())));
					oMultiTransactionSignature21.setSignature(ByteString.copyFrom(
							encApi.ecSign(oKeyStoreValue.getPrikey(), oMultiTransactionBody.build().toByteArray())));
					oMultiTransactionBody.addSignatures(oMultiTransactionSignature21);
					oMultiTransaction.setTxBody(oMultiTransactionBody);

					oRespCreateContract.setContractHash(encApi.hexEnc(transactionHelper
							.getContractAddressByTransaction(oMultiTransaction.build()).toByteArray()));

					String txHash = transactionHelper.CreateMultiTransaction(oMultiTransaction);
					oRespCreateContract.setRetCode("1");
				}
			} catch (Throwable e) {
				if (br != null) {
					br.close();
				}
				if (fr != null) {
					fr.close();
				}
				throw e;
			}

		} catch (Throwable e) {
			oRespCreateContract.setRetCode("-1");
			oRespCreateContract.setRetMsg("未知异常:" + e.getMessage());
		}
		handler.onFinished(PacketHelper.toPBReturn(pack, oRespCreateContract.build()));
		return;
	}
}
