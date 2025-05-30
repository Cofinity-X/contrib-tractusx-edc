/********************************************************************************
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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
 ********************************************************************************/

package org.eclipse.tractusx.edc.agreements.retirement.store.sql;

import org.eclipse.edc.sql.translation.TranslationMapping;

public class AgreementRetirementMapping extends TranslationMapping {
    private static final String FIELD_ID = "agreementId";
    private static final String FIELD_REASON = "reason";
    private static final String FIELD_AGREEMENT_RETIREMENT_DATE = "agreementRetirementDate";

    AgreementRetirementMapping(PostgresAgreementRetirementStatements statements) {
        add(FIELD_ID, statements.getIdColumn());
        add(FIELD_REASON, statements.getReasonColumn());
        add(FIELD_AGREEMENT_RETIREMENT_DATE, statements.getRetirementDateColumn());
    }
}
