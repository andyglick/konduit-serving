/*
 *  ******************************************************************************
 *  * Copyright (c) 2020 Konduit K.K.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Apache License, Version 2.0 which is available at
 *  * https://www.apache.org/licenses/LICENSE-2.0.
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  * License for the specific language governing permissions and limitations
 *  * under the License.
 *  *
 *  * SPDX-License-Identifier: Apache-2.0
 *  *****************************************************************************
 */

package ai.konduit.serving.pipeline.impl.pipeline.graph;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * Merge the output of the input GraphSteps together<br>
 * This means that during execution, the output Data instances of all the steps are combined together into a single Data
 * instance.
 *
 * @author Alex Black
 */
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class MergeStep extends BaseMergeStep {

    public MergeStep(GraphBuilder b, List<String> steps, String name) {
        super(b, steps, name);
    }

    @Override
    public String toString() {
        return "Merge(\"" + String.join("\",\"", inputs()) + "\")";
    }

}
