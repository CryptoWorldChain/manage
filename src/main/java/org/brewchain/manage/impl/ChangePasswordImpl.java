package org.brewchain.manage.impl;

import org.brewchain.account.core.AccountHelper;
import org.brewchain.account.gens.Actimpl.PACTCommand;
import org.brewchain.account.gens.Actimpl.PACTModule;
import org.brewchain.bcapi.gens.Oentity.OValue;
import org.brewchain.manage.dao.ManageDaos;
import org.brewchain.manage.gens.Manage.AdministratorAccountValue;
import org.brewchain.manage.gens.Manageimpl.PMANCommand;
import org.brewchain.manage.gens.Manageimpl.PMANModule;
import org.brewchain.manage.gens.Manageimpl.ReqChangePassword;
import org.brewchain.manage.gens.Manageimpl.RespChangePassword;
import org.brewchain.manage.util.ManageKeys;
import org.brewchain.manage.util.OEntityBuilder;
import org.fc.brewchain.bcapi.EncAPI;
import org.fc.brewchain.bcapi.IDGenerator;

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
public class ChangePasswordImpl extends SessionModules<ReqChangePassword> {
	@ActorRequire(name = "Account_Helper", scope = "global")
	AccountHelper oAccountHelper;
	@ActorRequire(name = "bc_encoder", scope = "global")
	EncAPI encApi;
	@ActorRequire(name = "man_Daos", scope = "global")
	ManageDaos dao;

	@Override
	public String[] getCmds() {
		return new String[] { PMANCommand.CPW.name() };
	}

	@Override
	public String getModule() {
		return PMANModule.MAN.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqChangePassword pb, final CompleteHandler handler) {
		RespChangePassword.Builder oRespChangePassword = RespChangePassword.newBuilder();
		try {
			OValue oOValue = dao.getManageDao()
					.get(OEntityBuilder.byteKey2OKey(ManageKeys.ADMINISTRATOR_KEY.getBytes())).get();
			if (oOValue == null || oOValue.getExtdata() == null || oOValue.getExtdata().equals(ByteString.EMPTY)) {
				oRespChangePassword.setRetCode("-1");
				oRespChangePassword.setRetMsg("账户不存在");
				handler.onFinished(PacketHelper.toPBReturn(pack, oRespChangePassword.build()));
				return;
			}

			AdministratorAccountValue.Builder oAdministratorAccountValue = AdministratorAccountValue
					.parseFrom(oOValue.getExtdata()).toBuilder();
			if (oAdministratorAccountValue.getPwd()
					.equals(encApi.hexEnc(encApi.sha256Encode(pb.getOld().getBytes())))) {
				String token = IDGenerator.getInstance().generate();

				oAdministratorAccountValue.setPwd(encApi.hexEnc(encApi.sha256Encode(pb.getNew().getBytes())));
				oAdministratorAccountValue.setToken(token);
				oAdministratorAccountValue.setLastLoginTime(System.currentTimeMillis());

				dao.getManageDao().put(OEntityBuilder.byteKey2OKey(ManageKeys.ADMINISTRATOR_KEY.getBytes()),
						OEntityBuilder.byteValue2OValue(oAdministratorAccountValue.build().toByteArray()));

				oRespChangePassword.setRetCode("1");
				oRespChangePassword.setRetMsg("修改成功");
				handler.onFinished(PacketHelper.toPBReturn(pack, oRespChangePassword.build()));
				return;
			} else {
				oRespChangePassword.setRetCode("-1");
				oRespChangePassword.setRetMsg("密码错误");
				handler.onFinished(PacketHelper.toPBReturn(pack, oRespChangePassword.build()));
				return;
			}
		} catch (Exception e) {
			oRespChangePassword.setRetCode("-1");
			oRespChangePassword.setRetMsg("未知异常:" + e.getMessage());
			handler.onFinished(PacketHelper.toPBReturn(pack, oRespChangePassword.build()));
			return;
		}
	}
}
