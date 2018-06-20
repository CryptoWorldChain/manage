package org.brewchain.manage.impl;

import java.util.LinkedList;

import org.brewchain.manage.util.OEntityBuilder;
import org.brewchain.bcapi.gens.Oentity.OKey;
import org.brewchain.bcapi.gens.Oentity.OValue;
import org.brewchain.manage.dao.ManageDaos;
import org.brewchain.manage.gens.Manageimpl.PMANCommand;
import org.brewchain.manage.gens.Manageimpl.PMANModule;
import org.brewchain.manage.gens.Manageimpl.ReqBatchSetConfig;
import org.brewchain.manage.gens.Manageimpl.RespBatchSetConfig;
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
public class BatchSetConfigImpl extends SessionModules<ReqBatchSetConfig> {
	@ActorRequire(name = "man_Daos", scope = "global")
	ManageDaos dao;
	@ActorRequire(name = "bc_encoder", scope = "global")
	EncAPI encApi;

	@Override
	public String[] getCmds() {
		return new String[] { PMANCommand.BSC.name() };
	}

	@Override
	public String getModule() {
		return PMANModule.MAN.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqBatchSetConfig pb, final CompleteHandler handler) {
		RespBatchSetConfig.Builder oRespBatchSetConfig = RespBatchSetConfig.newBuilder();

		try {
			LinkedList<OKey> keys = new LinkedList<OKey>();
			LinkedList<OValue> values = new LinkedList<OValue>();
			for (int i = 0; i < pb.getKeysCount(); i++) {
				keys.add(OEntityBuilder.byteKey2OKey(pb.getKeys(i).getBytes()));
				values.add(OEntityBuilder.byteValue2OValue(ByteString.copyFromUtf8(pb.getValues(i)).toByteArray()));
			}
			dao.getManageDao().batchPuts(keys.toArray(new OKey[0]), values.toArray(new OValue[0]));
			oRespBatchSetConfig.setRetCode("1");
		} catch (Exception e) {
			oRespBatchSetConfig.setRetCode("-1");
			if (e.getMessage() != null)
				oRespBatchSetConfig.setRetMsg(e.getMessage());
		}

		handler.onFinished(PacketHelper.toPBReturn(pack, oRespBatchSetConfig.build()));
	}
}
