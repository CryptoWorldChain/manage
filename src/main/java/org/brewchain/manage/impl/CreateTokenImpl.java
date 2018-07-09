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
import org.brewchain.evmapi.gens.Tx.MultiTransaction;
import org.brewchain.evmapi.gens.Tx.MultiTransactionBody;
import org.brewchain.evmapi.gens.Tx.MultiTransactionInput;
import org.brewchain.evmapi.gens.Tx.MultiTransactionSignature;
import org.brewchain.manage.dao.ManageDaos;
import org.brewchain.manage.gens.Manageimpl.PMANCommand;
import org.brewchain.manage.gens.Manageimpl.PMANModule;
import org.brewchain.manage.gens.Manageimpl.ReqCreateContract;
import org.brewchain.manage.gens.Manageimpl.ReqCreateNewAccount;
import org.brewchain.manage.gens.Manageimpl.ReqCreateToken;
import org.brewchain.manage.gens.Manageimpl.RespCreateContract;
import org.brewchain.manage.gens.Manageimpl.RespCreateNewAccount;
import org.brewchain.manage.gens.Manageimpl.RespCreateToken;
import org.brewchain.rcvm.utils.ByteUtil;
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
public class CreateTokenImpl extends SessionModules<ReqCreateToken> {
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
		return new String[] { PMANCommand.CTT.name() };
	}

	@Override
	public String getModule() {
		return PMANModule.MAN.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqCreateToken pb, final CompleteHandler handler) {
		RespCreateToken.Builder oRespCreateToken = RespCreateToken.newBuilder();

		try {
			if (StringUtils.isBlank(pb.getPwd())) {
				oRespCreateToken.setRetCode("-1");
				oRespCreateToken.setRetMsg("请输入账户密码");
				handler.onFinished(PacketHelper.toPBReturn(pack, oRespCreateToken.build()));
				return;
			}

			if (pb.getToken().length() > 8 || pb.getToken().length() < 3) {
				oRespCreateToken.setRetCode("-1");
				oRespCreateToken.setRetMsg("Token名称长度必须介于3到8之间");
				handler.onFinished(PacketHelper.toPBReturn(pack, oRespCreateToken.build()));
				return;
			}

			if (pb.getToken().toUpperCase().startsWith("CW")) {
				oRespCreateToken.setRetCode("-1");
				oRespCreateToken.setRetMsg("Token名称不能已CW开头");
				handler.onFinished(PacketHelper.toPBReturn(pack, oRespCreateToken.build()));
				return;
			}

			if (UnitUtil.compareTo(blockChainConfig.getMaxTokenTotal(), UnitUtil.toWei(pb.getTotal())) == -1
					|| UnitUtil.compareTo(UnitUtil.toWei(pb.getTotal()), blockChainConfig.getMinTokenTotal()) == -1) {
				oRespCreateToken.setRetCode("-1");
				oRespCreateToken.setRetMsg("Token发行总量输入错误");
				handler.onFinished(PacketHelper.toPBReturn(pack, oRespCreateToken.build()));
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
					oRespCreateToken.setRetCode("-1");
					oRespCreateToken.setRetMsg("keystore file or password invalid");
				} else {
					MultiTransaction.Builder oMultiTransaction = MultiTransaction.newBuilder();
					MultiTransactionBody.Builder oMultiTransactionBody = MultiTransactionBody.newBuilder();

					MultiTransactionInput.Builder oMultiTransactionInput4 = MultiTransactionInput.newBuilder();
					oMultiTransactionInput4.setAddress(ByteString.copyFrom(encApi.hexDec(oKeyStoreValue.getAddress())));
					oMultiTransactionInput4.setAmount(ByteString.copyFrom(
							ByteUtil.bigIntegerToBytes(new BigInteger(pb.getTotal() + "000000000000000000"))));
					int nonce = oAccountHelper
							.getNonce(ByteString.copyFrom(encApi.hexDec(oKeyStoreValue.getAddress())));
					oMultiTransactionInput4.setNonce(nonce);
					oMultiTransactionInput4.setPubKey(ByteString.copyFrom(encApi.hexDec(oKeyStoreValue.getPubkey())));
					oMultiTransactionInput4.setToken(pb.getToken().toUpperCase());
					oMultiTransactionBody.addInputs(oMultiTransactionInput4);
					oMultiTransactionBody.setType(TransTypeEnum.TYPE_CreateToken.value());
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
					String txHash = transactionHelper.CreateMultiTransaction(oMultiTransaction);
					oRespCreateToken.setTxHash(txHash);
					oRespCreateToken.setRetCode("1");
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
			oRespCreateToken.setRetCode("-1");
			oRespCreateToken.setRetMsg("未知异常:" + e.getMessage());
		}
		handler.onFinished(PacketHelper.toPBReturn(pack, oRespCreateToken.build()));
		return;
	}
}
