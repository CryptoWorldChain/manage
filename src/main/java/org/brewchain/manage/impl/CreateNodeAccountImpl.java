package org.brewchain.manage.impl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.brewchain.account.util.OEntityBuilder;
import org.brewchain.manage.dao.ManageDaos;
import org.brewchain.manage.gens.Manageimpl.PMANCommand;
import org.brewchain.manage.gens.Manageimpl.PMANModule;
import org.brewchain.manage.gens.Manageimpl.ReqCheckIsFirstOpen;
import org.brewchain.manage.gens.Manageimpl.ReqCreateNewAccount;
import org.brewchain.manage.gens.Manageimpl.ReqSetNodeAccount;
import org.brewchain.manage.gens.Manageimpl.RespCreateNewAccount;
import org.brewchain.manage.gens.Manageimpl.RespSetNodeAccount;
import org.brewchain.manage.util.KeyStoreHelper;
import org.brewchain.manage.util.ManageKeys;
import org.fc.brewchain.bcapi.EncAPI;
import org.fc.brewchain.bcapi.KeyPairs;

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
public class CreateNodeAccountImpl extends SessionModules<ReqCreateNewAccount> {
	@ActorRequire(name = "man_Daos", scope = "global")
	ManageDaos dao;
	@ActorRequire(name = "bc_encoder", scope = "global")
	EncAPI encApi;
	@ActorRequire(name = "KeyStore_Helper", scope = "global")
	KeyStoreHelper keyStoreHelper;

	@Override
	public String[] getCmds() {
		return new String[] { PMANCommand.CNA.name() };
	}

	@Override
	public String getModule() {
		return PMANModule.MAN.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqCreateNewAccount pb, final CompleteHandler handler) {
		RespCreateNewAccount.Builder oRespCreateNewAccount = RespCreateNewAccount.newBuilder();

		KeyPairs oKeyPairs = encApi.genKeys();
		String keyStoreFileStr = keyStoreHelper.generate(oKeyPairs, pb.getPwd());
		// 写入文件夹
		BufferedWriter bw = null;
		FileWriter fw = null;
		try {
			File keyStoreFile = new File(".keystore");
			if (keyStoreFile.exists()) {
				keyStoreFile.delete();
			}
			if (!keyStoreFile.createNewFile()) {
				oRespCreateNewAccount.setRetCode("-1");
				oRespCreateNewAccount.setRetMsg("设置账户失败");
			} else {
				fw = new FileWriter(keyStoreFile);
				bw = new BufferedWriter(fw);
				bw.write(keyStoreFileStr);
				bw.close();
				fw.close();

				oRespCreateNewAccount.setRetCode("1");
				dao.getManageDao().put(OEntityBuilder.byteKey2OKey(ManageKeys.NODE_ADDRESS.getBytes()),
						OEntityBuilder.byteValue2OValue(encApi.hexDec(oKeyPairs.getAddress())));
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

			oRespCreateNewAccount.clear();
			oRespCreateNewAccount.setRetCode("-1");
			if (e.getMessage() != null)
				oRespCreateNewAccount.setRetMsg(e.getMessage());
		}
		handler.onFinished(PacketHelper.toPBReturn(pack, oRespCreateNewAccount.build()));
	}
}