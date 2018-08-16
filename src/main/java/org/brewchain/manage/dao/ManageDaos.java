package org.brewchain.manage.dao;

import org.apache.felix.ipojo.annotations.Instantiate;
import org.brewchain.bcapi.backend.ODBSupport;
import org.brewchain.manage.gens.Manageimpl.PMANModule;
import org.fc.brewchain.p22p.core.PZPCtrl;

import com.google.protobuf.Message;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.oapi.scala.commons.SessionModules;
import onight.osgi.annotation.NActorProvider;
import onight.tfw.ntrans.api.annotation.ActorRequire;
import onight.tfw.ojpa.api.DomainDaoSupport;
import onight.tfw.ojpa.api.annotations.StoreDAO;

@NActorProvider
@Data
@Slf4j
@Instantiate(name = "man_Daos")
public class ManageDaos extends SessionModules<Message> {
	@StoreDAO(target = "bc_bdb", daoClass = ManageDomain.class)
	ODBSupport manageDao;
//	@StoreDAO(target = "bc_bdb", daoClass = AccoutDomain.class)
//	ODBSupport accountDao;
	@ActorRequire(scope = "global", name = "pzpctrl")
	PZPCtrl pzp;

	@Override
	public void onDaoServiceAllReady() {
		// log.debug("EncAPI==" + enc);
		// 校验
		log.debug("service ready!!!!");
	}

	@Override
	public void onDaoServiceReady(DomainDaoSupport arg0) {
	}
//	public void setAccountDao(DomainDaoSupport accountDao) {
//		this.accountDao = (ODBSupport) accountDao;
//	}
//
//	public ODBSupport getAccountDao() {
//		return accountDao;
//	}
	public void setManageDao(DomainDaoSupport manageDao) {
		this.manageDao = (ODBSupport) manageDao;
	}

	public ODBSupport getManageDao() {
		return manageDao;
	}

	@Override
	public String[] getCmds() {
		return new String[] { "DEFDAOS" };
	}

	@Override
	public String getModule() {
		return PMANModule.MAN.name();
	}

	public boolean isReady() {
		if (manageDao != null
				&& ManageDomain.class.isInstance(manageDao)
				&& manageDao.getDaosupport() != null) {
			;
			return true;
		}
		return false;
	}

}
