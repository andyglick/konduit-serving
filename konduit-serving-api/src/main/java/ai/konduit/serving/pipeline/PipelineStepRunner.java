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

package ai.konduit.serving.pipeline;

import ai.konduit.serving.InferenceConfiguration;
import ai.konduit.serving.pipeline.step.CustomPipelineStep;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.datavec.api.records.Record;
import org.datavec.api.writable.Writable;
import org.nd4j.linalg.api.ndarray.INDArray;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.io.Closeable;


/**
 * Pipeline steps represent a component
 * in pre processing data ending
 * in data in an {@link INDArray} form.
 * <p>
 * A runner is the actual implementation
 * of the {@link BasePipelineStep}
 * which is just a configuration interface
 * for a runner.
 * <p>
 * A runner takes in 1 or more input
 * {@link Record} and returns 1 or more output {@link Record}.
 * <p>
 * There are a  number of implementations. You can also create a custom one
 * using the {@link CustomPipelineStep} and {@link CustomPipelineStepUDF}
 * definitions. This is recommended as the easiest way of creating your own custom ones.
 * Otherwise, we try to provide any number of off the shelf ones
 * for running python scripts or machine learning models.
 *
 * @author Adam Gibson
 */
@Path("/")
@Consumes("application/json")
@Produces("application/json")
public interface PipelineStepRunner extends Closeable {

    /**
     * Destroy the pipeline runner.
     * <p>
     * This means cleaning up used resources.
     * This method will be called when a pipeline needs to be finalized.
     */
    void close();

    PipelineStep<?> getPipelineStep();

    /**
     * Transform a set of {@link Object}
     * via this operation.
     *
     * @param input the input array
     * @return the output from the transform
     */
    Writable[][] transform(Object... input);

    /**
     * Transform a set of {@link Object}
     * via this operation.
     *
     * @param input the input array
     * @return the output from the transform
     */
    Writable[][] transform(Object[][] input);

    /**
     * Transform a set of {@link INDArray}
     * via this operation.
     *
     * @param input the input array
     * @return the output from the transform
     */
    @POST
    @Path("/transform")
    @Operation(summary = "Find pet by ID",
            tags = {"pets"},
            description = "Returns a pet when 0 < ID <= 10.  ID > 10 or nonintegers will simulate API error conditions",
            responses = {
                    @ApiResponse(description = "The pet", content = @Content(
                            schema = @Schema(implementation = InferenceConfiguration.class)
                    ))
            })
    Record[] transform(Record[] input);
}