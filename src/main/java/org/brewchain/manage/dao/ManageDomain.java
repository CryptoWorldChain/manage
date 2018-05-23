package org.brewchain.manage.dao;

import org.brewchain.bcapi.backend.ODBDao;

import onight.tfw.ojpa.api.ServiceSpec;

public class ManageDomain extends ODBDao{

	public ManageDomain(ServiceSpec serviceSpec) {
		super(serviceSpec);
	}

	@Override
	public String getDomainName() {
		return "manage";
	}

	
}
