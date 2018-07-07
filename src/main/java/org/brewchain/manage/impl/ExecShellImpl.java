package org.brewchain.manage.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.brewchain.manage.gens.Manageimpl.PMANCommand;
import org.brewchain.manage.gens.Manageimpl.PMANModule;
import org.brewchain.manage.gens.Manageimpl.ReqExecShell;
import org.brewchain.manage.gens.Manageimpl.RespExecShell;
import org.brewchain.manage.util.ShellRunner;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.oapi.scala.commons.SessionModules;
import onight.osgi.annotation.NActorProvider;
import onight.tfw.async.CompleteHandler;
import onight.tfw.otransio.api.PacketHelper;
import onight.tfw.otransio.api.beans.FramePacket;

@NActorProvider
@Slf4j
@Data
public class ExecShellImpl extends SessionModules<ReqExecShell> {
	@Override
	public String[] getCmds() {
		return new String[] { PMANCommand.ESS.name() };
	}

	@Override
	public String getModule() {
		return PMANModule.MAN.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqExecShell pb, final CompleteHandler handler) {
		RespExecShell.Builder oRespExecShell = RespExecShell.newBuilder();
		try {

			Process p = Runtime.getRuntime().exec("sh/restart.sh");
			p.waitFor();

			BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			StringBuilder sb = new StringBuilder();
			String line = "";
			while ((line = reader.readLine()) != null) {
				sb.append(line + "\n");
			}
			oRespExecShell.setRetMsg(sb.toString());
		} catch (Exception e) {
			e.printStackTrace();
			oRespExecShell.setRetCode("-1");
			oRespExecShell.setRetMsg("未知异常:" + e);
		}
		handler.onFinished(PacketHelper.toPBReturn(pack, oRespExecShell.build()));
		return;
	}
}
