package org.brewchain.manage.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;

import org.brewchain.account.util.OEntityBuilder;
import org.brewchain.manage.dao.ManageDaos;
import org.brewchain.manage.gens.Manageimpl.PMANCommand;
import org.brewchain.manage.gens.Manageimpl.PMANModule;
import org.brewchain.manage.gens.Manageimpl.ReqExportNodeAccount;
import org.brewchain.manage.gens.Manageimpl.ReqSetNodeAccount;
import org.brewchain.manage.gens.Manageimpl.RespExportNodeAccount;
import org.brewchain.manage.gens.Manageimpl.RespSetNodeAccount;
import org.brewchain.manage.util.KeyStoreHelper;
import org.brewchain.manage.util.ManageKeys;
import org.fc.brewchain.bcapi.EncAPI;

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
public class ExportNodeAccountImpl extends SessionModules<ReqExportNodeAccount> {
	@ActorRequire(name = "man_Daos", scope = "global")
	ManageDaos dao;
	@ActorRequire(name = "bc_encoder", scope = "global")
	EncAPI encApi;
	@ActorRequire(name = "KeyStore_Helper", scope = "global")
	KeyStoreHelper keyStoreHelper;

	@Override
	public String[] getCmds() {
		return new String[] { PMANCommand.ENA.name() };
	}

	@Override
	public String getModule() {
		return PMANModule.MAN.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqExportNodeAccount pb, final CompleteHandler handler) {
		RespExportNodeAccount.Builder oRespExportNodeAccount = RespExportNodeAccount.newBuilder();

		try {
			// read file
			FileReader fr = new FileReader(".keystore");
			BufferedReader br = new BufferedReader(fr);
			String keyStoreJsonStr = br.readLine().trim().replace("\r", "").replace("\t", "");
			br.close();
			fr.close();
			oRespExportNodeAccount.setRetCode("1");
			oRespExportNodeAccount
					.setKeyStoreJsonStr(keyStoreHelper.parseToJsonStr(keyStoreHelper.parse(keyStoreJsonStr)));
		} catch (Exception e) {
			if (e.getMessage() != null) {
				oRespExportNodeAccount.setRetMsg(e.getMessage());
			}
			oRespExportNodeAccount.setRetCode("-1");
		}
		handler.onFinished(PacketHelper.toPBReturn(pack, oRespExportNodeAccount.build()));
	}
}
