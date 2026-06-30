import sc.fiji.segmentationFramework.*;
import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.process.ImageProcessor;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.ArrayUtils;
//Added
import ai.onnxruntime.*;
import ij.ImageStack;
import ij.process.FloatProcessor​;
import ij.process.ColorProcessor;
import ij.process.LUT;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.awt.Point;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.awt.Dimension;
import java.awt.Color;

public class SAM_Template extends SegmentationFramework {
	
	/**
	 * New global variables
	 */
	 
	/** Flag for the state of show_hide_preview() function */
	public boolean showPreview = false;
	/** Default values for parameters in settings */
	public String background = "Black";
	public String output_type = "Probability Map";
	public double threshold = 0.5;
	public String output_return = "All results";
	public String mask_retained = "None";
	public double ratio = 1.0;
	public String physical_units = "pixels"; 
	public String comments = "";
	/** Model variables */
	public OrtEnvironment env;
	public String path_to_encoder;
	public String path_to_decoder;
	public OrtSession encoder_session, decoder_session;
	/** Output mask in case they are retained */
	float[][] low_res_logits = null;
	
	/**
	 * Change of predetermined global variables and initialization of complex new ones
	 */
	public SAM_Template() {
		/** Predetermined variables */
		super(true);
		cSlider.setPreferredSize(new Dimension(100, 5));
		cSlider.setBackground(Color.BLACK);
		zSlider.setPreferredSize(new Dimension(100, 5));
		pluginTitle = "SAM Template";
		numOfClasses = 2;
		numOfTypes = 2;
		classLabels[0] = "Reference Box";
		classLabels[1] = "Reference Mask";
		typeLabels[0] = "Positive point";
		typeLabels[1] = "Negative point";
		nameOfStatistics = new String[]{"Example 1", "Example 2"};
		//previewButton.setToolTipText("Description of the custom aspect of the analysis being previewed");
		numOfStatistics = nameOfStatistics.length;
		/** New variables */
	}
	@Override
	public boolean checkFormat(final String fileName) {
        if (fileName.endsWith(".jpg") || fileName.endsWith(".tiff") || fileName.endsWith(".dicom")
                || fileName.endsWith(".fits") || fileName.endsWith(".pgm") || fileName.endsWith(".bmp")
                || fileName.endsWith(".gif") || fileName.endsWith(".nd2") || fileName.endsWith(".png")) {
            return true;
        }
        return false;
    }
	
	
	/**
	 * Run
	 */
	@Override
	public void run(String arg) {
		/** Choose a folder */
		File dir = new File(IJ.getDir("Select a Directory"));
		path = dir.getAbsolutePath() + "/";
		pathnames = dir.list();
		pathnames = Stream.of(pathnames).filter(str -> checkFormat(str)).collect(Collectors.toList()).toArray(new String[0]);
		/** Add placeholders of global segments and settings for each image */
		while (pathnames.length > totalSegmentsNames.size()) {
			totalSegmentsROIs.add(null);
			totalSegmentsNames.add(null);
			if (useLocales) {
				totalLocalesROIs.add(null);
				totalLocalesNames.add(null);
			}
			settingsValues.add(newImageSettings());
		}
		/** Create the table for the results */
		resultsTable = newResultsTable();
		/** Open the first image or return if the user canceled the dialog */
		analyzedImage = IJ.openImage(path + pathnames[index]);
		if(null == analyzedImage) {return;} 
		/** Create the processor for the analysis and the display images */
		displayImage = analyzedImage.duplicate();
		//IJ.run(analyzedImage, "8-bit", "true");
		setSliders();
		updateSliderLabels();
		setUpFromMetadata();
		/** Create a toolbar for working with the display, selecting the most convenient tool */
		ij.gui.Toolbar.getInstance().setTool(ij.gui.Toolbar.POINT);
		/** Import SAM */
		env = OrtEnvironment.getEnvironment();
		//path_to_encoder = IJ.getFilePath​("Path to encoder model");
		//path_to_decoder = IJ.getFilePath​("Path to decoder model");
		path_to_encoder = "/Volumes/kingstom/Laboratorio/SAM/encoder-quant.onnx";
		path_to_decoder = "/Volumes/kingstom/Laboratorio/SAM/decoder-quant.onnx";
		try {
			encoder_session = env.createSession(path_to_encoder, new OrtSession.SessionOptions());
			decoder_session = env.createSession(path_to_decoder, new OrtSession.SessionOptions());
		} catch (OrtException e) {
			IJ.log("" + e.getCode());
		}
		/** Build GUI */
		win = new CustomWindow(displayImage);
		win.pack();
		win.fixZoom();
	}
	
	/**
	 * Settings functions
	 */
	
	/** Create a list of settings with default values */
	@Override
	public Map<String, Object> newImageSettings() {
		Map<String, Object> defaultSettings = new HashMap();
		defaultSettings.put("Background color", background);
		defaultSettings.put("Output type", output_type);
		defaultSettings.put("Threshold", threshold);
		defaultSettings.put("Results returned", output_return);
		defaultSettings.put("Mask retained", mask_retained);
		defaultSettings.put("Ratio px/u", ratio);
		defaultSettings.put("Physical units", physical_units);
		defaultSettings.put("Comments", comments);
		return defaultSettings;
	}
	
	/** Display the settings */
	@Override
	public void showSettingsDialog() {
		/** Creates the dialog */
		GenericDialogPlus settingsDialog = new GenericDialogPlus("Settings");
		settingsDialog.addMessage("Analysis parameters:");
		settingsDialog.addChoice​("Background color", new String[] {"Black", "White"}, (String) settingsValues.get(index).get("Background color"));
		settingsDialog.addChoice​("Output type", new String[] {"Mask", "Probability Map"}, (String) settingsValues.get(index).get("Output type"));
		settingsDialog.addSlider​("Threshold when masking", 0.01, 0.99, (double) settingsValues.get(index).get("Threshold"), 0.01);
		settingsDialog.addChoice​("Results returned", new String[] {"Best result", "All results"}, (String) settingsValues.get(index).get("Results returned"));
		settingsDialog.addChoice​("Mask retained", new String[] {"None", "1", "2", "3", "4"}, (String) settingsValues.get(index).get("Mask retained"));
		settingsDialog.addStringField​("Physical units", (String) settingsValues.get(index).get("Physical units"), 10);
		settingsDialog.addStringField​("Comments", (String) settingsValues.get(index).get("Comments"), 10);
		settingsDialog.addMessage("Segmentation classes:");
		for(int i = 0; i < numOfClasses; i++) {
			settingsDialog.addStringField("Class "+(i+1), classLabels[i], 15);
		}
		settingsDialog.showDialog();
		/** Extract the inputs and execute any change left, or cancel before that */
		if(settingsDialog.wasCanceled()) {return;}
		settingsValues.get(index).put("Background color", settingsDialog.getNextChoice());
		settingsValues.get(index).put("Output type", settingsDialog.getNextChoice());
		settingsValues.get(index).put("Threshold", settingsDialog.getNextNumber());
		settingsValues.get(index).put("Results returned", settingsDialog.getNextChoice());
		Object m_retained = settingsDialog.getNextChoice();
		settingsValues.get(index).put("Mask returned", m_retained);
		Object current_ratio = settingsDialog.getNextNumber();
		settingsValues.get(index).put("Ratio px/u", current_ratio);
		Object current_physical_units = settingsDialog.getNextString();
		settingsValues.get(index).put("Physical units", current_physical_units);
		Object current_comments = settingsDialog.getNextString();
		settingsValues.get(index).put("Comments", comments);
		if(resultsTable.get(index).get(0).size() > 2) {
			resultsTable.get(index).get(0).set(2, "Comments = " + current_comments);
		}
		for(int i = 0; i < numOfClasses; i++) {
			String new_name = settingsDialog.getNextString();
			classLabels[i] = new_name;
			addSegmentButton[i].setText("Add to " + classLabels[i]);
		}
		if (!((String) m_retained).equals("None")) {
				low_res_logits = null;
		}
		win.pack();
	}
	
	/**
	 * Analysis functions
	 */
	/** Custom analysis function */
	@Override
	public void customAnalysis() {
		/** Disable buttons until the analysis has finished */
		setComponentsEnabled(false);
		/** Check if points or segments are being used. 
			At least one locale is needed to continue */
		boolean localesEmpty = true;
		for(int i = 0; i < numOfClasses; i++) {
			if(!localesROIs[i].isEmpty()) {
				localesEmpty = false;
				break;
			}
		}
		boolean boxesEmpty = segmentsROIs[0].isEmpty();
		if (localesEmpty && boxesEmpty) {return;}
		/** Get parameters */
		String b_color = (String) settingsValues.get(index).get("Background color");
		String o_type = (String) settingsValues.get(index).get("Output type");
		String r_returned = (String) settingsValues.get(index).get("Results returned");
		Object m_retained = settingsValues.get(index).get("Mask retained");
		/** Preprocessing of the analyzedImage. First comes resizing, then it depends
			wheter the image is grayscale or RGB */ 
		int ogWidth = analyzedImage.getWidth();
		int ogHeight = analyzedImage.getHeight();
		float factor = 1024f/Math.max(ogWidth,ogHeight);
		int newWidth = (int) (ogWidth*factor + 0.5);
		int newHeight = (int) (ogHeight*factor + 0.5);
		ImageProcessor inputProcessor = analyzedImage.getProcessor().resize​(newWidth, newHeight, true);
		float[][][][] finalInputImage = null;
		if (inputProcessor.isGrayscale()) {
			float[][] processedInputImage = preprocess2dImage(inputProcessor);
			finalInputImage = new float[][][][]{{processedInputImage, processedInputImage, processedInputImage}};
		}
		else {
			ColorProcessor rgbProcessor = inputProcessor.convertToColorProcessor();
			float[][] processedR = preprocess2dImage((ImageProcessor) rgbProcessor.toFloat​(0, new FloatProcessor(newWidth, newHeight)));
			float[][] processedG = preprocess2dImage((ImageProcessor) rgbProcessor.toFloat​(1, new FloatProcessor(newWidth, newHeight)));
			float[][] processedB = preprocess2dImage((ImageProcessor) rgbProcessor.toFloat​(2, new FloatProcessor(newWidth, newHeight)));
			finalInputImage = new float[][][][]{{processedR, processedG, processedB}};
		}
    	/** Transformation into Onnx Tensor and run the Encoder model */
    	OrtSession.Result imageEmbedding = null;
		try {
			OnnxTensor x = OnnxTensor.createTensor(env, finalInputImage);
			Map<String, OnnxTensor> encoderInputs = new HashMap<>();
       		encoderInputs.put("x", x);
			imageEmbedding = encoder_session.run(encoderInputs);
			x.close();
		} catch (OrtException e) {
			IJ.log("" + e.getCode());
		}
		/** Locales into coordinates and labels (1 for positive examples, 0 for negative) */
		List<float[]> coordinatesList = new ArrayList();
		List<Float> labelsList = new ArrayList();
		if (!localesEmpty) {
			for (int i = 0; i < numOfTypes; i++) {
	            for (int j = 0; j < localesROIs[i].size(); j++){
	                for (Point containedPoint : localesROIs[i].get(j).getContainedPoints()) {
	                    coordinatesList.add(new float[] {(float)(int)(containedPoint.getY()*factor), (float)(int)(containedPoint.getX()*factor)});
	                    labelsList.add(1f - i);
	                }
	            }
	        }
		}
        /** Box segment into coordinate list */
        if (!boxesEmpty) {
	        for (int j = 0; j < segmentsROIs[0].size(); j++){
	        	float top_leftX = (float) segmentsROIs[0].get(j).getXBase();
	        	float top_leftY = (float) segmentsROIs[0].get(j).getYBase();
	        	float bottom_leftX = (float) (top_leftX + segmentsROIs[0].get(j).getFloatWidth());
	        	float bottom_leftY = (float) (top_leftY + segmentsROIs[0].get(j).getFloatHeight());
	        	coordinatesList.add(new float[] {(float)(int)(top_leftY*factor), (float)(int)(top_leftX*factor)});
	        	labelsList.add(2f);
	        	coordinatesList.add(new float[] {(float)(int)(bottom_leftY*factor), (float)(int)(bottom_leftX*factor)});
	        	labelsList.add(3f);
	        }
        } else {
        	coordinatesList.add(new float[] {0f, 0f});
        	labelsList.add(-1f);
        }
        /** List into array */
        int n_points = coordinatesList.size();
        float[][][] coordinatesArray = new float[1][n_points][2];
        float[][] labelsArray = new float[1][n_points];
        for (int i = 0; i < n_points; i++) {
        	coordinatesArray[0][i] = coordinatesList.get(i);
        	labelsArray[0][i] = labelsList.get(i);
        }
        /** Mask segment into mask array */
        float[][][][] maskArray = new float[1][1][256][256];
		float[] maskLabelArray = {0f};
		if (low_res_logits != null) {
			maskLabelArray[0] = 1f;
			maskArray[0][0] = low_res_logits;   
		}
		else if (!segmentsROIs[1].isEmpty()) {
			maskLabelArray[0] = 1f;
			for (Point containedPoint : segmentsROIs[1].get(0).getContainedPoints()) {
				int x = (int)(containedPoint.getX()/4) - 1;
				int y = (int)(containedPoint.getY()/4) - 1;
	        	maskArray[0][0][x][y] = 30f;       
	        }
	        imagePlusFromFloatArray(maskArray);
		}
		/** Transformation into Onnx Tensor and run the Decoder model */
		OrtSession.Result results = null;
		float[][][][] outputs = null;
		float[] predictions = null;
		try {
			OnnxTensor embeddings = OnnxTensor.createTensor(env, imageEmbedding.get(0).getValue());
			OnnxTensor coordinates = OnnxTensor.createTensor(env, coordinatesArray);
			OnnxTensor labels = OnnxTensor.createTensor(env, labelsArray);
			OnnxTensor emptyMask = OnnxTensor.createTensor(env, maskArray);
			OnnxTensor noMaskLabel = OnnxTensor.createTensor(env, maskLabelArray);
			OnnxTensor ogSize = OnnxTensor.createTensor(env, new float[] {ogWidth, ogHeight});
			Map<String, OnnxTensor> decoderInputs = new HashMap<>();
			decoderInputs.put("image_embeddings", embeddings);
			decoderInputs.put("point_coords", coordinates);
			decoderInputs.put("point_labels", labels);
			decoderInputs.put("mask_input", emptyMask);
			decoderInputs.put("has_mask_input", noMaskLabel);
			decoderInputs.put("orig_im_size", ogSize);
			//IJ.log("" + decoder_session.getOutputInfo());
			results = decoder_session.run(decoderInputs);
			outputs = (float[][][][]) results.get(0).getValue();
			predictions = ((float[][]) results.get(1).getValue())[0];
			if (!((String) m_retained).equals("None")) {
				low_res_logits = ((float[][][][]) results.get(2).getValue())[0][(int) m_retained - 1];
			}
			imageEmbedding.close();
			embeddings.close();
			coordinates.close();
			labels.close();
			emptyMask.close();
			noMaskLabel.close();
			ogSize.close();
		} catch (OrtException e) {
			IJ.log("" + e.getCode());
		}
		/** Return an imagePlus from the results. 
			These have a prediction score for automatically selecting the best */
		int bestPredictionIndex = 0;
		int n_pred = predictions.length;
		for (int i = 1; i < n_pred; i++) {
	        if (predictions[i] > predictions[bestPredictionIndex]) {
	            bestPredictionIndex = i;
	        }
	    }
	    int colorFactor = 0;
		if (b_color == "Black") {colorFactor = -1;}
		else if (b_color == "White") {colorFactor = 1;}
		if (r_returned == "Best result") {
			ImagePlus image = outputToImagePlus(outputs[0][bestPredictionIndex], o_type, colorFactor);
			image.show();
		}
		else if (r_returned == "All results") {
			ImagePlus[] images = new ImagePlus[n_pred];
			for (int i = 0; i < n_pred; i++) {
				images[i] = outputToImagePlus(outputs[0][i], o_type, colorFactor);
			}
			ImageStack stack = ImageStack.create​(images);
			ImagePlus image = new ImagePlus("Outputs", stack);
			image.show();
		}
		/** Free resources by restarting session */
		setComponentsEnabled(true);
		try {
			results.close();
			encoder_session.close();
			decoder_session.close();
			encoder_session = env.createSession(path_to_encoder, new OrtSession.SessionOptions());
			decoder_session = env.createSession(path_to_decoder, new OrtSession.SessionOptions());
		
		} catch (OrtException e) {
			IJ.log("" + e.getCode());
		}
	}
	
	public float[][] preprocess2dImage(ImageProcessor inputProcessor) {
		float[][] inputImage = inputProcessor.getFloatArray();
		int newWidth = inputImage.length;
		int newHeight = inputImage[0].length;
		/** Standardize */
		float sum = 0;
    	for (float[] i:inputImage) {
        	for (float value:i) {
        		sum += value;
    		}
    	}
    	float mean = sum/(newWidth*newHeight);
    	float variance = 0;
    	for (float[] i:inputImage) {
        	for (float value:i) {
        		variance += (value-mean)*(value-mean);
    		}
    	}
    	float std = (float) Math.sqrt(variance/(newWidth*newHeight));
		for (int i = 0; i < newWidth; i++) {
		    for (int j = 0; j < newHeight; j++) {
		        inputImage[i][j] = (inputImage[i][j] - mean) / std;
		    }
		}
		/** Pad */
		float[][] paddedInputImage = new float[1024][1024];
		for (int i = 0; i < newWidth; i++) {
        	for (int j = 0; j < newHeight; j++) {
        		paddedInputImage[i][j] = inputImage[i][j];
    		}
    	}
    	return paddedInputImage;
	}
	public ImagePlus outputToImagePlus(float[][] output, String o_type, int colorFactor) {
		ImagePlus image = new ImagePlus("pre",  new FloatProcessor​​(output));
		ImageProcessor preImage = image.getProcessor();
		FloatProcessor postImage = new FloatProcessor(preImage.getWidth(),preImage.getHeight());
		if (o_type == "Probability Map") {
			postImage = generateProbabilityMap(preImage, postImage, colorFactor);
		}
		else if (o_type == "Mask") {
			postImage = generateMask(preImage, postImage, colorFactor);
		}
		ImagePlus outputImage = new ImagePlus​("post", postImage);
		return outputImage;
	}
	public FloatProcessor generateProbabilityMap(ImageProcessor pre, FloatProcessor post, int colorFactor) {
		for(int x = 0; x < pre.getWidth(); x++){
            for(int y = 0; y < pre.getHeight(); y++){
            	double value = 1/(1 + Math.exp(colorFactor * pre.getPixelValue(x, y)));
            	post.putPixelValue(x, y, value);
            }
        }
        return post;
	}
	public FloatProcessor generateMask(ImageProcessor pre, FloatProcessor post, int colorFactor) {
		double th = (double) settingsValues.get(index).get("Threshold");
		for(int x = 0; x < pre.getWidth(); x++){
            for(int y = 0; y < pre.getHeight(); y++){
            	double value = 1/(1 + Math.exp(colorFactor * pre.getPixelValue(x, y)));
            	if (value >= th) {value = 255;} 
            	else {value = 0;}
            	post.putPixelValue(x, y, value);
            }
        }
        return post;
	}
	
	// Mantener presente
	public void imagePlusFromFloatArray (float[][][][] floatArray){
		//Flatten float array and transform to int
		int totalElements = floatArray.length * floatArray[0].length * floatArray[0][0].length * floatArray[0][0][0].length;
		int[] data = new int[totalElements];
		int index = 0;
		for (int i = 0; i < floatArray[0][0][0].length; i++) {
	        for (int j = 0; j < floatArray[0][0].length; j++) {
	            for (int k = 0; k < floatArray[0].length; k++) {
	                data[index++] = (int) floatArray[0][k][j][i];
	            }
	        }
	    }
		//Construct ImagePlus
		//BufferedImage img = new BufferedImage(floatArray[0][0].length, floatArray[0][0][0].length, BufferedImage.TYPE_INT_RGB);
        BufferedImage img = new BufferedImage(floatArray[0][0].length, floatArray[0][0][0].length, BufferedImage.TYPE_USHORT_GRAY);
        WritableRaster raster = img.getRaster();
        raster.setPixels(raster.getMinX(), raster.getMinY(), raster.getWidth(), raster.getHeight(), data);
        ImagePlus image = new ImagePlus​("", img);
        image.show();
		return;
	}
	
	/** Custom preview function */
	@Override
	public void show_hide_preview() {
		if (!showPreview) {
			/** Insert operation necessary for the analysis that you wish to preview */
			showPreview = true;
		} else {
			/** Insert way to hide it */
			showPreview = false;
		}
	}
}
