/*
 *
 *  * ******************************************************************************
 *  *  * Copyright (c) 2020 Konduit AI.
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
import ai.konduit.serving.config.Input;
import ai.konduit.serving.config.Output;
import ai.konduit.serving.pipeline.step.WordPieceTokenizerStep;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.junit.Assert.assertNotNull;

public class WordPieceTokenizerStepSerializationTests
{
    protected static InferenceConfiguration config;
    protected static WordPieceTokenizerStep wordPieceTokenizerStep;

    @BeforeClass
    public static void initialize() throws Exception
    {
        wordPieceTokenizerStep = WordPieceTokenizerStep.builder()
                .sentenceMaxLen(256)
                .vocabPath("bert-base-uncased-vocab.txt")
                .build();

        config = InferenceConfiguration.builder()
                .step(wordPieceTokenizerStep)
                .build();
    }
    @Test
    public void testWordPieceStepJsonSanity() throws Exception
    {
        assertEquals(config, InferenceConfiguration.fromJson(config.toJson()));
    }

    @Test
    public void testWordPieceStepYamlSanity() throws Exception
    {
        assertEquals(config, InferenceConfiguration.fromYaml(config.toYaml()));
    }

    @Test
    public void testWordPieceFromJson()
    {
        assertNotNull(WordPieceTokenizerStep.fromJson(wordPieceTokenizerStep.toJson()));
    }

    @Test
    public void testWordPieceFromYaml()
    {
        assertNotNull(WordPieceTokenizerStep.fromYaml(wordPieceTokenizerStep.toYaml()));
    }

    @Test
    public void testWordPieceValidInputType()
    {
        assertEquals(Input.DataFormat.JSON, wordPieceTokenizerStep.validInputTypes()[0]);
    }

    @Test
    public void testWordPieceValidOutputType()
    {
        assertEquals(Output.DataFormat.NUMPY, wordPieceTokenizerStep.validOutputTypes()[0]);
    }

    @Test
    public void testWorkPieceStepConfig() throws Exception
    {
        WordPieceTokenizerStep wordConfig = WordPieceTokenizerStep.builder()
                .sentenceMaxLen(150)
                .vocabPath("sample.txt")
                .build();

        InferenceConfiguration anotherConfig = InferenceConfiguration.builder()
                .step(wordConfig)
                .build();
        assertNotEquals(config, anotherConfig);
    }
}