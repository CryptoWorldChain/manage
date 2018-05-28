package org.brewchain.manage.impl;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;
import org.brewchain.account.core.AccountHelper;
import org.brewchain.account.util.OEntityBuilder;
import org.brewchain.bcapi.gens.Oentity.OValue;
import org.brewchain.manage.dao.ManageDaos;
import org.brewchain.manage.gens.Manageimpl.PMANModule;
import org.brewchain.manage.gens.Node.DposNodeInfo;
import org.brewchain.manage.gens.Node.NodeNetwork;
import org.brewchain.manage.gens.Node.PNODCommand;
import org.brewchain.manage.gens.Node.RaftNodeInfo;
import org.brewchain.manage.gens.Node.ReqGetNodeInfo;
import org.brewchain.manage.gens.Node.RespGetNodeInfo;
import org.brewchain.manage.util.KeyStoreHelper;
import org.brewchain.manage.util.ManageKeys;
import org.fc.brewchain.bcapi.EncAPI;
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
	@ActorRequire(name = "Account_Helper", scope = "global")
	AccountHelper oAccountHelper;
	@ActorRequire(name = "KeyStore_Helper", scope = "global")
	KeyStoreHelper keyStoreHelper;
	
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
		oRespGetNodeInfo.setServerTime(String.valueOf((new Date()).getTime()));
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
			
			FileReader fr = new FileReader(".keystore");
			BufferedReader br = new BufferedReader(fr);
			String keyStoreStr = br.readLine().trim().replace("\r","").replace("\t","");
			br.close();
			fr.close();
			
			// encApi.
			
			OValue oOValue = dao.getManageDao().get(OEntityBuilder.byteKey2OKey(ManageKeys.NODE_ADDRESS.getBytes()))
					.get();

			if (oOValue == null || oOValue.getExtdata() == null || oOValue.getExtdata().equals(ByteString.EMPTY)) {

			} else {
				oRespGetNodeInfo.setAddress(encApi.hexEnc(oOValue.getExtdata().toByteArray()));
				oRespGetNodeInfo.setCwstotal(oAccountHelper.getTokenBalance(oOValue.getExtdata().toByteArray(), "CWS"));
			}
			
			
		} catch (Exception e) {
			log.error(e.getMessage());
		}
		
		// net
		try {
			OValue oOValue = dao.getManageDao().get(OEntityBuilder.byteKey2OKey(ManageKeys.NODE_NET.getBytes()))
					.get();

			if (oOValue == null || oOValue.getExtdata() == null || oOValue.getExtdata().equals(ByteString.EMPTY)) {

			} else {
				NodeNetwork oNodeNetwork = NodeNetwork.parseFrom(oOValue.getExtdata());
				oRespGetNodeInfo.setNetwork(oNodeNetwork);
			}
		} catch (Exception e) {
			log.error(e.getMessage());
		}

		handler.onFinished(PacketHelper.toPBReturn(pack, oRespGetNodeInfo.build()));
	}
}
