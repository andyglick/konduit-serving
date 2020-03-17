/*
 *
 *  * ******************************************************************************
 *  *  * Copyright (c) 2015-2019 Skymind Inc.
 *  *  * Copyright (c) 2019-2020 Konduit AI.
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

package ai.konduit.serving.verticles.onnx;

import ai.konduit.serving.InferenceConfiguration;
import ai.konduit.serving.config.Output;
import ai.konduit.serving.config.ServingConfig;
import ai.konduit.serving.model.ModelConfig;
import ai.konduit.serving.model.ModelConfigType;
import ai.konduit.serving.model.OnnxConfig;
import ai.konduit.serving.pipeline.step.ModelStep;
import ai.konduit.serving.util.image.NativeImageLoader;
import ai.konduit.serving.verticles.BaseVerticleTest;
import ai.konduit.serving.verticles.inference.InferenceVerticle;
import com.jayway.restassured.response.Response;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.After;
import org.junit.runner.RunWith;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.buffer.DataBuffer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.io.ClassPathResource;
import org.nd4j.serde.binary.BinarySerde;
import org.datavec.image.data.Image;
import org.bytedeco.javacpp.FloatPointer;
import org.bytedeco.javacpp.indexer.FloatIndexer;
import org.apache.commons.io.FileUtils;

import javax.annotation.concurrent.NotThreadSafe;
import java.net.URL;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.Arrays;

import static ai.konduit.serving.executioner.PipelineExecutioner.convertBatchOutput;
import static com.jayway.restassured.RestAssured.given;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;


@RunWith(VertxUnitRunner.class)
@NotThreadSafe
public class OnnxMultipleInputsTest extends BaseVerticleTest {

    @Override
    public Class<? extends AbstractVerticle> getVerticalClazz() {
        return InferenceVerticle.class;
    }

    @Override
    public Handler<HttpServerRequest> getRequest() {
        return null;
    }

    @Override
    public JsonObject getConfigObject() throws Exception {    	
	File model = new File("konduit-serving-test/src/test/resources/inference/onnx/add.onnx");

	if(!model.exists()){
	    FileUtils.copyURLToFile(new URL("https://raw.githubusercontent.com/onnx/onnx/master/onnx/backend/test/data/node/test_add/model.onnx"), model);
	}
	String modelPath = new File("src/test/resources/inference/onnx/add.onnx").getAbsolutePath();

        ServingConfig servingConfig = ServingConfig.builder()
                .outputDataFormat(Output.DataFormat.NUMPY)
                .httpPort(port)
                .build();

        OnnxConfig modelConfig = OnnxConfig.builder()
                .modelConfigType(
                        ModelConfigType.builder()
                                .modelType(ModelConfig.ModelType.ONNX)
                                .modelLoadingPath(modelPath)
                                .build()
                ).build();

        ModelStep modelPipelineConfig = ModelStep.builder()
                .modelConfig(modelConfig)
                .inputNames(Arrays.asList("x", "y"))
                .outputNames(Arrays.asList("sum"))
                .build();


        InferenceConfiguration inferenceConfiguration = InferenceConfiguration.builder()
                .servingConfig(servingConfig)
                .step(modelPipelineConfig)
                .build();

//        return new JsonObject();
	return new JsonObject(inferenceConfiguration.toJson());
    }

    @Test
    public void runAdd(TestContext testContext) throws Exception {

	long inputTensorSize = 3 * 4 * 5;

 	FloatPointer inputTensorValues = new FloatPointer(inputTensorSize);
        FloatIndexer idx = FloatIndexer.create(inputTensorValues);
        for (long i = 0; i < inputTensorSize; i++)
          idx.put(i, (float)i / (inputTensorSize + 1));
  
	DataBuffer buffer = Nd4j.createBuffer(inputTensorValues, DataType.FLOAT, inputTensorSize, idx);

        INDArray contents = Nd4j.create(buffer);
	
	byte[] npyContents = Nd4j.toNpyByteArray(contents);

	File inputFile = temporary.newFile();
        FileUtils.writeByteArrayToFile(inputFile, npyContents);

       	Response response = given().port(port)
                .multiPart("x", inputFile)
                .multiPart("y", inputFile) 
                .post("nd4j/numpy")
                .andReturn();

        assertEquals("Response failed", 200, response.getStatusCode());
        
        INDArray bodyResult = Nd4j.createNpyFromByteArray(response.getBody().asByteArray());
        System.out.println(inputTensorValues.get(1));
	System.out.println(bodyResult.getFloat(1));

	assert Math.abs(bodyResult.getFloat(1) - 0.032786883) < 1e-6;	
	
	assertArrayEquals(new long[]{1,60}, bodyResult.shape());
    }

    @After
    public void after(TestContext context) {
      super.after(context);   
    }

}