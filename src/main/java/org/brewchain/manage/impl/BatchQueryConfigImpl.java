package org.brewchain.manage.impl;

import java.util.LinkedList;

import org.brewchain.manage.util.OEntityBuilder;
import org.brewchain.bcapi.gens.Oentity.OValue;
import org.brewchain.manage.dao.ManageDaos;
import org.brewchain.manage.gens.Manageimpl.PMANCommand;
import org.brewchain.manage.gens.Manageimpl.PMANModule;
import org.brewchain.manage.gens.Manageimpl.ReqBatchQueryConfig;
import org.brewchain.manage.gens.Manageimpl.RespBatchQueryConfig;
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
public class BatchQueryConfigImpl extends SessionModules<ReqBatchQueryConfig> {
	@ActorRequire(name = "man_Daos", scope = "global")
	ManageDaos dao;
	@ActorRequire(name = "bc_encoder", scope = "global")
	EncAPI encApi;

	@Override
	public String[] getCmds() {
		return new String[] { PMANCommand.BQC.name() };
	}

	@Override
	public String getModule() {
		return PMANModule.MAN.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqBatchQueryConfig pb, final CompleteHandler handler) {
		RespBatchQueryConfig.Builder oRespBatchSetConfig = RespBatchQueryConfig.newBuilder();
		LinkedList<String> retValues = new LinkedList<String>();
		try {
			for (String key : pb.getKeysList()) {
				OValue oOValue = dao.getManageDao().get(OEntityBuilder.byteKey2OKey(key.getBytes())).get();
				if (oOValue == null || oOValue.getExtdata() == null || oOValue.getExtdata().equals(ByteString.EMPTY)) {
					retValues.add("");
				} else {
					oRespBatchSetConfig.addValues(oOValue.getExtdata().toStringUtf8());
				}
			}
			oRespBatchSetConfig.setRetCode("1");
			handler.onFinished(PacketHelper.toPBReturn(pack, oRespBatchSetConfig.build()));
		} catch (Exception e) {
			oRespBatchSetConfig.clear();
			oRespBatchSetConfig.setRetCode("-1");
			if (e.getMessage() != null)
				oRespBatchSetConfig.setRetMsg(e.getMessage());

			handler.onFinished(PacketHelper.toPBReturn(pack, oRespBatchSetConfig.build()));
		}
	}
}
