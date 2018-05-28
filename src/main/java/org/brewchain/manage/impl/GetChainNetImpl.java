package org.brewchain.manage.impl;

import java.util.concurrent.ExecutionException;

import org.brewchain.account.util.OEntityBuilder;
import org.brewchain.bcapi.backend.ODBException;
import org.brewchain.bcapi.gens.Oentity.OValue;
import org.brewchain.manage.dao.ManageDaos;
import org.brewchain.manage.gens.Manageimpl.PMANCommand;
import org.brewchain.manage.gens.Manageimpl.PMANModule;
import org.brewchain.manage.gens.Manageimpl.ReqGetNetwork;
import org.brewchain.manage.gens.Manageimpl.ReqSetNetwork;
import org.brewchain.manage.gens.Manageimpl.RespGetNetwork;
import org.brewchain.manage.gens.Manageimpl.RespSetNetwork;
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
public class GetChainNetImpl extends SessionModules<ReqGetNetwork> {
	@ActorRequire(name = "man_Daos", scope = "global")
	ManageDaos dao;
	@ActorRequire(name = "bc_encoder", scope = "global")
	EncAPI encApi;

	@Override
	public String[] getCmds() {
		return new String[] { PMANCommand.GNW.name() };
	}

	@Override
	public String getModule() {
		return PMANModule.MAN.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqGetNetwork pb, final CompleteHandler handler) {
		RespGetNetwork.Builder oRespGetNetwork = RespGetNetwork.newBuilder();

		try {
			OValue oOValue = dao.getManageDao().get(OEntityBuilder.byteKey2OKey(ManageKeys.NODE_NET.getBytes())).get();
			if (oOValue == null || oOValue.getExtdata() == null || oOValue.getExtdata().equals(ByteString.EMPTY)) {
				String net = this.props().get(ManageKeys.NODE_NET, null);
				if (net == null) {
					oRespGetNetwork.setRetCode("-1");
					oRespGetNetwork.setRetMsg("chain net not found");
				} else {
					oRespGetNetwork.setRetCode("1");
					oRespGetNetwork.setNetwork(net);
				}
			} else {
				oRespGetNetwork.setRetCode("1");
				oRespGetNetwork.setNetwork(ByteString.copyFrom(oOValue.getExtdata().toByteArray()).toStringUtf8());
			}
		} catch (Exception e) {
			oRespGetNetwork.setRetCode("-1");
			oRespGetNetwork.setRetMsg(e.getMessage());
		}
		handler.onFinished(PacketHelper.toPBReturn(pack, oRespGetNetwork.build()));

	}
}
