package org.brewchain.manage.impl;

import java.util.ArrayList;
import java.util.List;

import org.brewchain.account.core.AccountHelper;
import org.brewchain.bcapi.gens.Oentity.OValue;
import org.brewchain.core.util.ByteUtil;
import org.brewchain.evmapi.gens.Act.Account;
import org.brewchain.evmapi.gens.Act.ERC20TokenValue;
import org.brewchain.manage.dao.ManageDaos;
import org.brewchain.manage.gens.Manageimpl.MsgContract;
import org.brewchain.manage.gens.Manageimpl.MsgToken;
import org.brewchain.manage.gens.Manageimpl.PMANCommand;
import org.brewchain.manage.gens.Manageimpl.PMANModule;
import org.brewchain.manage.gens.Manageimpl.ReqGetContractList;
import org.brewchain.manage.gens.Manageimpl.ReqGetNetwork;
import org.brewchain.manage.gens.Manageimpl.ReqQueryToken;
import org.brewchain.manage.gens.Manageimpl.RespGetContractList;
import org.brewchain.manage.gens.Manageimpl.RespQueryToken;
import org.brewchain.manage.util.ManageKeys;
import org.brewchain.manage.util.OEntityBuilder;
import org.fc.brewchain.bcapi.EncAPI;
import org.fc.brewchain.bcapi.UnitUtil;

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
public class GetTokenListImpl extends SessionModules<ReqQueryToken> {
	@ActorRequire(name = "man_Daos", scope = "global")
	ManageDaos dao;
	@ActorRequire(name = "bc_encoder", scope = "global")
	EncAPI encApi;
	@ActorRequire(name = "Account_Helper", scope = "global")
	AccountHelper oAccountHelper;

	@Override
	public String[] getCmds() {
		return new String[] { PMANCommand.GTT.name() };
	}

	@Override
	public String getModule() {
		return PMANModule.MAN.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqQueryToken pb, final CompleteHandler handler) {
		RespQueryToken.Builder oRespQueryToken = RespQueryToken.newBuilder();
		try {
			OValue oOValue = dao.getManageDao().get(OEntityBuilder.byteKey2OKey(ManageKeys.NODE_ACCOUNT.getBytes()))
					.get();
			if (oOValue == null || oOValue.getExtdata() == null || oOValue.getExtdata().equals(ByteString.EMPTY)) {
				oRespQueryToken.setRetCode("-1");
				oRespQueryToken.setRetMsg("没有找到节点账户");
			} else {
				List<ERC20TokenValue> list = oAccountHelper.getTokens(pb.getAddress(), pb.getToken());
				for (ERC20TokenValue token : list) {
					MsgToken.Builder oMsgToken = MsgToken.newBuilder();
					oMsgToken.setTimestamp(String.valueOf(token.getTimestamp()));
					oMsgToken.setToken(token.getToken());
					oMsgToken.setTotal(String
							.valueOf(UnitUtil.fromWei(ByteUtil.bytesToBigInteger(token.getTotal().toByteArray()))));
					oRespQueryToken.addTokens(oMsgToken.build());
				}
				oRespQueryToken.setRetCode("1");
			}
		} catch (Exception e) {
			if (e.getMessage() != null) {
				oRespQueryToken.setRetMsg(e.getMessage());
			}
			oRespQueryToken.setRetCode("-1");
		}

		handler.onFinished(PacketHelper.toPBReturn(pack, oRespQueryToken.build()));
		return;
	}
}
