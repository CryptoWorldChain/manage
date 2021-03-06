package org.brewchain.manage.impl;

import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;

import org.brewchain.account.core.AccountHelper;
import org.brewchain.account.core.BlockChainConfig;
import org.brewchain.evmapi.gens.Act.Account;
import org.brewchain.evmapi.gens.Act.AccountTokenValue;
import org.brewchain.bcapi.gens.Oentity.OValue;
import org.brewchain.bcvm.utils.ByteUtil;
import org.brewchain.manage.dao.ManageDaos;
import org.brewchain.manage.gens.Manageimpl.PMANModule;
import org.brewchain.manage.gens.Node.DposNodeInfo;
import org.brewchain.manage.gens.Node.NodeNetwork;
import org.brewchain.manage.gens.Node.PNODCommand;
import org.brewchain.manage.gens.Node.RaftNodeInfo;
import org.brewchain.manage.gens.Node.ReqGetNodeInfo;
import org.brewchain.manage.gens.Node.RespGetNodeInfo;
import org.brewchain.manage.util.ManageKeys;
import org.brewchain.manage.util.OEntityBuilder;
import org.fc.brewchain.bcapi.EncAPI;
import org.fc.brewchain.bcapi.KeyStoreHelper;
import org.fc.brewchain.p22p.node.Network;

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
public class GetNodeInfoImpl extends SessionModules<ReqGetNodeInfo> {
	@ActorRequire(name = "man_Daos", scope = "global")
	ManageDaos dao;
	@ActorRequire(name = "bc_encoder", scope = "global")
	EncAPI encApi;
	@ActorRequire(name = "KeyStore_Helper", scope = "global")
	KeyStoreHelper keyStoreHelper;

	@ActorRequire(name = "BlockChain_Config", scope = "global")
	BlockChainConfig blockChainConfig;

	@ActorRequire(name = "Account_Helper", scope = "global")
	AccountHelper oAccountHelper;

	@Override
	public String[] getCmds() {
		return new String[] { PNODCommand.GNI.name() };
	}

	@Override
	public String getModule() {
		return PMANModule.MAN.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqGetNodeInfo pb, final CompleteHandler handler) {
		RespGetNodeInfo.Builder oRespGetNodeInfo = RespGetNodeInfo.newBuilder();

		Properties props = System.getProperties();
		oRespGetNodeInfo.setOsName(props.getProperty("os.name"));
		oRespGetNodeInfo.setOsTimeZone(TimeZone.getDefault().getDisplayName());
		oRespGetNodeInfo.setOsVersion(props.getProperty("os.version"));
		oRespGetNodeInfo.setServerTime(String.valueOf(System.currentTimeMillis()));
		oRespGetNodeInfo.setServerType(props.getProperty("os.arch"));

		Network oRaftNetwork = dao.getPzp().networkByID("raft");
		RaftNodeInfo.Builder oRaftNodeInfo = RaftNodeInfo.newBuilder();
		oRaftNodeInfo.setAddress(oRaftNetwork.root().v_address());
		oRaftNodeInfo.setBcuid(oRaftNetwork.root().bcuid());
		oRaftNodeInfo.setName(oRaftNetwork.root().name());
		oRaftNodeInfo.setNodeIdx(oRaftNetwork.root().node_idx());
		oRaftNodeInfo.setStartUpTime(oRaftNetwork.root().startup_time());
		oRaftNodeInfo.setUri(oRaftNetwork.root().uri());
		oRespGetNodeInfo.setRaft(oRaftNodeInfo);

		Network oDposNetwork = dao.getPzp().networkByID("dpos");
		DposNodeInfo.Builder oDposNodeInfo = DposNodeInfo.newBuilder();
		oDposNodeInfo.setAddress(oDposNetwork.root().v_address());
		oDposNodeInfo.setBcuid(oDposNetwork.root().bcuid());
		oDposNodeInfo.setName(oDposNetwork.root().name());
		oDposNodeInfo.setNodeIdx(oDposNetwork.root().node_idx());
		oDposNodeInfo.setStartUpTime(oDposNetwork.root().startup_time());
		oDposNodeInfo.setUri(oDposNetwork.root().uri());
		oRespGetNodeInfo.setDpos(oDposNodeInfo);

		// 尝试读取地址配置
		try {
			OValue oOValue = dao.getManageDao().get(OEntityBuilder.byteKey2OKey(ManageKeys.NODE_ACCOUNT.getBytes()))
					.get();
			if (oOValue == null || oOValue.getExtdata() == null || oOValue.getExtdata().equals(ByteString.EMPTY)) {

			} else {
				oRespGetNodeInfo.setAddress(encApi.hexEnc(oOValue.getExtdata().toByteArray()));
			}

			Account account = oAccountHelper.GetAccount(oOValue.getExtdata());
			List<AccountTokenValue> tokenValues = account.getValue().getTokensList();
			for (AccountTokenValue token : tokenValues) {
				if (token.getToken().equals("CWS")) {
					oRespGetNodeInfo
							.setCwstotal(String.valueOf(ByteUtil.bytesToBigInteger(token.getBalance().toByteArray())));
				}
			}
		} catch (Exception e) {
			log.error(e.getMessage());
		}

		// net
		try {
			OValue oOValue = dao.getManageDao().get(OEntityBuilder.byteKey2OKey(ManageKeys.NODE_NET.getBytes())).get();

			if (oOValue == null || oOValue.getExtdata() == null || oOValue.getExtdata().equals(ByteString.EMPTY)) {

			} else {
				NodeNetwork oNodeNetwork = NodeNetwork.parseFrom(oOValue.getExtdata());
				oRespGetNodeInfo.setNetwork(oNodeNetwork);
			}
		} catch (Exception e) {
			log.error("未知异常:" + e.getMessage());
		}

		handler.onFinished(PacketHelper.toPBReturn(pack, oRespGetNodeInfo.build()));
	}
}
