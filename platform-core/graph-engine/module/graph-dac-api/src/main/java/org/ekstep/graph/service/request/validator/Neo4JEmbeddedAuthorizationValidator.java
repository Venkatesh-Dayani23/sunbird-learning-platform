package org.ekstep.graph.service.request.validator;

import com.ilimi.common.logger.PlatformLogger;


public class Neo4JEmbeddedAuthorizationValidator {
	
	
	
	public boolean isAuthorizedToken() {
		PlatformLogger.log("Checking with Authorization.");
		return false;
	}

}
