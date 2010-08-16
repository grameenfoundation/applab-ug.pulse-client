/**
 * Copyright (C) 2010 Grameen Foundation
Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations under
the License.
 */

package applab.pulse.client;

import java.io.IOException;
import java.net.URI;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

public class HtmlLoader {

	public HtmlLoader() {
	}

	/**
	 * Downloads an HTML string from the specified location.
	 * 
	 * @param uri The URI to fetch content from
	 * @return An HTML string; if null is returned, the caller should assume that the
	 * destination was unreachable and trigger the appropriate error behavior.
	 */
	public String fetchContent(URI uri) {
		try {
			ResponseHandler<String> responseHandler = new BasicResponseHandler();			
			HttpParams httpParameters = new BasicHttpParams();
			HttpConnectionParams.setSoTimeout(httpParameters, Global.TIMEOUT);
			HttpConnectionParams.setConnectionTimeout(httpParameters,
					Global.TIMEOUT);
			HttpClient client = new DefaultHttpClient(httpParameters);
			HttpGet getMethod = new HttpGet(uri);
			return client.execute(getMethod, responseHandler);
		} catch (IOException e) {
			return null;
		}
	}
}