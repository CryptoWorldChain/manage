package org.brewchain.manage.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.math.BigInteger;
import java.util.Date;

import org.brewchain.account.core.AccountHelper;
import org.brewchain.account.core.BlockChainConfig;
import org.brewchain.account.core.TransactionHelper;
import org.brewchain.evmapi.gens.Tx.MultiTransaction;
import org.brewchain.evmapi.gens.Tx.MultiTransactionBody;
import org.brewchain.evmapi.gens.Tx.MultiTransactionInput;
import org.brewchain.evmapi.gens.Tx.MultiTransactionOutput;
import org.brewchain.evmapi.gens.Tx.MultiTransactionSignature;
import org.brewchain.account.enums.TransTypeEnum;
import org.brewchain.account.util.ByteUtil;
import org.brewchain.bcapi.gens.Oentity.KeyStoreValue;
import org.brewchain.manage.dao.ManageDaos;
import org.brewchain.manage.gens.Manageimpl.PMANCommand;
import org.brewchain.manage.gens.Manageimpl.PMANModule;
import org.brewchain.manage.gens.Manageimpl.ReqSendLockCWS;
import org.brewchain.manage.gens.Manageimpl.RespDoTxResult;
import org.fc.brewchain.bcapi.EncAPI;
import org.fc.brewchain.bcapi.KeyStoreHelper;
import org.fc.brewchain.bcapi.UnitUtil;

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
public class SendLockCWSImpl extends SessionModules<ReqSendLockCWS> {
	@ActorRequire(name = "man_Daos", scope = "global")
	ManageDaos dao;
	@ActorRequire(name = "bc_encoder", scope = "global")
	EncAPI encApi;
	@ActorRequire(name = "KeyStore_Helper", scope = "global")
	KeyStoreHelper keyStoreHelper;
	@ActorRequire(name = "Account_Helper", scope = "global")
	AccountHelper accountHelper;
	@ActorRequire(name = "Transaction_Helper", scope = "global")
	TransactionHelper transactionHelper;

	@ActorRequire(name = "BlockChain_Config", scope = "global")
	BlockChainConfig blockChainConfig;

	@Override
	public String[] getCmds() {
		return new String[] { PMANCommand.SLC.name() };
	}

	@Override
	public String getModule() {
		return PMANModule.MAN.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqSendLockCWS pb, final CompleteHandler handler) {
		RespDoTxResult.Builder oRespDoTxResult = RespDoTxResult.newBuilder();
		try {
			// read file
			String net = blockChainConfig.getNet();
			FileReader fr = new FileReader("keystore" + File.separator + net + File.separator + "keystore"
					+ blockChainConfig.getKeystoreNumber() + ".json");
			BufferedReader br = new BufferedReader(fr);
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
				oRespDoTxResult.setRetCode("-1");
				oRespDoTxResult.setRetMsg("pwd or jsonstr error");
			} else {
				int nonce = accountHelper.getNonce(ByteString.copyFrom(encApi.hexDec(oKeyStoreValue.getAddress())));

				MultiTransaction.Builder oMultiTransaction = MultiTransaction.newBuilder();
				MultiTransactionBody.Builder oMultiTransactionBody = MultiTransactionBody.newBuilder();
				MultiTransactionInput.Builder oMultiTransactionInput = MultiTransactionInput.newBuilder();
				oMultiTransactionInput.setAddress(ByteString.copyFrom(encApi.hexDec(oKeyStoreValue.getAddress())));
				oMultiTransactionInput
						.setAmount(ByteString.copyFrom(ByteUtil.bigIntegerToBytes(UnitUtil.toWei(pb.getAmount()))));
				oMultiTransactionInput.setNonce(nonce);
				oMultiTransactionInput.setToken("CWS");
				oMultiTransactionBody.addInputs(oMultiTransactionInput);
//				MultiTransactionOutput.Builder oMultiTransactionOutput = MultiTransactionOutput.newBuilder();
//				oMultiTransactionOutput.setAddress(ByteString.copyFrom(encApi.hexDec(oKeyStoreValue.getAddress())));
//				oMultiTransactionOutput.setAmount(ByteString.copyFrom(ByteUtil.bigIntegerToBytes(new BigInteger(pb.getAmount()))));
//				oMultiTransactionBody.addOutputs(oMultiTransactionOutput);
				oMultiTransactionBody.setType(TransTypeEnum.TYPE_LockTokenTransaction.value());
				oMultiTransaction.clearTxHash();
				oMultiTransactionBody.clearSignatures();
				oMultiTransactionBody.setTimestamp(System.currentTimeMillis());
				// 签名
				MultiTransactionSignature.Builder oMultiTransactionSignature = MultiTransactionSignature.newBuilder();
				oMultiTransactionSignature.setSignature(ByteString.copyFrom(
						encApi.ecSign(oKeyStoreValue.getPrikey(), oMultiTransactionBody.build().toByteArray())));
				oMultiTransactionBody.addSignatures(oMultiTransactionSignature);
				oMultiTransaction.setTxBody(oMultiTransactionBody);

				String txHash = transactionHelper.CreateMultiTransaction(oMultiTransaction);
				oRespDoTxResult.setRetCode("1");
				oRespDoTxResult.setTxHash(txHash);
			}
		} catch (Exception e) {
			if (e.getMessage() != null) {
				oRespDoTxResult.setRetMsg(e.getMessage());
			}
			oRespDoTxResult.setRetCode("-1");
		}
		handler.onFinished(PacketHelper.toPBReturn(pack, oRespDoTxResult.build()));
	}
}
