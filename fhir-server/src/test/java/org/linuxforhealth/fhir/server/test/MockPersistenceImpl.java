/*
 * (C) Copyright IBM Corp. 2020, 2022
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.linuxforhealth.fhir.server.test;

import java.time.ZoneOffset;
import java.util.List;
import java.util.function.Function;

import org.linuxforhealth.fhir.model.resource.OperationOutcome;
import org.linuxforhealth.fhir.model.resource.OperationOutcome.Builder;
import org.linuxforhealth.fhir.model.resource.Patient;
import org.linuxforhealth.fhir.model.resource.Resource;
import org.linuxforhealth.fhir.model.type.Id;
import org.linuxforhealth.fhir.model.type.Instant;
import org.linuxforhealth.fhir.model.type.Meta;
import org.linuxforhealth.fhir.persistence.FHIRPersistence;
import org.linuxforhealth.fhir.persistence.FHIRPersistenceTransaction;
import org.linuxforhealth.fhir.persistence.HistorySortOrder;
import org.linuxforhealth.fhir.persistence.InteractionStatus;
import org.linuxforhealth.fhir.persistence.MultiResourceResult;
import org.linuxforhealth.fhir.persistence.ResourceChangeLogRecord;
import org.linuxforhealth.fhir.persistence.ResourcePayload;
import org.linuxforhealth.fhir.persistence.ResourceResult;
import org.linuxforhealth.fhir.persistence.SingleResourceResult;
import org.linuxforhealth.fhir.persistence.context.FHIRPersistenceContext;
import org.linuxforhealth.fhir.persistence.exception.FHIRPersistenceException;
import org.linuxforhealth.fhir.persistence.payload.PayloadPersistenceResponse;
import org.linuxforhealth.fhir.server.util.FHIRRestHelperTest;

/**
 * Mock implementation of FHIRPersistence for use during testing.
 */
public class MockPersistenceImpl implements FHIRPersistence {
    int id = 0;

    @Override
    public <T extends Resource> SingleResourceResult<T> create(FHIRPersistenceContext context, T resource)
            throws FHIRPersistenceException {

        SingleResourceResult.Builder<T> resultBuilder = new SingleResourceResult.Builder<T>()
                .success(true)
                .interactionStatus(InteractionStatus.MODIFIED)
                .resource(resource);
        return resultBuilder.build();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Resource> SingleResourceResult<T> read(FHIRPersistenceContext context, Class<T> resourceType, String logicalId)
            throws FHIRPersistenceException {
        // TODO. Why this logic? Definitely worthy of a comment
        if (logicalId.startsWith("generated")) {
            return new SingleResourceResult.Builder<T>()
                    .interactionStatus(InteractionStatus.READ)
                    .success(true)
                    .resource(null).build();
        } else {
            T updatedResource = (T) Patient.builder().id("test").meta(Meta.builder().versionId(Id.of("1")).lastUpdated(Instant.now()).build()).build();
            return new SingleResourceResult.Builder<T>()
                    .interactionStatus(InteractionStatus.READ)
                    .success(true)
                    .resource(updatedResource).build();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Resource> SingleResourceResult<T> vread(FHIRPersistenceContext context, Class<T> resourceType, String logicalId, String versionId)
            throws FHIRPersistenceException {
        if (logicalId.startsWith("generated")) {
            return new SingleResourceResult.Builder<T>()
                    .success(true)
                    .interactionStatus(InteractionStatus.READ)
                    .resource(null).build();
        } else {
            T updatedResource = (T) Patient.builder().id("test").meta(Meta.builder().versionId(Id.of("1")).lastUpdated(Instant.now()).build()).build();
            return new SingleResourceResult.Builder<T>()
                    .success(true)
                    .interactionStatus(InteractionStatus.READ)
                    .resource(updatedResource).build();
        }
    }

    @Override
    public <T extends Resource> SingleResourceResult<T> update(FHIRPersistenceContext context, T resource)
            throws FHIRPersistenceException {
        OperationOutcome operationOutcome = null;
        if (resource.getLanguage() != null && resource.getLanguage().getValue().equals("en-US")) {
            operationOutcome = FHIRRestHelperTest.ID_SPECIFIED;
        }
        SingleResourceResult.Builder<T> resultBuilder = new SingleResourceResult.Builder<T>()
                .success(true)
                .resource(resource) // persistence layer should no longer change the resource!
                .interactionStatus(InteractionStatus.MODIFIED)
                .outcome(operationOutcome);
        return resultBuilder.build();
    }

    @Override
    public MultiResourceResult history(FHIRPersistenceContext context, Class<? extends Resource> resourceType, String logicalId) throws FHIRPersistenceException {
        Instant lastUpdated = Instant.now(ZoneOffset.UTC);
        Patient updatedResource = Patient.builder().id("test").meta(Meta.builder().versionId(Id.of("1")).lastUpdated(lastUpdated).build()).build();
        ResourceResult<Resource> resourceResult = ResourceResult.builder()
                .resource(updatedResource)
                .logicalId(logicalId)
                .version(1)
                .resourceTypeName(updatedResource.getClass().getSimpleName())
                .lastUpdated(lastUpdated.getValue().toInstant())
                .build();
        return MultiResourceResult.builder()
                .success(true)
                .resourceResult(resourceResult).build();
    }

    @Override
    public MultiResourceResult search(FHIRPersistenceContext context, Class<? extends Resource> resourceType) throws FHIRPersistenceException {
        return MultiResourceResult.builder()
                .success(true)
                .build();
    }

    @Override
    public boolean isTransactional() {
        return true;
    }

    @Override
    public FHIRPersistenceTransaction getTransaction() {
        return new MockTransactionAdapter();
    }

    @Override
    public OperationOutcome getHealth() throws FHIRPersistenceException {
        return null;
    }

    @Override
    public String generateResourceId() {
        return "generated-" + id++;
    }

    @Override
    public int reindex(FHIRPersistenceContext context, Builder operationOutcomeResult, java.time.Instant tstamp, List<Long> indexIds,
        String resourceLogicalId, boolean force) throws FHIRPersistenceException {
        return 0;
    }

    @Override
    public <T extends Resource> void delete(FHIRPersistenceContext context, Class<T> resourceType, String logicalId, int versionId,
            org.linuxforhealth.fhir.model.type.Instant lastUpdated) throws FHIRPersistenceException {
        // NOP. No need to do anything in this very simple mock
    }

    @Override
    public ResourcePayload fetchResourcePayloads(Class<? extends Resource> resourceType, java.time.Instant fromLastModified,
        java.time.Instant toLastModified, Function<ResourcePayload, Boolean> process) throws FHIRPersistenceException {
        // NOP
        return null;
    }

    @Override
    public List<ResourceChangeLogRecord> changes(FHIRPersistenceContext context, int resourceCount, java.time.Instant sinceLastModified, java.time.Instant beforeLastModified,
            Long changeIdMarker, List<String> resourceTypeNames, boolean excludeTransactionTimeoutWindow, HistorySortOrder historySortOrder)
            throws FHIRPersistenceException {
        // NOP
        return null;
    }

    @Override
    public List<Long> retrieveIndex(FHIRPersistenceContext context, int count, java.time.Instant notModifiedAfter, Long afterIndexId, String resourceTypeName) throws FHIRPersistenceException {
        // NOP
        return null;
    }

    @Override
    public PayloadPersistenceResponse storePayload(Resource resource, String logicalId, int newVersionNumber, String resourcePayloadKey) throws FHIRPersistenceException {
        // NOP
        return null;
    }

    @Override
    public List<Resource> readResourcesForRecords(List<ResourceChangeLogRecord> records) throws FHIRPersistenceException {
        // NOP
        return null;
    }
}