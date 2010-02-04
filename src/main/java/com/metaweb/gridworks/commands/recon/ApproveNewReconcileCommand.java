package com.metaweb.gridworks.commands.recon;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;

import com.metaweb.gridworks.commands.EngineDependentCommand;
import com.metaweb.gridworks.model.AbstractOperation;
import com.metaweb.gridworks.model.operations.ApproveNewReconOperation;

public class ApproveNewReconcileCommand extends EngineDependentCommand {
	
	@Override
	protected AbstractOperation createOperation(HttpServletRequest request,
			JSONObject engineConfig) throws Exception {
		
		int cellIndex = Integer.parseInt(request.getParameter("cell"));
		
		return new ApproveNewReconOperation(engineConfig, cellIndex);
	}
}
