/*
 * Zed Attack Proxy (ZAP) and its related class files.
 *
 * ZAP is an HTTP/HTTPS proxy for assessing web application security.
 *
 * Copyright 2011 The ZAP Development Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.zaproxy.zap.extension.pscanrules;

import java.util.List;
import java.util.Set;
import net.htmlparser.jericho.Source;
import org.apache.commons.collections.iterators.IteratorChain;
import org.parosproxy.paros.Constant;
import org.parosproxy.paros.core.scanner.Alert;
import org.parosproxy.paros.model.Model;
import org.parosproxy.paros.network.HttpHeader;
import org.parosproxy.paros.network.HttpMessage;
import org.zaproxy.zap.extension.pscan.PassiveScanThread;
import org.zaproxy.zap.extension.pscan.PluginPassiveScanner;
import org.zaproxy.zap.sharedutils.CookieUtils;

public class CookieHttpOnlyScanner extends PluginPassiveScanner {

    /** Prefix for internationalised messages used by this rule */
    private static final String MESSAGE_PREFIX = "pscanrules.cookiehttponlyscanner.";

    private static final int PLUGIN_ID = 10010;

    private static final String HTTP_ONLY_COOKIE_ATTRIBUTE = "HttpOnly";

    private PassiveScanThread parent = null;
    private Model model = null;

    @Override
    public void setParent(PassiveScanThread parent) {
        this.parent = parent;
    }

    @Override
    public void scanHttpRequestSend(HttpMessage msg, int id) {
        // Ignore
    }

    @Override
    public void scanHttpResponseReceive(HttpMessage msg, int id, Source source) {
        IteratorChain iterator = new IteratorChain();
        List<String> cookies1 = msg.getResponseHeader().getHeaderValues(HttpHeader.SET_COOKIE);

        if (!cookies1.isEmpty()) {
            iterator.addIterator(cookies1.iterator());
        }

        List<String> cookies2 = msg.getResponseHeader().getHeaderValues(HttpHeader.SET_COOKIE2);

        if (!cookies2.isEmpty()) {
            iterator.addIterator(cookies2.iterator());
        }

        Set<String> ignoreList = CookieUtils.getCookieIgnoreList(getModel());

        while (iterator.hasNext()) {
            String headerValue = (String) iterator.next();
            if (!CookieUtils.hasAttribute(headerValue, HTTP_ONLY_COOKIE_ATTRIBUTE)) {
                if (CookieUtils.isExpired(headerValue)) {
                    continue;
                }
                if (!ignoreList.contains(CookieUtils.getCookieName(headerValue))) {
                    this.raiseAlert(msg, id, headerValue);
                }
            }
        }
    }

    private void raiseAlert(HttpMessage msg, int id, String headerValue) {
        Alert alert = new Alert(getPluginId(), Alert.RISK_LOW, Alert.CONFIDENCE_MEDIUM, getName());
        alert.setDetail(
                getDescription(),
                msg.getRequestHeader().getURI().toString(),
                CookieUtils.getCookieName(headerValue),
                "",
                "",
                getSolution(),
                getReference(),
                CookieUtils.getSetCookiePlusName(msg.getResponseHeader().toString(), headerValue),
                16, // CWE Id 16 - Configuration
                13, // WASC Id - Info leakage
                msg);

        parent.raiseAlert(id, alert);
    }

    @Override
    public int getPluginId() {
        return PLUGIN_ID;
    }

    @Override
    public String getName() {
        return Constant.messages.getString(MESSAGE_PREFIX + "name");
    }

    private String getDescription() {
        return Constant.messages.getString(MESSAGE_PREFIX + "desc");
    }

    private String getSolution() {
        return Constant.messages.getString(MESSAGE_PREFIX + "soln");
    }

    private String getReference() {
        return Constant.messages.getString(MESSAGE_PREFIX + "refs");
    }

    private Model getModel() {
        if (this.model == null) {
            this.model = Model.getSingleton();
        }
        return this.model;
    }

    /*
     * Just for use in the unit tests
     */
    protected void setModel(Model model) {
        this.model = model;
    }
}
