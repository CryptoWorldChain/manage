package org.brewchain.manage.impl;

import java.io.BufferedReader;
import java.io.FileReader;

import org.brewchain.bcapi.gens.Oentity.KeyStoreValue;
import org.brewchain.manage.dao.ManageDaos;
import org.brewchain.manage.gens.Manageimpl.PMANCommand;
import org.brewchain.manage.gens.Manageimpl.PMANModule;
import org.brewchain.manage.gens.Manageimpl.ReqExportNodeAccount;
import org.brewchain.manage.gens.Manageimpl.RespExportNodeAccount;
import org.fc.brewchain.bcapi.EncAPI;
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
			String keyStoreJsonStr = "";
			
			String line = br.readLine();    
			while(line != null){
				keyStoreJsonStr += line.trim().replace("\r", "").replace("\t", "");
			   line = br.readLine();
			}
			br.close();
			fr.close();
			
			KeyStoreValue oKeyStoreValue = keyStoreHelper.getKeyStore(keyStoreJsonStr, pb.getPwd());
			if (oKeyStoreValue == null) {
				oRespExportNodeAccount.setRetCode("-1");
				oRespExportNodeAccount.setRetMsg("pwd or jsonstr error");
			}else {
				oRespExportNodeAccount.setRetCode("1");
				oRespExportNodeAccount
				.setKeyStoreJsonStr(keyStoreHelper.parseToJsonStr(keyStoreHelper.parse(keyStoreJsonStr)));
			}
			
		} catch (Exception e) {
			if (e.getMessage() != null) {
				oRespExportNodeAccount.setRetMsg(e.getMessage());
			}
			oRespExportNodeAccount.setRetCode("-1");
		}
		handler.onFinished(PacketHelper.toPBReturn(pack, oRespExportNodeAccount.build()));
	}
}
