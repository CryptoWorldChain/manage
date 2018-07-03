package org.brewchain.manage.impl;

import java.util.ArrayList;
import java.util.List;

import org.brewchain.account.core.AccountHelper;
import org.brewchain.bcapi.gens.Oentity.OValue;
import org.brewchain.evmapi.gens.Act.Account;
import org.brewchain.manage.dao.ManageDaos;
import org.brewchain.manage.gens.Manageimpl.MsgContract;
import org.brewchain.manage.gens.Manageimpl.PMANCommand;
import org.brewchain.manage.gens.Manageimpl.PMANModule;
import org.brewchain.manage.gens.Manageimpl.ReqGetContractList;
import org.brewchain.manage.gens.Manageimpl.ReqGetNetwork;
import org.brewchain.manage.gens.Manageimpl.RespGetContractList;
import org.brewchain.manage.util.ManageKeys;
import org.brewchain.manage.util.OEntityBuilder;
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
public class GetContractListImpl extends SessionModules<ReqGetContractList> {
	@ActorRequire(name = "man_Daos", scope = "global")
	ManageDaos dao;
	@ActorRequire(name = "bc_encoder", scope = "global")
	EncAPI encApi;
	@ActorRequire(name = "Account_Helper", scope = "global")
	AccountHelper oAccountHelper;

	@Override
	public String[] getCmds() {
		return new String[] { PMANCommand.GLC.name() };
	}

	@Override
	public String getModule() {
		return PMANModule.MAN.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqGetContractList pb, final CompleteHandler handler) {
		RespGetContractList.Builder oRespGetContractList = RespGetContractList.newBuilder();
		try {
			OValue oOValue = dao.getAccountDao().get(OEntityBuilder.byteKey2OKey(ManageKeys.NODE_ACCOUNT.getBytes()))
					.get();
			if (oOValue == null || oOValue.getExtdata() == null || oOValue.getExtdata().equals(ByteString.EMPTY)) {
				oRespGetContractList.setRetCode("-1");
				oRespGetContractList.setRetMsg("not found node account");
			} else {
				List<Account> list = oAccountHelper.getContractByCreator(oOValue.getExtdata());
				List<MsgContract> contracts = new ArrayList<>();
				for (Account contract : list) {
					MsgContract.Builder oMsgContract = MsgContract.newBuilder();

					oMsgContract.setCode(encApi.hexEnc(contract.getValue().getCode().toByteArray()));
					oMsgContract.setCodeHash(encApi.hexEnc(contract.getValue().getCodeHash().toByteArray()));
					oMsgContract.setData(encApi.hexEnc(contract.getValue().getData().toByteArray()));
					oMsgContract.setHash(encApi.hexEnc(contract.getAddress().toByteArray()));
					oMsgContract.setTimestamp(contract.getValue().getTimestamp());

					oRespGetContractList.addContracts(oMsgContract.build());
				}

				oRespGetContractList.setRetCode("1");
			}

		} catch (Exception e) {
			if (e.getMessage() != null) {
				oRespGetContractList.setRetMsg(e.getMessage());
			}
			oRespGetContractList.setRetCode("-1");
		}

		handler.onFinished(PacketHelper.toPBReturn(pack, oRespGetContractList.build()));
		return;
	}
}
