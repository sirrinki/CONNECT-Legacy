package gov.hhs.fha.nhinc.xdr.async.request.proxy;

import gov.hhs.fha.nhinc.common.nhinccommon.AssertionType;
import gov.hhs.fha.nhinc.common.nhinccommon.NhinTargetSystemType;
import gov.hhs.fha.nhinc.connectmgr.ConnectionManagerCache;
import gov.hhs.fha.nhinc.connectmgr.ConnectionManagerException;
import gov.hhs.fha.nhinc.nhinclib.NhincConstants;
import gov.hhs.fha.nhinc.nhinclib.NullChecker;
import gov.hhs.fha.nhinc.saml.extraction.SamlTokenCreator;
import ihe.iti.xdr._2007.AcknowledgementType;
import ihe.iti.xds_b._2007.ProvideAndRegisterDocumentSetRequestType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ihe.iti.xdr.async.request._2007.XDRRequestService;
import ihe.iti.xdr.async.request._2007.XDRRequestPortType;
import java.util.Map;
import java.util.StringTokenizer;
import javax.xml.ws.BindingProvider;

/**
 *
 * @author Neil Webb
 */
public class NhinXDRRequestWebServiceProxy implements NhinXDRRequestProxy
{
    private static Log log;
    private static XDRRequestService service;

    public NhinXDRRequestWebServiceProxy()
    {
        log = createLogger();
        service = createService();
    }

    public AcknowledgementType provideAndRegisterDocumentSetBRequest(ProvideAndRegisterDocumentSetRequestType request, AssertionType assertion, NhinTargetSystemType targetSystem)
    {
        AcknowledgementType response = null;
        String url = getUrl(targetSystem);

        if (NullChecker.isNotNullish(url))
        {
            try
            {
                XDRRequestPortType port = getPort(url);

                setRequestContext(assertion, url, port);
              
		int retryCount = gov.hhs.fha.nhinc.webserviceproxy.WebServiceProxyHelper.getInstance().getRetryAttempts();
		int retryDelay = gov.hhs.fha.nhinc.webserviceproxy.WebServiceProxyHelper.getInstance().getRetryDelay();
        String exceptionText = gov.hhs.fha.nhinc.webserviceproxy.WebServiceProxyHelper.getInstance().getExceptionText();
        javax.xml.ws.WebServiceException catchExp = null;
        if (retryCount > 0 && retryDelay > 0 && exceptionText != null && !exceptionText.equalsIgnoreCase("")) {
            int i = 1;
            while (i <= retryCount) {
                try {
                    response = port.provideAndRegisterDocumentSetBRequest(request);
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
                            log.error("Thread Got Interrupted while waiting on XDRRequestService call :" + iEx);
                        } catch (IllegalArgumentException iaEx) {
                            log.error("Thread Got Interrupted while waiting on XDRRequestService call :" + iaEx);
                        }
                        retryDelay = retryDelay + retryDelay; //This is a requirement from Customer
                    } else {
                        log.error("Unable to call XDRRequestService Webservice due to  : " + e);
                        throw e;
                    }
                }
            }

            if (i > retryCount) {
                log.error("Unable to call XDRRequestService Webservice due to  : " + catchExp);
                throw catchExp;
            }

        } else {
            response = port.provideAndRegisterDocumentSetBRequest(request);
        }
						
            }
            catch(Throwable t)
            {
                log.error("Error in NHIN client for XDR Request: " + t.getMessage(), t);
                response = new AcknowledgementType();
                response.setMessage("Error");
            }
        }
        else
        {
            log.error("The URL for service: " + NhincConstants.NHINC_XDR_REQUEST_SERVICE_NAME + " is null");
            response = new AcknowledgementType();
            response.setMessage("Error");
        }

        return response;
    }

    protected void setRequestContext(AssertionType assertion, String url, XDRRequestPortType port)
    {
        SamlTokenCreator tokenCreator = new SamlTokenCreator();
        Map requestContext = tokenCreator.CreateRequestContext(assertion, url, NhincConstants.XDR_REQUEST_ACTION);

        ((BindingProvider) port).getRequestContext().putAll(requestContext);
    }

    protected XDRRequestService createService()
    {
        return ((service != null) ? service : new XDRRequestService());
    }

    protected Log createLogger()
    {
        return ((log != null) ? log : LogFactory.getLog(getClass()));
    }

    protected String getUrl(NhinTargetSystemType target)
    {
        String url = null;

        if (target != null)
        {
            try
            {
                url = ConnectionManagerCache.getEndpontURLFromNhinTarget(target, NhincConstants.NHINC_XDR_REQUEST_SERVICE_NAME);
            }
            catch (ConnectionManagerException ex)
            {
                log.error("Error: Failed to retrieve url for service: " + NhincConstants.NHINC_XDR_REQUEST_SERVICE_NAME);
                log.error(ex.getMessage());
            }
        }
        else
        {
            log.error("Target system passed into the proxy is null");
        }

        return url;
    }

    protected XDRRequestPortType getPort(String url)
    {
        XDRRequestPortType port = service.getXDRRequestPortSoap12();
        gov.hhs.fha.nhinc.webserviceproxy.WebServiceProxyHelper.getInstance().initializePort((javax.xml.ws.BindingProvider) port, url);
        return port;
    }
}