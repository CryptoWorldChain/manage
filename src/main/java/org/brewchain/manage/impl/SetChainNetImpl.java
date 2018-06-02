package org.brewchain.manage.impl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.brewchain.manage.util.OEntityBuilder;
import org.brewchain.manage.dao.ManageDaos;
import org.brewchain.manage.gens.Manageimpl.PMANCommand;
import org.brewchain.manage.gens.Manageimpl.PMANModule;
import org.brewchain.manage.gens.Manageimpl.ReqSetNetwork;
import org.brewchain.manage.gens.Manageimpl.RespSetNetwork;
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
public class SetChainNetImpl extends SessionModules<ReqSetNetwork> {
	@ActorRequire(name = "man_Daos", scope = "global")
	ManageDaos dao;
	@ActorRequire(name = "bc_encoder", scope = "global")
	EncAPI encApi;

	@Override
	public String[] getCmds() {
		return new String[] { PMANCommand.SNW.name() };
	}

	@Override
	public String getModule() {
		return PMANModule.MAN.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqSetNetwork pb, final CompleteHandler handler) {
		RespSetNetwork.Builder oRespSelectNetwork = RespSetNetwork.newBuilder();

		// 写入.network
		BufferedWriter bw = null;
		FileWriter fw = null;
		try {
			File networkFile = new File(".chainnet");
			if (networkFile.exists()) {
				networkFile.delete();
			}
			if (!networkFile.createNewFile()) {
				oRespSelectNetwork.setRetCode("-1");
				oRespSelectNetwork.setRetMsg("设置net失败");
			} else {
				fw = new FileWriter(networkFile);
				bw = new BufferedWriter(fw);
				bw.write(pb.getNetwork() + "\t\n");
				bw.close();
				fw.close();

				oRespSelectNetwork.setRetCode("1");
				dao.getManageDao().put(OEntityBuilder.byteKey2OKey(ManageKeys.NODE_NET.getBytes()),
						OEntityBuilder.byteValue2OValue(pb.getNetworkBytes()));
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

			oRespSelectNetwork.clear();
			oRespSelectNetwork.setRetCode("-1");
			if (e.getMessage() != null)
				oRespSelectNetwork.setRetMsg(e.getMessage());
		}

		handler.onFinished(PacketHelper.toPBReturn(pack, oRespSelectNetwork.build()));
	}
}
