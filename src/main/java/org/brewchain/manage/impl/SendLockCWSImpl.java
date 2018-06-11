package org.brewchain.manage.impl;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Date;

import org.brewchain.account.core.AccountHelper;
import org.brewchain.account.core.TransactionHelper;
import org.brewchain.evmapi.gens.Tx.MultiTransaction;
import org.brewchain.evmapi.gens.Tx.MultiTransactionBody;
import org.brewchain.evmapi.gens.Tx.MultiTransactionInput;
import org.brewchain.evmapi.gens.Tx.MultiTransactionOutput;
import org.brewchain.evmapi.gens.Tx.MultiTransactionSignature;
import org.brewchain.bcapi.gens.Oentity.KeyStoreValue;
import org.brewchain.manage.dao.ManageDaos;
import org.brewchain.manage.gens.Manageimpl.PMANCommand;
import org.brewchain.manage.gens.Manageimpl.PMANModule;
import org.brewchain.manage.gens.Manageimpl.ReqSendLockCWS;
import org.brewchain.manage.gens.Manageimpl.ReqSetNetwork;
import org.brewchain.manage.gens.Manageimpl.RespDoTxResult;
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
			FileReader fr = new FileReader(".keystore");
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
				int nonce = accountHelper.getNonce(encApi.hexDec(oKeyStoreValue.getAddress()));

				MultiTransaction.Builder oMultiTransaction = MultiTransaction.newBuilder();
				MultiTransactionBody.Builder oMultiTransactionBody = MultiTransactionBody.newBuilder();
				MultiTransactionInput.Builder oMultiTransactionInput = MultiTransactionInput.newBuilder();
				oMultiTransactionInput.setAddress(ByteString.copyFrom(encApi.hexDec(oKeyStoreValue.getAddress())));
				oMultiTransactionInput.setAmount(pb.getAmount());
				oMultiTransactionInput.setFee(0);
				oMultiTransactionInput.setFeeLimit(0);
				oMultiTransactionInput.setNonce(nonce);
				oMultiTransactionBody.addInputs(oMultiTransactionInput);
				MultiTransactionOutput.Builder oMultiTransactionOutput = MultiTransactionOutput.newBuilder();
				oMultiTransactionOutput.setAddress(ByteString.copyFrom(encApi.hexDec(oKeyStoreValue.getAddress())));
				oMultiTransactionOutput.setAmount(pb.getAmount());
				oMultiTransactionBody.addOutputs(oMultiTransactionOutput);
				oMultiTransactionBody.setData(ByteString.copyFromUtf8("06"));
				oMultiTransaction.setTxHash(ByteString.EMPTY);
				oMultiTransactionBody.clearSignatures();
				oMultiTransactionBody.setTimestamp((new Date()).getTime());
				// 签名
				MultiTransactionSignature.Builder oMultiTransactionSignature = MultiTransactionSignature.newBuilder();
				oMultiTransactionSignature.setPubKey(oKeyStoreValue.getPubKey());
				oMultiTransactionSignature.setSignature(encApi.hexEnc(
						encApi.ecSign(oKeyStoreValue.getPriKey(), oMultiTransactionBody.build().toByteArray())));
				oMultiTransactionBody.addSignatures(oMultiTransactionSignature);
				oMultiTransaction.setTxBody(oMultiTransactionBody);

				ByteString txHash = transactionHelper.CreateMultiTransaction(oMultiTransaction);
				oRespDoTxResult.setRetCode("1");
				oRespDoTxResult.setTxHash(encApi.hexEnc(txHash.toByteArray()));
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
