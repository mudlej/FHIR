/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
 
package com.ibm.fhir.server.rest;

import javax.ws.rs.core.MultivaluedMap;

import com.ibm.fhir.config.FHIRRequestContext;
import com.ibm.fhir.model.resource.Resource;
import com.ibm.fhir.model.resource.Bundle.Entry;
import com.ibm.fhir.server.operation.spi.FHIROperationContext;
import com.ibm.fhir.server.operation.spi.FHIRRestOperationResponse;

/**
 * Executes an invoke (custom) operation on the visitor
 */
public class FHIRRestOperationInvoke extends FHIRRestOperationResource {

    private final FHIROperationContext operationContext;
    private final String method;
    private final String resourceTypeName;
    private String logicalId;
    private String versionId;
    private String operationName;
    private Resource resource;
    private MultivaluedMap<String, String> queryParameters;

    /**
     * Public constructor
     * @param operationContext
     * @param resourceTypeName
     * @param logicalId
     * @param versionId
     * @param operationName
     * @param resource
     * @param queryParameters
     */
    public FHIRRestOperationInvoke(int entryIndex, Entry validationResponseEntry, String requestDescription, long initialTime, FHIROperationContext operationContext, String method,
        String resourceTypeName,
        String logicalId, String versionId, String operationName,
        Resource resource, MultivaluedMap<String, String> queryParameters) {
        super(entryIndex, validationResponseEntry, requestDescription, initialTime);
        this.operationContext = operationContext;
        this.method = method;
        this.resourceTypeName = resourceTypeName;
        this.logicalId = logicalId;
        this.versionId = versionId;
        this.operationName = operationName;
        this.resource = resource;
        this.queryParameters = queryParameters;
    }

    @Override
    public FHIRRestOperationResponse accept(FHIRRestOperationVisitor visitor) throws Exception {

        // Make sure the context is configured correctly before we call invoke
        FHIRRequestContext requestContext = FHIRRequestContext.get();
        operationContext.setProperty(FHIROperationContext.PROPNAME_URI_INFO, requestContext.getExtendedOperationProperties(FHIROperationContext.PROPNAME_URI_INFO));
        operationContext.setProperty(FHIROperationContext.PROPNAME_HTTP_HEADERS, requestContext.getExtendedOperationProperties(FHIROperationContext.PROPNAME_HTTP_HEADERS));
        operationContext.setProperty(FHIROperationContext.PROPNAME_SECURITY_CONTEXT, requestContext.getExtendedOperationProperties(FHIROperationContext.PROPNAME_SECURITY_CONTEXT));
        operationContext.setProperty(FHIROperationContext.PROPNAME_HTTP_REQUEST, requestContext.getExtendedOperationProperties(FHIROperationContext.PROPNAME_HTTP_REQUEST));
        operationContext.setProperty(FHIROperationContext.PROPNAME_METHOD_TYPE, method);
        
        
        return visitor.doInvoke(this.method, getEntryIndex(), getValidationResponseEntry(), getRequestDescription(), getInitialTime(), operationContext, resourceTypeName, logicalId, versionId, operationName, resource, queryParameters);
    }
}