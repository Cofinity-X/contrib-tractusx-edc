/********************************************************************************
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.edc.policy.cx.affiliates;

import org.eclipse.edc.participant.spi.ParticipantAgent;
import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.tractusx.edc.policy.cx.TestParticipantAgentPolicyContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.Mockito.mock;

class AffiliatesRegionConstraintFunctionTest {

    private final ParticipantAgent participantAgent = mock();
    private final AffiliatesRegionProhibitionConstraintFunction<ParticipantAgentPolicyContext> function = new AffiliatesRegionProhibitionConstraintFunction<>();
    private final ParticipantAgentPolicyContext context = new TestParticipantAgentPolicyContext(participantAgent);

    @Test
    void evaluate() {
        assertThat(function.evaluate(Operator.IS_ANY_OF, "BPNL00000000001A", null, context)).isTrue();
    }

    @Test
    void validate_whenValidOperatorAndRightValueArePassed_thenSuccess() {
        var rightValue = List.of("cx.region.europe:1", "cx.region.northAmerica:1");
        var result = function.validate(Operator.IS_ANY_OF, rightValue, null);
        assertThat(result).isSucceeded();
    }

    @Test
    void validate_whenValidOperatorAndOneRightValueIsPassed_thenSuccess() {
        var rightValue = List.of("cx.region.all:1");
        var result = function.validate(Operator.IS_ANY_OF, rightValue, null);
        assertThat(result).isSucceeded();
    }

    @Test
    void validate_whenInvalidOperator_thenFailure() {
        var rightValue = List.of("cx.region.all:1");
        var result = function.validate(Operator.EQ, rightValue, null);
        assertThat(result).isFailed().detail().contains("Invalid operator");
    }

    @Test
    void validate_whenInvalidRightValueType_thenFailure() {
        var result = function.validate(Operator.IS_ANY_OF, "invalid_value", null);
        assertThat(result).isFailed().detail().contains("the following values are not allowed");
    }
}
