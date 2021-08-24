// Copyright 2016 Michel Kraemer
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package de.undercouch.gradle.tasks.download.internal;

import de.undercouch.gradle.tasks.download.org.apache.http.HttpHost;
import de.undercouch.gradle.tasks.download.org.apache.http.impl.client.CloseableHttpClient;

/**
 * Factory for Apache {@link CloseableHttpClient} objects
 * @author Michel Kraemer
 */
public interface HttpClientFactory {
    /**
     * Creates an HTTP client for the given host
     * @param httpHost the host to connect to
     * @param acceptAnyCertificate true if HTTPS certificate verification
     * errors should be ignored and any certificate (even an invalid one)
     * should be accepted
     * @param retries the number of retries to perform if an HTTP request fails
     * @return the HTTP client
     */
    CloseableHttpClient createHttpClient(HttpHost httpHost,
            boolean acceptAnyCertificate, int retries);
}
