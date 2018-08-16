package org.brewchain.manage.impl;

import java.io.BufferedReader;
import java.io.FileReader;

import org.brewchain.bcapi.gens.Oentity.KeyStoreValue;
import org.brewchain.bcapi.gens.Oentity.OValue;
import org.brewchain.manage.dao.ManageDaos;
import org.brewchain.manage.gens.Manageimpl.PMANCommand;
import org.brewchain.manage.gens.Manageimpl.PMANModule;
import org.brewchain.manage.gens.Manageimpl.ReqExportNodeAccount;
import org.brewchain.manage.gens.Manageimpl.ReqGetNodeAccount;
import org.brewchain.manage.gens.Manageimpl.RespExportNodeAccount;
import org.brewchain.manage.gens.Manageimpl.RespGetNodeAccount;
import org.brewchain.manage.util.ManageKeys;
import org.brewchain.manage.util.OEntityBuilder;
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
public class GetNodeAccountImpl extends SessionModules<ReqGetNodeAccount> {
	@ActorRequire(name = "man_Daos", scope = "global")
	ManageDaos dao;
	@ActorRequire(name = "bc_encoder", scope = "global")
	EncAPI encApi;
	@ActorRequire(name = "KeyStore_Helper", scope = "global")
	KeyStoreHelper keyStoreHelper;

	@Override
	public String[] getCmds() {
		return new String[] { PMANCommand.GNA.name() };
	}

	@Override
	public String getModule() {
		return PMANModule.MAN.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqGetNodeAccount pb, final CompleteHandler handler) {
		RespGetNodeAccount.Builder oRespGetNodeAccount = RespGetNodeAccount.newBuilder();

		try {
			OValue oOValue = dao.getManageDao().get(OEntityBuilder.byteKey2OKey(ManageKeys.NODE_ACCOUNT.getBytes()))
					.get();
			if (oOValue == null || oOValue.getExtdata() == null || oOValue.getExtdata().equals(ByteString.EMPTY)) {
				oRespGetNodeAccount.setRetCode("-1");
				oRespGetNodeAccount.setRetMsg("没有找到节点账户");
			} else {
				oRespGetNodeAccount.setAddress(encApi.hexEnc(oOValue.getExtdata().toByteArray()));
				oRespGetNodeAccount.setRetCode("1");
			}
		} catch (Exception e) {
			if (e.getMessage() != null) {
				oRespGetNodeAccount.setRetMsg(e.getMessage());
			}
			oRespGetNodeAccount.setRetCode("-1");
		}
		handler.onFinished(PacketHelper.toPBReturn(pack, oRespGetNodeAccount.build()));
	}
}
