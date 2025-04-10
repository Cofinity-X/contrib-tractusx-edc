/**
 * Copyright (c) 2022 Amadeus - initial API and implementation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 **/

package org.eclipse.tractusx.edc.dataplane.proxy.api.controller;

import jakarta.ws.rs.container.ContainerRequestContext;

import java.util.Map;

/**
 * Wrapper around {@link ContainerRequestContext} enabling mocking.
 */
public interface ContainerRequestContextApi {

    /**
     * Get the request headers. Note that if more than one value is associated to a specific header,
     * only the first one is retained.
     *
     * @return Headers map.
     */
    Map<String, String> headers();

    /**
     * Format query of the request as string, e.g. "hello=world\&amp;foo=bar".
     *
     * @return Query param string.
     */
    String queryParams();

    /**
     * Format the request body into a string.
     *
     * @return Request body.
     */
    String body();

    /**
     * Get the media type from incoming request.
     *
     * @return Media type.
     */
    String mediaType();

    /**
     * Return request path, e.g. "hello/world/foo/bar".
     *
     * @return Path string.
     */
    String path();

    /**
     * Get http method from the incoming request, e.g. "GET", "POST"...
     *
     * @return Http method.
     */
    String method();
}
