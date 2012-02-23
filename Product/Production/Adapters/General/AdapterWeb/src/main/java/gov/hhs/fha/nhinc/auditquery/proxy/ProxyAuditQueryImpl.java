/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gov.hhs.fha.nhinc.auditquery.proxy;

import com.services.nhinc.schema.auditmessage.FindAuditEventsResponseType;
import gov.hhs.fha.nhinc.common.nhinccommon.AssertionType;
import gov.hhs.fha.nhinc.common.nhinccommonproxy.FindAuditEventsRequestType;
import gov.hhs.fha.nhinc.connectmgr.ConnectionManagerCache;
import gov.hhs.fha.nhinc.nhinclib.NhincConstants;
import gov.hhs.fha.nhinc.nhincproxyauditlogquerysecured.NhincProxyAuditLogQuerySecured;
import gov.hhs.fha.nhinc.nhincproxyauditlogquerysecured.NhincProxyAuditLogQuerySecuredPortType;
import gov.hhs.fha.nhinc.saml.extraction.SamlTokenCreator;
import java.util.Map;
import java.util.StringTokenizer;
import javax.xml.ws.BindingProvider;

/**
 *
 * @author jhoppesc
 */
public class ProxyAuditQueryImpl {
    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(ProxyAuditQueryImpl.class);
    private static NhincProxyAuditLogQuerySecured service = new NhincProxyAuditLogQuerySecured();

    public FindAuditEventsResponseType findAuditEvents(FindAuditEventsRequestType findAuditEventsRequest) {
        FindAuditEventsResponseType response = new FindAuditEventsResponseType();

        try
        {
            String url = ConnectionManagerCache.getLocalEndpointURLByServiceName(NhincConstants.NHINC_PROXY_AUDIT_QUERY_SECURED_SERVICE_NAME);

            NhincProxyAuditLogQuerySecuredPortType port = getPort(url);

            AssertionType assertIn = findAuditEventsRequest.getAssertion();
            SamlTokenCreator tokenCreator = new SamlTokenCreator();
            Map requestContext = tokenCreator.CreateRequestContext(assertIn, url, NhincConstants.AUDIT_QUERY_ACTION);
            ((BindingProvider) port).getRequestContext().putAll(requestContext);
  
            gov.hhs.fha.nhinc.common.nhinccommonproxy.FindAuditEventsSecuredRequestType body = new gov.hhs.fha.nhinc.common.nhinccommonproxy.FindAuditEventsSecuredRequestType();
            body.setFindAuditEvents(findAuditEventsRequest.getFindAuditEvents());
            body.setNhinTargetSystem(findAuditEventsRequest.getNhinTargetSystem());
			
		int retryCount = gov.hhs.fha.nhinc.webserviceproxy.WebServiceProxyHelper.getInstance().getRetryAttempts();
		int retryDelay = gov.hhs.fha.nhinc.webserviceproxy.WebServiceProxyHelper.getInstance().getRetryDelay();
        String exceptionText = gov.hhs.fha.nhinc.webserviceproxy.WebServiceProxyHelper.getInstance().getExceptionText();
        javax.xml.ws.WebServiceException catchExp = null;
        if (retryCount > 0 && retryDelay > 0 && exceptionText != null && !exceptionText.equalsIgnoreCase("")) {
            int i = 1;
            while (i <= retryCount) {
                try {
                    response = port.findAuditEvents(body);
                    break;
                } catch (javax.xml.ws.WebServiceException e) {
                    catchExp = e;
                    int flag = 0;
                    StringTokenizer st = new StringTokenizer(exceptionText, ",");
                    while (st.hasMoreTokens()) {
                        if (e.getMessage().contains(st.nextToken())) {
                            flag = 1;
                        }
                    }
                    if (flag == 1) {
                        log.warn("Exception calling ... web service: " + e.getMessage());
                        System.out.println("retrying the connection for attempt [ " + i + " ] after [ " + retryDelay + " ] seconds");
                        log.info("retrying attempt [ " + i + " ] the connection after [ " + retryDelay + " ] seconds");
                        i++;
                        try {
                            Thread.sleep(retryDelay);
                        } catch (InterruptedException iEx) {
                            log.error("Thread Got Interrupted while waiting on NhincProxyAuditLogQuerySecured call :" + iEx);
                        } catch (IllegalArgumentException iaEx) {
                            log.error("Thread Got Interrupted while waiting on NhincProxyAuditLogQuerySecured call :" + iaEx);
                        }
                        retryDelay = retryDelay + retryDelay; //This is a requirement from Customer
                    } else {
                        log.error("Unable to call NhincProxyAuditLogQuerySecured Webservice due to  : " + e);
                        throw e;
                    }
                }
            }

            if (i > retryCount) {
                log.error("Unable to call NhincProxyAuditLogQuerySecured Webservice due to  : " + catchExp);
                throw catchExp;
            }

        } else {
            response = port.findAuditEvents(body);
        }
		
        }
        catch (Exception ex)
        {
            log.error("Failed to send entity audit query from proxy EJB to secure interface: " + ex.getMessage(), ex);
        }

        return response;
    }

    private NhincProxyAuditLogQuerySecuredPortType getPort(String url) {
        NhincProxyAuditLogQuerySecuredPortType port = service.getNhincProxyAuditLogQuerySecuredPortSoap11();
        gov.hhs.fha.nhinc.webserviceproxy.WebServiceProxyHelper.getInstance().initializePort((javax.xml.ws.BindingProvider) port, url);
        return port;
    }

}