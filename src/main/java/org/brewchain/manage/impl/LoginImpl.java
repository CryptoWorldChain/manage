package org.brewchain.manage.impl;

import java.util.Date;

import org.brewchain.account.util.OEntityBuilder;
import org.brewchain.bcapi.gens.Oentity.OValue;
import org.brewchain.manage.dao.DefDaos;
import org.brewchain.manage.gens.Manage.AdministratorAccountValue;
import org.brewchain.manage.gens.Manageimpl.PMANCommand;
import org.brewchain.manage.gens.Manageimpl.PMANModule;
import org.brewchain.manage.gens.Manageimpl.ReqLogin;
import org.brewchain.manage.gens.Manageimpl.ReqRegister;
import org.brewchain.manage.gens.Manageimpl.RespLogin;
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
public class LoginImpl extends SessionModules<ReqLogin> {
	@ActorRequire(name = "Def_Daos", scope = "global")
	DefDaos dao;
	@ActorRequire(name = "bc_encoder", scope = "global")
	EncAPI encApi;

	@Override
	public String[] getCmds() {
		return new String[] { PMANCommand.LGI.name() };
	}

	@Override
	public String getModule() {
		return PMANModule.MAN.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqLogin pb, final CompleteHandler handler) {
		RespLogin.Builder oRespLogin = RespLogin.newBuilder();
		try {
			OValue oOValue = dao.getManageDao()
					.get(OEntityBuilder.byteKey2OKey(encApi.hexDec(ManageKeys.ADMINISTRATOR_KEY))).get();
			if (oOValue == null || oOValue.getExtdata() == null || oOValue.getExtdata().equals(ByteString.EMPTY)) {
				oRespLogin.setRetCode("-1");
				oRespLogin.setRetMsg("账户不存在");
				handler.onFinished(PacketHelper.toPBReturn(pack, oRespLogin.build()));
				return;
			}

			AdministratorAccountValue.Builder oAdministratorAccountValue = AdministratorAccountValue
					.parseFrom(oOValue.getExtdata()).toBuilder();
			if (oAdministratorAccountValue.getPwd()
					.equals(encApi.hexEnc(encApi.sha256Encode(pb.getPwd().getBytes())))) {
				String token = IDGenerator.nextID();

				oRespLogin.setRetCode("1");
				oRespLogin.setToken(token);
				
				oAdministratorAccountValue.setToken(token);
				oAdministratorAccountValue.setLastLoginTime((new Date()).getTime());

				dao.getManageDao().put(OEntityBuilder.byteKey2OKey(encApi.hexDec(ManageKeys.ADMINISTRATOR_KEY)),
						OEntityBuilder.byteValue2OValue(oAdministratorAccountValue.build().toByteArray()));

				handler.onFinished(PacketHelper.toPBReturn(pack, oRespLogin.build()));
				return;
			} else {
				oRespLogin.setRetCode("-1");
				oRespLogin.setRetMsg("密码错误");
				handler.onFinished(PacketHelper.toPBReturn(pack, oRespLogin.build()));
				return;
			}
		} catch (Exception e) {
			oRespLogin.setRetCode("-1");
			if (e.getMessage() != null)
				oRespLogin.setRetMsg(e.getMessage());
		}
		handler.onFinished(PacketHelper.toPBReturn(pack, oRespLogin.build()));
	}
}
