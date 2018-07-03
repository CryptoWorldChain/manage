package org.brewchain.manage.impl;

import org.brewchain.account.core.AccountHelper;
import org.brewchain.account.core.TransactionHelper;
import org.brewchain.manage.dao.ManageDaos;
import org.brewchain.manage.gens.Manageimpl.PMANCommand;
import org.brewchain.manage.gens.Manageimpl.PMANModule;
import org.brewchain.manage.gens.Manageimpl.ReqGetContractList;
import org.brewchain.manage.gens.Manageimpl.ReqGetNetwork;
import org.brewchain.manage.gens.Manageimpl.ReqGetNodeTransactionInfo;
import org.brewchain.manage.gens.Manageimpl.RespGetContractList;
import org.brewchain.manage.gens.Manageimpl.RespGetNodeTransactionInfo;
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
public class GetNodeTransactionInfoImpl extends SessionModules<ReqGetNodeTransactionInfo>{
	@ActorRequire(name = "man_Daos", scope = "global")
	ManageDaos dao;
	@ActorRequire(name = "bc_encoder", scope = "global")
	EncAPI encApi;
	@ActorRequire(name = "Account_Helper", scope = "global")
	AccountHelper oAccountHelper;
	@ActorRequire(name = "Transaction_Helper", scope = "global")
	TransactionHelper transactionHelper;
	
	@Override
	public String[] getCmds() {
		return new String[] { PMANCommand.GNT.name() };
	}

	@Override
	public String getModule() {
		return PMANModule.MAN.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqGetNodeTransactionInfo pb, final CompleteHandler handler) {
		RespGetNodeTransactionInfo.Builder oRespGetNodeTransactionInfo = RespGetNodeTransactionInfo.newBuilder();
		oRespGetNodeTransactionInfo.setWaitBlock(transactionHelper.getOPendingHashMapDB().getStorage().size());
		oRespGetNodeTransactionInfo.setWaitSend(transactionHelper.getOSendingHashMapDB().getStorage().size());
		
		handler.onFinished(PacketHelper.toPBReturn(pack, oRespGetNodeTransactionInfo.build()));
		return;
	}
}
