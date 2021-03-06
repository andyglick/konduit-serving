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

package ai.konduit.serving.executioner.inference;

import ai.konduit.serving.config.ParallelInferenceConfig;
import ai.konduit.serving.model.loader.ModelLoader;
import ai.konduit.serving.model.loader.samediff.SameDiffModelLoader;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.nd4j.autodiff.execution.NativeGraphExecutioner;
import org.nd4j.autodiff.execution.conf.ExecutionMode;
import org.nd4j.autodiff.execution.conf.ExecutorConfiguration;
import org.nd4j.autodiff.execution.conf.OutputMode;
import org.nd4j.autodiff.samediff.SameDiff;
import org.nd4j.common.base.Preconditions;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.ops.executioner.OpExecutioner;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * An {@link InferenceExecutioner}
 * for use with the {@link TensorFlowThreadPool}
 *
 * @author Adam Gibson
 */
@Slf4j
public class SameDiffInferenceExecutioner implements InferenceExecutioner<ModelLoader<SameDiff>, INDArray[], INDArray[],
        ParallelInferenceConfig, SameDiff> {

    @Getter
    private ModelLoader<SameDiff> modelLoader;
    private NativeGraphExecutioner nativeGraphExecutioner;
    private static ExecutorConfiguration configuration = ExecutorConfiguration.builder()
            .executionMode(ExecutionMode.SEQUENTIAL)
            .outputMode(OutputMode.EXPLICIT)
            .profilingMode(OpExecutioner.ProfilingMode.DISABLED)
            .gatherTimings(true)
            .build();


    private List<String> inputs,outputs;
    private SameDiff model;

    @Override
    public ModelLoader<SameDiff> modelLoader() {
        return modelLoader;
    }

    @Override
    public SameDiff model() {
        try {
            return modelLoader.loadModel();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void initialize(ModelLoader<SameDiff> model, ParallelInferenceConfig config) {
        nativeGraphExecutioner = new NativeGraphExecutioner();
        this.modelLoader = model;
        this.model = model();
        SameDiffModelLoader sameDiffModelLoader = (SameDiffModelLoader) model;
        this.inputs = sameDiffModelLoader.getInputNames();
        this.outputs = sameDiffModelLoader.getOutputNames();
        log.info("Inference execution loaded with inputs " + inputs + " and outputs " + outputs);
    }

    @Override
    public INDArray[] execute(INDArray[] input) {
        Preconditions.checkNotNull(input,"Inputs must not be null!");
        Preconditions.checkState(input.length == this.model.inputs().size(),"Number of inputs %s did not equal number of model inputs %s!",input.length,model.inputs().size());

        synchronized (this.model) {
            Map<String, INDArray> inputs = new LinkedHashMap(input.length);

            for (int i = 0; i < input.length; i++) {
                inputs.put(this.model.inputs().get(i), input[i]);
                this.model.associateArrayWithVariable(input[i], this.model.inputs().get(i));
            }

            model.output(inputs,outputs);
            Map<String, INDArray> ret =  model.output(inputs,outputs);
            INDArray[] returnOutput = new INDArray[outputs.size()];
            for(int i = 0; i < returnOutput.length; i++) {
                returnOutput[i] = ret.get(outputs.get(i));
            }

            return returnOutput;
        }
    }

    @Override
    public void stop() {
    }
}
