/*
 * (C) Copyright IBM Corp. 2017, 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ibm.fhir.persistence.jdbc.dto;

import java.util.Objects;

import com.ibm.fhir.persistence.exception.FHIRPersistenceException;
import com.ibm.fhir.persistence.jdbc.util.ParameterHashUtil;
import com.ibm.fhir.search.SearchConstants.Type;
import com.ibm.fhir.search.util.ReferenceValue;

/**
 * DTO representing external and local reference parameters
 */
public class ReferenceParmVal extends ExtractedParameterValue {

    // The value of the reference after it has been processed to determine target resource type, version etc.
    private ReferenceValue refValue;

    /**
     * Public constructor
     */
    public ReferenceParmVal() {
        super();
    }

    /**
     * Get the refValue
     * @return
     */
    public ReferenceValue getRefValue() {
        return this.refValue;
    }

    /**
     * Set the refValue
     * @param refValue
     */
    public void setRefValue(ReferenceValue refValue) {
        this.refValue = refValue;
    }

    public Type getType() {
        return Type.REFERENCE;
    }

    /**
     * We know our type, so we can call the correct method on the visitor
     */
    @Override
    public void accept(ExtractedParameterValueVisitor visitor) throws FHIRPersistenceException {
        visitor.visit(this);
    }

    @Override
    public String getHash(ParameterHashUtil parameterHashUtil) {
        StringBuilder sb = new StringBuilder();
        sb.append(Objects.toString(refValue.getTargetResourceType(), ""));
        sb.append("|").append(Objects.toString(refValue.getValue(), ""));
        sb.append("|").append(Objects.toString(refValue.getType(), ""));
        sb.append("|").append(Objects.toString(refValue.getVersion(), ""));
        return parameterHashUtil.getNameValueHash(getHashHeader(), sb.toString());
    }
}