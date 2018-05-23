package org.brewchain.manage.impl;

import org.brewchain.account.core.BlockChainHelper;
import org.brewchain.account.gens.Block.BlockEntity;
import org.brewchain.manage.dao.DefDaos;
import org.brewchain.manage.gens.Block.PBLOCommand;
import org.brewchain.manage.gens.Block.ReqGetGenesisBlock;
import org.brewchain.manage.gens.Block.RespGetGenesisBlock;
import org.brewchain.manage.gens.Manageimpl.PMANCommand;
import org.brewchain.manage.gens.Manageimpl.PMANModule;
import org.brewchain.manage.gens.Manageimpl.ReqLogin;
import org.brewchain.manage.gens.Manageimpl.RespLogin;
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
public class GetGenesisBlockImpl extends SessionModules<ReqGetGenesisBlock> {
	@ActorRequire(name = "Def_Daos", scope = "global")
	DefDaos dao;
	@ActorRequire(name = "bc_encoder", scope = "global")
	EncAPI encApi;
	@ActorRequire(name = "BlockChain_Helper", scope = "global")
	BlockChainHelper blockChainHelper;

	@Override
	public String[] getCmds() {
		return new String[] { PBLOCommand.GGB.name() };
	}

	@Override
	public String getModule() {
		return PMANModule.MAN.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqGetGenesisBlock pb, final CompleteHandler handler) {
		RespGetGenesisBlock.Builder oRespGetGenesisBlock = RespGetGenesisBlock.newBuilder();
		try {
			BlockEntity oBlockEntity = blockChainHelper.getGenesisBlock();
			if (oBlockEntity == null) {
				oRespGetGenesisBlock.setRetCode("-1");
			} else {
				oRespGetGenesisBlock.setHash(encApi.hexEnc(oBlockEntity.getHeader().getBlockHash().toByteArray()));
				oRespGetGenesisBlock.setRetCode("1");
			}
		} catch (Exception e) {
			oRespGetGenesisBlock.setRetCode("-1");
			if (e.getMessage() != null) {
				oRespGetGenesisBlock.setRetMsg(e.getMessage());
			}
		}
		handler.onFinished(PacketHelper.toPBReturn(pack, oRespGetGenesisBlock.build()));
	}
}
