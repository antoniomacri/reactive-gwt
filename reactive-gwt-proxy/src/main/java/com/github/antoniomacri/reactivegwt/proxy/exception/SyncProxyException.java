/**
 * Copyright 2014 Blue Esoteric Web Development, LLC
 * <http://www.blueesoteric.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at <http://www.apache.org/licenses/LICENSE-2.0>
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.github.antoniomacri.reactivegwt.proxy.exception;

import java.util.Arrays;

import com.github.antoniomacri.reactivegwt.proxy.RemoteServiceProxy;
import com.google.gwt.user.client.rpc.InvocationException;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.rpc.impl.RequestCallbackAdapter;

/**
 * @author Preethum
 * @since 0.5
 *
 */
public class SyncProxyException extends InvocationException {
	public enum InfoType {
		/**
		 * Used to indicate that the relative path for the service request was
		 * not provided manually and not able to be determined from the
		 * Synchronous interface's annotation value of
		 * {@link RemoteServiceRelativePath}
		 */
		REMOTE_SERVICE_RELATIVE_PATH("Check RemoteServiceRelativePath annotation on service"), MODULE_BASE_URL(
				"Set SyncProxy Base Url"), POLICY_NAME_POPULATION(
				"Unable to populate policy names, see below exception."),
		POLICY_FINDER_MISSING("Missing policy finder."),
		/**
		 * Used to indicate that an attempt to find the synchronous service
		 * interface based on a provided Asynchronous service interface failed.
		 */
		SERVICE_BASE("Make sure your service classes are on the classpath"),
		/**
		 * Used to indicate the use of the
		 * {@link ServiceDefTarget#setServiceEntryPoint(String)} where the
		 * moduleBaseUrl is different. This occurs because with a different base
		 * url, we have no good way of separating the provided EntryPoint into
		 * moduleBase and remoteServiceRelativePath. The moduleBase is needed
		 * separately in
		 * {@link RemoteServiceProxy#doInvokeAsync(RequestCallbackAdapter.ResponseReader, String)}
		 */
		SERVICE_BASE_DELTA("Unable to determine new module base url from provided service entry point.");
		String help;

		InfoType(String help) {
			this.help = help;
		}
	}

	private static final long serialVersionUID = 1L;
	InfoType type;

	public SyncProxyException(Class<?> service, InfoType type) {
		super("Missing " + type + " for service " + service.getName() + ". " + type.help);
		this.type = type;
	}

	public SyncProxyException(Class<?> service, InfoType type, Exception e) {
		super("Missing " + type + " for service " + service.getName() + ". " + type.help, e);
		this.type = type;
	}

	public SyncProxyException(InfoType type, Exception e) {
		super("Error " + type + ". " + type.help, e);
		this.type = type;
	}

	public InfoType getType() {
		return this.type;
	}

	/**
	 * Quickly verify's if this exceptions error type is in the provided set. If
	 * not, this exception is re-thrown.
	 */
	public void verify(InfoType... types) {
		if (!Arrays.asList(types).contains(this.type)) {
			throw this;
		}
	}
}
