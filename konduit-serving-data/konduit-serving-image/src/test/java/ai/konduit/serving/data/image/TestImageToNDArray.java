package ai.konduit.serving.data.image;

import ai.konduit.serving.data.image.convert.ImageToNDArrayConfig;
import ai.konduit.serving.data.image.convert.config.ImageNormalization;
import ai.konduit.serving.data.image.convert.config.NDChannelLayout;
import ai.konduit.serving.data.image.convert.config.NDFormat;
import ai.konduit.serving.data.image.step.ndarray.ImageToNDArrayStep;
import ai.konduit.serving.pipeline.api.data.*;
import ai.konduit.serving.pipeline.api.data.Image;
import ai.konduit.serving.pipeline.api.pipeline.Pipeline;
import ai.konduit.serving.pipeline.api.pipeline.PipelineExecutor;
import ai.konduit.serving.pipeline.impl.pipeline.SequencePipeline;
import org.junit.Test;
import org.nd4j.common.resources.Resources;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;

public class TestImageToNDArray {

    @Test
    public void testBasic(){

        File f = Resources.asFile("data/5_32x32.png");

        Image i = Image.create(f);

        Data in = Data.singleton("image", i);

        Pipeline p = SequencePipeline.builder()
                .add(ImageToNDArrayStep.builder()
                        .config(ImageToNDArrayConfig.builder()
                                .height(128)
                                .width(128)
                        .build())
                .build())
                .build();

        PipelineExecutor exec = p.executor();

        Data out = exec.exec(null, in);

        assertTrue(out.has("image"));
        assertEquals(ValueType.NDARRAY, out.type("image"));
        NDArray arr = out.getNDArray("image");
        assertEquals(NDArrayType.FLOAT, arr.type());
        assertArrayEquals(new long[]{1, 3, 128, 128}, arr.shape());
    }

    @Test
    public void testColorChannels(){
        //Make sure we get right channels order - for given


        int h = 32;
        int w = 28; //Intentionally different for this test

        for(boolean scaleNorm : new boolean[]{false, true}) {

            for (String color : new String[]{"red", "green", "blue"}) {
                int[][][] rgbArr = singleColorRGB(h, w, color);
                BufferedImage bi = toBufferedImage(rgbArr);
                Image i = Image.create(bi);

                Data d = Data.singleton("image", i);

                for (boolean rgb : new boolean[]{true, false}) {
                    for (NDFormat f : NDFormat.values()) {           //Channels first or channels last
                        for (boolean leadingDim : new boolean[]{false, true}) {
                            System.out.println(color + " - rgb=" + rgb + " - " + f + " - scaleNorm = " + scaleNorm);

                            Pipeline p = SequencePipeline.builder()
                                    .add(ImageToNDArrayStep.builder()
                                            .config(ImageToNDArrayConfig.builder()
                                                    .height(h)
                                                    .width(w)
                                                    .channelLayout(rgb ? NDChannelLayout.RGB : NDChannelLayout.BGR)
                                                    .format(f)
                                                    .includeMinibatchDim(leadingDim)
                                                    .normalization(new ImageNormalization(scaleNorm ? ImageNormalization.Type.SCALE : ImageNormalization.Type.NONE))
                                                    .build())
                                            .build())
                                    .build();

                            PipelineExecutor exec = p.executor();

                            Data out = exec.exec(null, d);

                            assertTrue(out.has("image"));
                            assertEquals(ValueType.NDARRAY, out.type("image"));
                            NDArray arr = out.getNDArray("image");
                            assertEquals(NDArrayType.FLOAT, arr.type());

                            long[] expShape;
                            if (f == NDFormat.CHANNELS_FIRST) {
                                expShape = leadingDim ? new long[]{1, 3, h, w} : new long[]{3, h, w};
                            } else {
                                expShape = leadingDim ? new long[]{1, h, w, 3} : new long[]{h, w, 3};
                            }

                            assertArrayEquals(expShape, arr.shape());

                            //Check actual values:
                            float[][][] fArr;
                            if (leadingDim) {
                                fArr = arr.getAs(float[][][][].class)[0];
                            } else {
                                fArr = arr.getAs(float[][][].class);
                            }
                            int cDim = (f == NDFormat.CHANNELS_FIRST) ? 0 : 2;
                            int idx255 = -1;
                            if (rgb) {
                                switch (color) {
                                    case "red":
                                        idx255 = 0;
                                        break;
                                    case "green":
                                        idx255 = 1;
                                        break;
                                    case "blue":
                                        idx255 = 2;
                                }
                            } else {
                                //BGR
                                switch (color) {
                                    case "red":
                                        idx255 = 2;
                                        break;
                                    case "green":
                                        idx255 = 1;
                                        break;
                                    case "blue":
                                        idx255 = 0;
                                }
                            }

                            float maxExp = scaleNorm ? 1.0f : 255.0f;
                            if (f == NDFormat.CHANNELS_FIRST) {
                                //CHW
                                for (int y = 0; y < h; y++) {
                                    for (int x = 0; x < w; x++) {
                                        for (int c = 0; c < 3; c++) {
                                            if (c == idx255) {
                                                assertEquals(maxExp, fArr[c][y][x], 0.0f);
                                            } else {
                                                assertEquals(0, fArr[c][y][x], 0.0f);
                                            }
                                        }
                                    }
                                }
                            } else {
                                //HWC
                                for (int y = 0; y < h; y++) {
                                    for (int x = 0; x < w; x++) {
                                        for (int c = 0; c < 3; c++) {
                                            if (c == idx255) {
                                                assertEquals(maxExp, fArr[y][x][c], 0.0f);
                                            } else {
                                                assertEquals(0, fArr[y][x][c], 0.0f);
                                            }
                                        }
                                    }
                                }
                            }

                            String json = p.toJson();
                            Pipeline pJson = Pipeline.fromJson(json);
                            assertEquals(p, pJson);

                            Data outPJson = pJson.executor().exec(null, d);
                            assertEquals(out, outPJson);
                        }
                    }
                }
            }
        }
    }


    @Test
    public void testDataTypes(){
        //Test image -> Float16, Float32, Float64, int8, int16, int32, int64, uint8, unt16, uint32, uint64
        for(NDArrayType t : new NDArrayType[]{NDArrayType.FLOAT, NDArrayType.DOUBLE,
                NDArrayType.INT8, NDArrayType.INT16, NDArrayType.INT32, NDArrayType.INT64,
                //TODO - these types: not yet supported
                //NDArrayType.FLOAT16, NDArrayType.BFLOAT16, NDArrayType.UINT8, NDArrayType.UINT16, NDArrayType.UINT32, NDArrayType.UINT64
        }){

            int h = 32;
            int w = 28; //Intentionally different for this test

            for(String color : new String[]{"red", "green", "blue"}) {
                int[][][] rgbArr = singleColorRGB(h, w, color);
                BufferedImage bi = toBufferedImage(rgbArr);
                Image i = Image.create(bi);

                Data d = Data.singleton("image", i);

                for (boolean rgb : new boolean[]{true, false}) {
                    for (NDFormat f : NDFormat.values()) {           //Channels first or channels last
                        for (boolean leadingDim : new boolean[]{false, true}) {
                            System.out.println(t + " - " + color + " - rgb=" + rgb + " - " + f);
                            Pipeline p = SequencePipeline.builder()
                                    .add(ImageToNDArrayStep.builder()
                                            .config(ImageToNDArrayConfig.builder()
                                                    .height(h)
                                                    .width(w)
                                                    .channelLayout(rgb ? NDChannelLayout.RGB : NDChannelLayout.BGR)
                                                    .format(f)
                                                    .includeMinibatchDim(leadingDim)
                                                    .dataType(t)
                                                    .build())
                                            .build())
                                    .build();

                            PipelineExecutor exec = p.executor();
                            Data out = exec.exec(null, d);
                            NDArray n = out.getNDArray("image");

                            assertTrue(out.has("image"));
                            assertEquals(ValueType.NDARRAY, out.type("image"));
                            NDArray arr = out.getNDArray("image");
                            assertEquals(t, arr.type());

                            long[] expShape;
                            if(f == NDFormat.CHANNELS_FIRST){
                                expShape = leadingDim ? new long[]{1, 3, h, w} : new long[]{3, h, w};
                            } else {
                                expShape = leadingDim ? new long[]{1, h, w, 3} : new long[]{h, w, 3};
                            }

                            assertArrayEquals(expShape, arr.shape());

                            assertEquals(t, n.type());


                            String json = p.toJson();
                            Pipeline pJson = Pipeline.fromJson(json);
                            assertEquals(p, pJson);

                            Data outPJson = pJson.executor().exec(null, d);
                            assertEquals(out, outPJson);
                        }
                    }
                }
            }
        }
    }

    public int[][][] singleColorRGB(int h, int w, String color){

        int idx;
        switch (color.toLowerCase()){
            case "red":
                idx = 0;
                break;
            case "green":
                idx = 1;
                break;
            case "blue":
                idx = 2;
                break;
            default:
                throw new RuntimeException();
        }

        int[][][] out = new int[3][h][w];
        for( int y=0; y<h; y++ ){
            for( int x=0; x<w; x++ ){
                out[idx][y][x] = 255;
            }
        }

        return out;
    }


    /**
     * Convert an integer array (CHW, in RGB format) to a BufferedImage
     * @param img
     * @return
     */
    public BufferedImage toBufferedImage(int[][][] img){

        int h = img[0].length;
        int w = img[0][0].length;
        BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

        for( int i=0; i<h; i++){
            for( int j=0; j<w; j++ ){
                int rgb = img[0][i][j] << 16 | img[1][i][j] << 8 | img[2][i][j];
                bi.setRGB(j, i, rgb);
            }
        }

        return bi;
    }

    public BufferedImage createConstantImageRgb(int h, int w, int red, int green, int blue){
        BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

        for( int i=0; i<h; i++){
            for( int j=0; j<w; j++ ){
                int rgb = red << 16 | green << 8 | blue;
                bi.setRGB(j, i, rgb);
            }
        }

        return bi;
    }


    @Test
    public void testImageNormalizationRgbBgr(){
        //Test image normalization - RGB and BGR

        int r = 255;
        int g = 128;
        int b = 32;
        BufferedImage bi = createConstantImageRgb(32, 32, r, g, b);

        double[] meanRgb = {128, 200, 50};
        double[] stdRgb = {180, 140, 100};

        Data in = Data.singleton("image", Image.create(bi));

        for(ImageNormalization.Type normType : ImageNormalization.Type.values()){
            float expR, expG, expB;
            switch (normType){
                case NONE:
                    expR = r;
                    expG = g;
                    expB = b;
                    break;
                case SCALE:
                    expR = r / 255f;
                    expG = g / 255f;
                    expB = b / 255f;
                    break;
                case SUBTRACT_MEAN:
                    expR = r - (float)meanRgb[0];
                    expG = g - (float)meanRgb[1];
                    expB = b - (float)meanRgb[2];
                    break;
                case STANDARDIZE:
                    expR = (r - (float)meanRgb[0]) / (float)stdRgb[0];
                    expG = (g - (float)meanRgb[1]) / (float)stdRgb[1];
                    expB = (b - (float)meanRgb[2]) / (float)stdRgb[2];
                    break;
                case INCEPTION:
                    expR = ((r / 255f) - 0.5f) * 2.0f;
                    expG = ((g / 255f) - 0.5f) * 2.0f;
                    expB = ((b / 255f) - 0.5f) * 2.0f;
                    break;
                case VGG_SUBTRACT_MEAN:
                    double[] vggmean = ImageNormalization.getVggMeanRgb();
                    expR = r - (float)vggmean[0];
                    expG = g - (float)vggmean[1];
                    expB = b - (float)vggmean[2];
                    break;
                default:
                    throw new RuntimeException();
            }

            boolean needsMean = normType == ImageNormalization.Type.SUBTRACT_MEAN || normType == ImageNormalization.Type.STANDARDIZE;
            boolean needsStd = normType == ImageNormalization.Type.STANDARDIZE;

            ImageNormalization norm = new ImageNormalization()
                    .type(normType)
                    .meanRgb(needsMean ? meanRgb : null)
                    .stdRgb(needsStd ? stdRgb : null);


            for(boolean rgb : new boolean[]{true, false}) {
                for(NDFormat f : NDFormat.values()) {


                    Pipeline p = SequencePipeline.builder()
                            .add(ImageToNDArrayStep.builder()
                                    .outputNames(Collections.singletonList("im2ndarray"))
                                    .config(ImageToNDArrayConfig.builder()
                                            .normalization(norm)
                                            .height(32)
                                            .width(32)
                                            .channelLayout(rgb ? NDChannelLayout.RGB : NDChannelLayout.BGR)
                                            .format(f)
                                            .includeMinibatchDim(false)
                                            .dataType(NDArrayType.FLOAT)
                                            .build())
                                    .build())
                            .build();

                    PipelineExecutor exec = p.executor();
                    Data out = exec.exec(null, in);

                    NDArray arr = out.getNDArray("im2ndarray");
                    assertEquals(NDArrayType.FLOAT, arr.type());

                    float[][][] fArr = arr.getAs(float[][][].class);


                    if(f == NDFormat.CHANNELS_FIRST){
                        //CHW
                        for( int c = 0; c<3; c++){
                            float e;
                            if(rgb) {
                                if(c == 0) e = expR;
                                else if(c == 1) e = expG;
                                else e = expB;
                            } else {
                                if(c == 0) e = expB;
                                else if(c == 1) e = expG;
                                else e = expR;
                            }

                            for( int y=0; y<32; y++){
                                for( int x=0; x<32; x++ ){
                                    float a = fArr[c][y][x];
                                    assertEquals(e, a, 1e-4f);
                                }
                            }
                        }
                    } else {
                        //HWC
                        for( int y=0; y<32; y++){
                            for( int x=0; x<32; x++ ){
                                for( int c = 0; c<3; c++){
                                    float e;
                                    if(rgb) {
                                        if(c == 0) e = expR;
                                        else if(c == 1) e = expG;
                                        else e = expB;
                                    } else {
                                        if(c == 0) e = expB;
                                        else if(c == 1) e = expG;
                                        else e = expR;
                                    }

                                    float a = fArr[y][x][c];
                                    assertEquals(e, a, 1e-4f);
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    @Test
    public void testImageNormalizationNonRgb(){
        //Test image normalization - RGBA, BGRA, GRAYSCALE

        System.out.println("***** NON-RGB NORMALIZATION NOT YET IMPLEMENTED *****");
    }

}