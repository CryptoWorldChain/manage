package org.brewchain.manage.impl;

import java.util.Date;

import org.brewchain.account.util.OEntityBuilder;
import org.brewchain.bcapi.gens.Oentity.OValue;
import org.brewchain.manage.dao.ManageDaos;
import org.brewchain.manage.gens.Manage.AdministratorAccountValue;
import org.brewchain.manage.gens.Manageimpl.PMANCommand;
import org.brewchain.manage.gens.Manageimpl.PMANModule;
import org.brewchain.manage.gens.Manageimpl.ReqRegister;
import org.brewchain.manage.gens.Manageimpl.RespRegister;
import org.brewchain.manage.util.IDGenerator;
import org.brewchain.manage.util.ManageKeys;
import org.fc.brewchain.bcapi.EncAPI;

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
public class RegisterImpl extends SessionModules<ReqRegister> {
	@ActorRequire(name = "man_Daos", scope = "global")
	ManageDaos dao;
	@ActorRequire(name = "bc_encoder", scope = "global")
	EncAPI encApi;

	@Override
	public String[] getCmds() {
		return new String[] { PMANCommand.RAA.name() };
	}

	@Override
	public String getModule() {
		return PMANModule.MAN.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqRegister pb, final CompleteHandler handler) {
		RespRegister.Builder oRespRegister = RespRegister.newBuilder();
		try {
			OValue oOValue = dao.getManageDao()
					.get(OEntityBuilder.byteKey2OKey(ManageKeys.ADMINISTRATOR_KEY.getBytes())).get();
			if (oOValue != null && oOValue.getExtdata() != null && !oOValue.getExtdata().equals(ByteString.EMPTY)) {
				oRespRegister.setRetCode("-1");
				oRespRegister.setRetMsg("账户已存在");
				handler.onFinished(PacketHelper.toPBReturn(pack, oRespRegister.build()));
				return;
			}

			String token = IDGenerator.getInstance().generate();

			AdministratorAccountValue.Builder oAdministratorAccountValue = AdministratorAccountValue.newBuilder();
			oAdministratorAccountValue.setPwd(encApi.hexEnc(encApi.sha256Encode(pb.getPwd().getBytes())));
			oAdministratorAccountValue.setToken(token);
			oAdministratorAccountValue.setLastLoginTime((new Date()).getTime());

			dao.getManageDao().put(OEntityBuilder.byteKey2OKey(ManageKeys.ADMINISTRATOR_KEY.getBytes()),
					OEntityBuilder.byteValue2OValue(oAdministratorAccountValue.build().toByteArray()));

			oRespRegister.setRetCode("1");
			oRespRegister.setToken(token);
		} catch (Exception e) {
			oRespRegister.setRetCode("-1");
			if (e.getMessage() != null)
				oRespRegister.setRetMsg(e.getMessage());
		}
		handler.onFinished(PacketHelper.toPBReturn(pack, oRespRegister.build()));
	}
}
