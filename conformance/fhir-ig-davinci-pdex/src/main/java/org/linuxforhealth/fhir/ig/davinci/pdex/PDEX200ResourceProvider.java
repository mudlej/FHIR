/*
 * (C) Copyright IBM Corp. 2022
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.linuxforhealth.fhir.ig.davinci.pdex;

import org.linuxforhealth.fhir.registry.util.PackageRegistryResourceProvider;

public class PDEX200ResourceProvider extends PackageRegistryResourceProvider {
    @Override
    public String getPackageId() {
        return "hl7.fhir.us.davinci-pdex.200";
    }
}