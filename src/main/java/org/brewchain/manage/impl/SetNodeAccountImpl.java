package org.brewchain.manage.impl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.brewchain.account.util.OEntityBuilder;
import org.brewchain.manage.dao.ManageDaos;
import org.brewchain.manage.gens.Manageimpl.PMANCommand;
import org.brewchain.manage.gens.Manageimpl.PMANModule;
import org.brewchain.manage.gens.Manageimpl.ReqSetNodeAccount;
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
public class SetNodeAccountImpl extends SessionModules<ReqSetNodeAccount> {
	@ActorRequire(name = "man_Daos", scope = "global")
	ManageDaos dao;
	@ActorRequire(name = "bc_encoder", scope = "global")
	EncAPI encApi;
	@ActorRequire(name = "KeyStore_Helper", scope = "global")
	KeyStoreHelper keyStoreHelper;

	@Override
	public String[] getCmds() {
		return new String[] { PMANCommand.SNA.name() };
	}

	@Override
	public String getModule() {
		return PMANModule.MAN.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqSetNodeAccount pb, final CompleteHandler handler) {
		RespSetNodeAccount.Builder oRespSetNodeAccount = RespSetNodeAccount.newBuilder();

		try {
			byte[] priv = keyStoreHelper.getPriv(pb.getKeyStoreJsonStr(), pb.getPwd());
			// TODO : address
			byte[] address = null;

			BufferedWriter bw = null;
			FileWriter fw = null;
			try {
				File keyStoreFile = new File(".keystore");
				if (keyStoreFile.exists()) {
					keyStoreFile.delete();
				}
				if (!keyStoreFile.createNewFile()) {
					oRespSetNodeAccount.setRetCode("-1");
					oRespSetNodeAccount.setRetMsg("设置账户失败");
				} else {
					fw = new FileWriter(keyStoreFile);
					bw = new BufferedWriter(fw);
					bw.write(pb.getKeyStoreJsonStr());
					bw.close();
					fw.close();

					oRespSetNodeAccount.setRetCode("1");
					dao.getManageDao().put(OEntityBuilder.byteKey2OKey(ManageKeys.NODE_ADDRESS.getBytes()),
							OEntityBuilder.byteValue2OValue(address));
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
		} catch (Exception e) {
			if (e.getMessage() != null) {
				oRespSetNodeAccount.setRetMsg(e.getMessage());
			}
			oRespSetNodeAccount.setRetCode("-1");
		}
		handler.onFinished(PacketHelper.toPBReturn(pack, oRespSetNodeAccount.build()));
	}
}
