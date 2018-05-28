package org.brewchain.manage.impl;

import org.brewchain.account.core.AccountHelper;
import org.brewchain.account.gens.Act.Account;
import org.brewchain.account.gens.Act.AccountCryptoToken;
import org.brewchain.account.gens.Act.AccountCryptoValue;
import org.brewchain.account.gens.Act.AccountTokenValue;
import org.brewchain.account.gens.Act.AccountValue;
import org.brewchain.manage.dao.ManageDaos;
import org.brewchain.manage.gens.Manageimpl.AccountCryptoTokenImpl;
import org.brewchain.manage.gens.Manageimpl.AccountCryptoValueImpl;
import org.brewchain.manage.gens.Manageimpl.AccountTokenValueImpl;
import org.brewchain.manage.gens.Manageimpl.AccountValueImpl;
import org.brewchain.manage.gens.Manageimpl.PMANCommand;
import org.brewchain.manage.gens.Manageimpl.PMANModule;
import org.brewchain.manage.gens.Manageimpl.ReqGetNodeAccount;
import org.brewchain.manage.gens.Manageimpl.ReqSetNodeAccount;
import org.brewchain.manage.gens.Manageimpl.RespGetNodeAccount;
import org.brewchain.manage.gens.Manageimpl.RespSetNodeAccount;
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
public class GetNodeAccountImpl extends SessionModules<ReqGetNodeAccount> {
	@ActorRequire(name = "man_Daos", scope = "global")
	ManageDaos dao;
	@ActorRequire(name = "bc_encoder", scope = "global")
	EncAPI encApi;
	@ActorRequire(name = "Account_Helper", scope = "global")
	AccountHelper accountHelper;

	@Override
	public String[] getCmds() {
		return new String[] { PMANCommand.GNA.name() };
	}

	@Override
	public String getModule() {
		return PMANModule.MAN.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqGetNodeAccount pb, final CompleteHandler handler) {
		RespGetNodeAccount.Builder oRespGetNodeAccount = RespGetNodeAccount.newBuilder();

		try {
			Account oAccount = accountHelper.GetAccount(encApi.hexDec(pb.getAddress()));
			oRespGetNodeAccount.setRetCode("1");
			AccountValue oAccountValue = oAccount.getValue();
			AccountValueImpl.Builder oAccountValueImpl = AccountValueImpl.newBuilder();
			oAccountValueImpl.setAcceptLimit(oAccountValue.getAcceptLimit());
			oAccountValueImpl.setAcceptMax(oAccountValue.getAcceptMax());
			for (ByteString relAddress : oAccountValue.getAddressList()) {
				oAccountValueImpl.addAddress(encApi.hexEnc(relAddress.toByteArray()));
			}

			oAccountValueImpl.setBalance(oAccountValue.getBalance());
			// oAccountValueImpl.setCryptos(index, value)
			for (AccountCryptoValue oAccountTokenValue : oAccountValue.getCryptosList()) {
				AccountCryptoValueImpl.Builder oAccountCryptoValueImpl = AccountCryptoValueImpl.newBuilder();
				oAccountCryptoValueImpl.setSymbol(oAccountTokenValue.getSymbol());

				for (AccountCryptoToken oAccountCryptoToken : oAccountTokenValue.getTokensList()) {
					AccountCryptoTokenImpl.Builder oAccountCryptoTokenImpl = AccountCryptoTokenImpl.newBuilder();
					oAccountCryptoTokenImpl.setCode(oAccountCryptoToken.getCode());
					oAccountCryptoTokenImpl.setHash(encApi.hexEnc(oAccountCryptoToken.getHash().toByteArray()));
					oAccountCryptoTokenImpl.setIndex(oAccountCryptoToken.getIndex());
					oAccountCryptoTokenImpl.setName(oAccountCryptoToken.getName());
					oAccountCryptoTokenImpl.setNonce(oAccountCryptoToken.getNonce());
					oAccountCryptoTokenImpl.setOwner(encApi.hexEnc(oAccountCryptoToken.getOwner().toByteArray()));
					oAccountCryptoTokenImpl.setOwnertime(oAccountCryptoToken.getOwnertime());
					oAccountCryptoTokenImpl.setTimestamp(oAccountCryptoToken.getTimestamp());
					oAccountCryptoTokenImpl.setTotal(oAccountCryptoToken.getTotal());

					oAccountCryptoValueImpl.addTokens(oAccountCryptoTokenImpl);
				}
				oAccountValueImpl.addCryptos(oAccountCryptoValueImpl);
			}
			oAccountValueImpl.setMax(oAccountValue.getMax());
			oAccountValueImpl.setNonce(oAccountValue.getNonce());
			oAccountValueImpl.setPubKey(encApi.hexEnc(oAccountValue.getPubKey().toByteArray()));
			for (AccountTokenValue oAccountTokenValue : oAccountValue.getTokensList()) {
				AccountTokenValueImpl.Builder oAccountTokenValueImpl = AccountTokenValueImpl.newBuilder();
				oAccountTokenValueImpl.setBalance(oAccountTokenValue.getBalance());
				oAccountTokenValueImpl.setToken(oAccountTokenValue.getToken());
				oAccountValueImpl.addTokens(oAccountTokenValueImpl);
			}
			oRespGetNodeAccount.setAccountValue(oAccountValueImpl);

		} catch (Exception e) {
			oRespGetNodeAccount.clear();
			oRespGetNodeAccount.setRetCode("-1");
			if (e.getMessage() != null) {
				oRespGetNodeAccount.setRetMsg(e.getMessage());
			}
		}
		handler.onFinished(PacketHelper.toPBReturn(pack, oRespGetNodeAccount.build()));
	}
}
