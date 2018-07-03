package org.brewchain.manage.impl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.brewchain.manage.util.OEntityBuilder;
import org.brewchain.account.core.BlockChainConfig;
import org.brewchain.account.core.KeyConstant;
import org.brewchain.bcapi.gens.Oentity.KeyStoreValue;
import org.brewchain.bcapi.gens.Oentity.OKey;
import org.brewchain.bcapi.gens.Oentity.OValue;
import org.brewchain.manage.dao.ManageDaos;
import org.brewchain.manage.gens.Manageimpl.PMANCommand;
import org.brewchain.manage.gens.Manageimpl.PMANModule;
import org.brewchain.manage.gens.Manageimpl.ReqSetNodeAccount;
import org.brewchain.manage.gens.Manageimpl.ReqSetNodeAccountByPriv;
import org.brewchain.manage.gens.Manageimpl.RespSetNodeAccount;
import org.brewchain.manage.util.ManageKeys;
import org.fc.brewchain.bcapi.EncAPI;
import org.fc.brewchain.bcapi.KeyStoreFile;
import org.fc.brewchain.bcapi.KeyStoreHelper;

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
public class SetNodeAccountByPrivImpl extends SessionModules<ReqSetNodeAccountByPriv> {
	@ActorRequire(name = "man_Daos", scope = "global")
	ManageDaos dao;
	@ActorRequire(name = "bc_encoder", scope = "global")
	EncAPI encApi;
	@ActorRequire(name = "KeyStore_Helper", scope = "global")
	KeyStoreHelper keyStoreHelper;
	@ActorRequire(name = "BlockChain_Config", scope = "global")
	BlockChainConfig blockChainConfig;

	@Override
	public String[] getCmds() {
		return new String[] { PMANCommand.SNP.name() };
	}

	@Override
	public String getModule() {
		return PMANModule.MAN.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqSetNodeAccountByPriv pb, final CompleteHandler handler) {
		RespSetNodeAccount.Builder oRespSetNodeAccount = RespSetNodeAccount.newBuilder();

		String address = encApi.priKeyToAddress(pb.getPriv());

		// 写入文件夹
		BufferedWriter bw = null;
		FileWriter fw = null;
		try {
			KeyStoreFile oKeyStoreFile = keyStoreHelper.generate(address, pb.getPriv(), "", "", pb.getPwd());
			String keyStoreFileStr = keyStoreHelper.parseToJsonStr(oKeyStoreFile);
			KeyStoreValue oKeyStoreValue = keyStoreHelper.getKeyStore(keyStoreFileStr, pb.getPwd());

			File keyStoreFile = new File("keystore" + File.separator + blockChainConfig.getNet() +  File.separator + "keystore" + blockChainConfig.getKeystoreNumber() + ".json");
			if (keyStoreFile.exists()) {
				keyStoreFile.delete();
			}
			if (!keyStoreFile.createNewFile()) {
				oRespSetNodeAccount.setRetCode("-1");
				oRespSetNodeAccount.setRetMsg("设置账户失败");
			} else {
				fw = new FileWriter(keyStoreFile);
				bw = new BufferedWriter(fw);
				bw.write(keyStoreFileStr);
				bw.close();
				fw.close();

				oRespSetNodeAccount.setRetCode("1");

				dao.getAccountDao().put(OEntityBuilder.byteKey2OKey(ManageKeys.NODE_ACCOUNT.getBytes()),
						OEntityBuilder.byteValue2OValue(oKeyStoreValue.toByteArray()));
				KeyConstant.PWD = pb.getPwd();
			}
		} catch (Exception e) {
			log.error("error on read network::" + e.getMessage());
			if (fw != null) {
				try {
					fw.close();
				} catch (IOException e1) {
				}
			}
			if (bw != null) {
				try {
					bw.close();
				} catch (IOException e1) {
				}
			}

			oRespSetNodeAccount.clear();
			oRespSetNodeAccount.setRetCode("-1");
			if (e.getMessage() != null)
				oRespSetNodeAccount.setRetMsg(e.getMessage());
		}

		handler.onFinished(PacketHelper.toPBReturn(pack, oRespSetNodeAccount.build()));
	}
}
