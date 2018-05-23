package org.brewchain.manage.impl;

import org.brewchain.manage.util.ManageKeys;
import org.brewchain.manage.util.OEntityBuilder;
import org.fc.brewchain.bcapi.EncAPI;

import com.google.protobuf.ByteString;

import java.util.concurrent.ExecutionException;

import org.brewchain.bcapi.backend.ODBException;
import org.brewchain.bcapi.gens.Oentity.OValue;
import org.brewchain.manage.dao.DefDaos;
import org.brewchain.manage.gens.Manageimpl.PMANCommand;
import org.brewchain.manage.gens.Manageimpl.PMANModule;
import org.brewchain.manage.gens.Manageimpl.ReqCheckIsFirstOpen;
import org.brewchain.manage.gens.Manageimpl.RespCheckIsFirstOpen;

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
public class CheckIsFirstOpenImpl extends SessionModules<ReqCheckIsFirstOpen> {
	@ActorRequire(name = "Def_Daos", scope = "global")
	DefDaos dao;
	@ActorRequire(name = "bc_encoder", scope = "global")
	EncAPI encApi;

	@Override
	public String[] getCmds() {
		return new String[] { PMANCommand.CIA.name() };
	}

	@Override
	public String getModule() {
		return PMANModule.MAN.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqCheckIsFirstOpen pb, final CompleteHandler handler) {
		org.brewchain.manage.gens.Manageimpl.RespCheckIsFirstOpen.Builder oRespCheckIsFirstOpen = RespCheckIsFirstOpen
				.newBuilder();

		try {
			OValue oOValue = dao.getManageDao()
					.get(OEntityBuilder.byteKey2OKey(encApi.hexDec(ManageKeys.ADMINISTRATOR_KEY))).get();

			if (oOValue == null || oOValue.getExtdata() == null || oOValue.getExtdata().equals(ByteString.EMPTY)) {
				oRespCheckIsFirstOpen.setRetCode("1");
			} else {
				oRespCheckIsFirstOpen.setRetCode("-1");
			}
		} catch (Exception e) {
			oRespCheckIsFirstOpen.setRetCode("-2");
			if (e.getMessage() != null)
				oRespCheckIsFirstOpen.setRetMsg(e.getMessage());
		}
		handler.onFinished(PacketHelper.toPBReturn(pack, oRespCheckIsFirstOpen.build()));
	}
}
