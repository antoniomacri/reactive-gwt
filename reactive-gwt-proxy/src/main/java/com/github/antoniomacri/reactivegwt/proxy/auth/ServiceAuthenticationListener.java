/**
 * Copyright 2015 Blue Esoteric Web Development, LLC
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
package com.github.antoniomacri.reactivegwt.proxy.auth;

/**
 * Helper to be notified when a {@link ServiceAuthenticator} has been prepared.
 *
 * @author Preethum
 * @since 0.6
 *
 */
public interface ServiceAuthenticationListener {
	/**
	 * Indicates that this authenticator has the information necessary to
	 * proceed with RPC's. This authenticator should be provided to the RPC
	 * service to take effect
	 *
	 * @param authenticator
	 *            the account the user logged in with, if available. This may be
	 *            null in circumstances where a token was provided for access
	 *            without identified specifically which user is being
	 *            represented.
	 * @version 0.6.1
	 */
	void onAuthenticatorPrepared(ServiceAuthenticator authenticator);
}
