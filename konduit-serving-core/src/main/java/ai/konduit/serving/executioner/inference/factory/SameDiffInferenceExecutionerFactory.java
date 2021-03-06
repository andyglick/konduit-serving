/*
 *
 *  * ******************************************************************************
 *  *  * Copyright (c) 2015-2019 Skymind Inc.
 *  *  * Copyright (c) 2019 Konduit AI.
 *  *  *
 *  *  * This program and the accompanying materials are made available under the
 *  *  * terms of the Apache License, Version 2.0 which is available at
 *  *  * https://www.apache.org/licenses/LICENSE-2.0.
 *  *  *
 *  *  * Unless required by applicable law or agreed to in writing, software
 *  *  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  *  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  *  * License for the specific language governing permissions and limitations
 *  *  * under the License.
 *  *  *
 *  *  * SPDX-License-Identifier: Apache-2.0
 *  *  *****************************************************************************
 *
 *
 */

package ai.konduit.serving.executioner.inference.factory;

import ai.konduit.serving.config.ParallelInferenceConfig;
import ai.konduit.serving.executioner.inference.InitializedInferenceExecutionerConfig;
import ai.konduit.serving.executioner.inference.SameDiffInferenceExecutioner;
import ai.konduit.serving.model.loader.samediff.SameDiffModelLoader;
import ai.konduit.serving.pipeline.step.ModelStep;
import ai.konduit.serving.pipeline.step.model.SameDiffStep;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.List;

@Slf4j
public class SameDiffInferenceExecutionerFactory implements InferenceExecutionerFactory {
    @Override
    public InitializedInferenceExecutionerConfig create(ModelStep modelPipelineStepConfig) throws Exception {
        SameDiffStep sameDiffStep = null;
        ParallelInferenceConfig parallelInferenceConfig = modelPipelineStepConfig.getParallelInferenceConfig();

        try {
            sameDiffStep = (SameDiffStep) modelPipelineStepConfig;
        } catch (Exception e) {
            log.error("Could not extract SameDiffConfig. Did you provide one to your verticle?");
        }

        SameDiffModelLoader modelLoader = new SameDiffModelLoader(
                new File(sameDiffStep.getPath()),
                modelPipelineStepConfig.getInputNames(),
                modelPipelineStepConfig.getOutputNames());

        SameDiffInferenceExecutioner inferenceExecutioner = new SameDiffInferenceExecutioner();
        inferenceExecutioner.initialize(modelLoader, parallelInferenceConfig);
        List<String> inputNames = modelPipelineStepConfig.getInputNames();
        List<String> outputNames = modelPipelineStepConfig.getOutputNames();
        if (inputNames.isEmpty() || outputNames.isEmpty()) {
            throw new IllegalStateException("Input and output names are empty!");
        }

        return new InitializedInferenceExecutionerConfig(inferenceExecutioner, inputNames, outputNames);
    }
}
