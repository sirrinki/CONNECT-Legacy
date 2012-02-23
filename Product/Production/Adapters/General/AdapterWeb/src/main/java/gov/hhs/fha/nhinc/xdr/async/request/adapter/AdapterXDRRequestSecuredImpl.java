/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gov.hhs.fha.nhinc.xdr.async.request.adapter;

import gov.hhs.fha.nhinc.adapterxdrrequestsecured.AdapterXDRRequestSecuredPortType;
import gov.hhs.fha.nhinc.adapterxdrrequest.AdapterXDRRequestPortType;
import gov.hhs.fha.nhinc.adapterxdrrequest.AdapterXDRRequestService;
import gov.hhs.fha.nhinc.common.nhinccommon.AssertionType;
import gov.hhs.fha.nhinc.common.nhinccommon.NhinTargetSystemType;
import gov.hhs.fha.nhinc.common.nhinccommonadapter.AdapterProvideAndRegisterDocumentSetRequestType;
import gov.hhs.fha.nhinc.common.nhinccommonentity.RespondingGatewayProvideAndRegisterDocumentSetSecuredResponseRequestType;
import gov.hhs.fha.nhinc.connectmgr.ConnectionManagerCache;
import gov.hhs.fha.nhinc.connectmgr.ConnectionManagerException;
import gov.hhs.fha.nhinc.nhincadapterxdr.AdapterXDRPortType;
import gov.hhs.fha.nhinc.nhincadapterxdr.AdapterXDRService;
import gov.hhs.fha.nhinc.nhincentityxdrsecured.async.response.EntityXDRSecuredResponsePortType;
import gov.hhs.fha.nhinc.nhincentityxdrsecured.async.response.EntityXDRSecuredResponseService;
import gov.hhs.fha.nhinc.nhinclib.NhincConstants;
import gov.hhs.fha.nhinc.nhinclib.NullChecker;
import gov.hhs.fha.nhinc.saml.extraction.SamlTokenCreator;
import gov.hhs.fha.nhinc.saml.extraction.SamlTokenExtractor;
import gov.hhs.fha.nhinc.xdr.adapter.AdapterComponentXDRImpl;
import ihe.iti.xdr._2007.AcknowledgementType;
import java.util.Map;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.WebServiceContext;
import oasis.names.tc.ebxml_regrep.xsd.rs._3.RegistryResponseType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 *
 * @author patlollav
 */
public class AdapterXDRRequestSecuredImpl {

    private static Log logger = null;
    private static EntityXDRSecuredResponseService entityXDRSecuredResponseService = null;
    public static String INVALID_ENDPOINT_MESSAGE = "ERROR: entityXDRSecuredResponseEndPointURL is null";
    private static AdapterXDRService adapterXDRService = null;
    private static AdapterXDRRequestService requestService = null;

    public AdapterXDRRequestSecuredImpl()
    {
        logger = createLogger();
    }
    public ihe.iti.xdr._2007.AcknowledgementType provideAndRegisterDocumentSetBRequest(ihe.iti.xds_b._2007.ProvideAndRegisterDocumentSetRequestType body, WebServiceContext context) {
        getLogger().debug("Entering provideAndRegisterDocumentSetBRequest");

        // Call AdapterComponent implementation to process the request.
        AssertionType assertion = createAssertion(context);

        AdapterProvideAndRegisterDocumentSetRequestType unsecured = new AdapterProvideAndRegisterDocumentSetRequestType();

        unsecured.setAssertion(assertion);
        unsecured.setProvideAndRegisterDocumentSetRequest(body);

        String url = getAdapterXDRRequestUrl();
        AdapterXDRRequestPortType port = getAdapterXDRRequestPort(url);
              
      
        getLogger().debug("Exiting provideAndRegisterDocumentSetBRequest");

        return port.provideAndRegisterDocumentSetBRequest(unsecured);
    }
    protected Log createLogger()
    {
        return ((logger != null) ? logger : LogFactory.getLog(getClass()));
    }
    protected AdapterComponentXDRImpl getAdapterComponentXDRImpl(){
        return new AdapterComponentXDRImpl();
    }

    protected Log getLogger(){
        return logger;
    }

    protected AssertionType createAssertion(WebServiceContext context){
        AssertionType assertion = SamlTokenExtractor.GetAssertion(context);
        return assertion;
    }


    protected RegistryResponseType callAdapterComponentXDR(ihe.iti.xds_b._2007.ProvideAndRegisterDocumentSetRequestType body, AssertionType assertion){

        getLogger().debug("Calling AdapterComponentXDRImpl");

        AdapterProvideAndRegisterDocumentSetRequestType adapterComponentXDRRequest = new AdapterProvideAndRegisterDocumentSetRequestType();

        adapterComponentXDRRequest.setAssertion(assertion);
        adapterComponentXDRRequest.setProvideAndRegisterDocumentSetRequest(body);

        RegistryResponseType registryResponse = null;

        String adapterComponentXDRUrl = getAdapterComponentXDRUrl();

        if (NullChecker.isNotNullish(adapterComponentXDRUrl)) {
            AdapterXDRPortType port = getAdapterXDRPort(adapterComponentXDRUrl);

            registryResponse = port.provideAndRegisterDocumentSetb(adapterComponentXDRRequest);

        } else {
            getLogger().error("The URL for service: " + NhincConstants.ADAPTER_XDR_SERVICE_NAME + " is null");
        }

        return registryResponse;

    }

    /**
     *
     * @param body
     * @param assertion
     * @return
     */
    public ihe.iti.xdr._2007.AcknowledgementType sendXDRResponse(oasis.names.tc.ebxml_regrep.xsd.rs._3.RegistryResponseType body, AssertionType assertion){

        getLogger().debug("Entering provideAndRegisterDocumentSetBResponse");

        String entityXDRSecuredResponseEndPointURL = null;

        ihe.iti.xdr._2007.AcknowledgementType response = null;

        entityXDRSecuredResponseEndPointURL = getEntityXDRSecuredResponseEndPointURL();

        if (NullChecker.isNotNullish(entityXDRSecuredResponseEndPointURL)) {
            EntityXDRSecuredResponsePortType port = getEntityXDRSecuredResponsePort(entityXDRSecuredResponseEndPointURL);

            setRequestContext(assertion, entityXDRSecuredResponseEndPointURL, port);

            RespondingGatewayProvideAndRegisterDocumentSetSecuredResponseRequestType request = createEntityXDRResponseSecuredRequest(body, assertion);

            response = port.provideAndRegisterDocumentSetBResponse(request);

        } else {
            getLogger().error("The URL for service: " + NhincConstants.ENTITY_XDR_RESPONSE_SECURED_SERVICE_NAME + " is null");
            response = new AcknowledgementType();
            response.setMessage(INVALID_ENDPOINT_MESSAGE);
        }

        getLogger().debug("Existing provideAndRegisterDocumentSetBResponse");

        return response;

    }

    protected void setRequestContext(AssertionType assertion, String entityXDRSecuredResponseEndPointURL, EntityXDRSecuredResponsePortType port)
    {
            SamlTokenCreator tokenCreator = new SamlTokenCreator();
            Map requestContext = tokenCreator.CreateRequestContext(assertion, entityXDRSecuredResponseEndPointURL, NhincConstants.ENTITY_XDR_SECURED_RESPONSE_ACTION);

        ((BindingProvider) port).getRequestContext().putAll(requestContext);
    }

    /**
     *
     * @return
     */
    protected String getEntityXDRSecuredResponseEndPointURL() {
        String url = null;

        try {
            url = ConnectionManagerCache.getLocalEndpointURLByServiceName(NhincConstants.ENTITY_XDR_RESPONSE_SECURED_SERVICE_NAME);

        } catch (ConnectionManagerException ex) {
            logger.error("Error: Failed to retrieve url for service: " + NhincConstants.ENTITY_XDR_RESPONSE_SECURED_SERVICE_NAME, ex);
        }

        return url;
    }

    /**
     *
     * @param url
     * @return
     */
    protected EntityXDRSecuredResponsePortType getEntityXDRSecuredResponsePort(String url) {
        EntityXDRSecuredResponsePortType port = getEntityXDRSecuredResponseService().getEntityXDRSecuredResponsePort();
        gov.hhs.fha.nhinc.webserviceproxy.WebServiceProxyHelper.getInstance().initializePort((javax.xml.ws.BindingProvider) port, url);
        return port;
    }


    /**
     *
     * @return
     */
    protected EntityXDRSecuredResponseService getEntityXDRSecuredResponseService(){
        if (entityXDRSecuredResponseService == null){
            return new EntityXDRSecuredResponseService();
        }
        else{
            return entityXDRSecuredResponseService;
        }

    }


    protected RespondingGatewayProvideAndRegisterDocumentSetSecuredResponseRequestType createEntityXDRResponseSecuredRequest(oasis.names.tc.ebxml_regrep.xsd.rs._3.RegistryResponseType body, AssertionType assertion){
        RespondingGatewayProvideAndRegisterDocumentSetSecuredResponseRequestType request = new RespondingGatewayProvideAndRegisterDocumentSetSecuredResponseRequestType();
        request.setRegistryResponse(body);
        NhinTargetSystemType nhinTargetSystem = createNhinTargetSystem(assertion);
        request.setNhinTargetSystem(nhinTargetSystem);

        return request;
    }

    /**
     * This will be removed once the broadcast PD is implented
     */
    private NhinTargetSystemType createNhinTargetSystem(AssertionType assertion){

        NhinTargetSystemType targetSystem = new NhinTargetSystemType();
        targetSystem.setHomeCommunity(assertion.getHomeCommunity());

        return targetSystem;
    }

    /**
     * 
     * @return
     */
    protected String getAdapterComponentXDRUrl() {
        String url = null;

        try {
            url = ConnectionManagerCache.getLocalEndpointURLByServiceName(NhincConstants.ADAPTER_XDR_SERVICE_NAME);
        } catch (ConnectionManagerException ex) {
            getLogger().error("Error: Failed to retrieve url for service: " + NhincConstants.ADAPTER_XDR_SERVICE_NAME, ex);
        }

        return url;
    }
    protected String getAdapterXDRRequestUrl() {
        String url = null;

        try {
            url = ConnectionManagerCache.getLocalEndpointURLByServiceName(NhincConstants.ADAPTER_XDR_REQUEST_SERVICE_NAME);
        } catch (ConnectionManagerException ex) {
            getLogger().error("Error: Failed to retrieve url for service: " + NhincConstants.ADAPTER_XDR_REQUEST_SERVICE_NAME, ex);
        }

        return url;
    }
    protected AdapterXDRRequestPortType getAdapterXDRRequestPort(String url) {
        AdapterXDRRequestPortType port = getAdapterXDRRequestService().getAdapterXDRRequestPort();
        gov.hhs.fha.nhinc.webserviceproxy.WebServiceProxyHelper.getInstance().initializePort((javax.xml.ws.BindingProvider) port, url);
        return port;
    }
    protected AdapterXDRPortType getAdapterXDRPort(String url) {
        AdapterXDRPortType port = getAdapterXDRService().getAdapterXDRPort();
        gov.hhs.fha.nhinc.webserviceproxy.WebServiceProxyHelper.getInstance().initializePort((javax.xml.ws.BindingProvider) port, url);
        return port;
    }

    protected AdapterXDRService getAdapterXDRService()
    {
        if (adapterXDRService == null){
            return new AdapterXDRService();
        }else{
            return adapterXDRService;
        }

    }
    protected AdapterXDRRequestService getAdapterXDRRequestService()
    {
        if (requestService == null){
            return new AdapterXDRRequestService();
        }else{
            return requestService;
        }

    }

}