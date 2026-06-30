import sc.fiji.segmentationFramework.*;
import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.process.ImageProcessor;
import ij.process.ColorProcessor;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Pixel_Intensity_Measurer extends SegmentationFramework {
	
	/**
	 * New global variables
	 */
	/** Processor used for previsualizing and analyzing the thresholds applyied */
	public ColorProcessor thresholdProcessor;
	/***/
	public boolean showPreview = false; //Recordar eliminar
	/** Default values for parameters in settings */
	public double ratio = 1.0;
	public String physical_units = "pixels"; 
	public String comments = "No comments";
	
	/**
	 * Change of predetermined global variables and initialization of complex new ones
	 */
	public Pixel_Intensity_Measurer() {
		/** Predetermined variables */
		super(false);
		pluginTitle = "Pixel Intensity Measurer";
		numOfClasses = 2;
		nameOfStatistics = new String[]{"Mean", "SD", "N"};
		//previewButton.setToolTipText("Preview/Hide image after applying the thresholds");
		/** New variables */
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
			settingsValues.add(newImageSettings());
		}
		/** Create the table for the results */
		resultsTable = newResultsTable();
		/** Open the first image or return if the user canceled the dialog */
		analyzedImage = IJ.openImage(path + pathnames[index]);
		if(null == analyzedImage) {return;} 
		/** Create the processor for the analysis and the display images */
		displayImage = analyzedImage.duplicate();
		IJ.run(analyzedImage, "8-bit", "true");
		setSliders();
		updateSliderLabels();
		setUpFromMetadata();
		/** Create a toolbar for working with the display, selecting the most convenient tool */
		ij.gui.Toolbar.getInstance().setTool(ij.gui.Toolbar.RECTANGLE);
		/** Build GUI */
		win = new CustomWindow(displayImage);
		win.pack();
		win.fixZoom();
	}
	
	/**
	 * Settings functions (example)
	 */
	/** Display the settings */
	@Override
	public void showSettingsDialog() {
		/** Creates the dialog */
		GenericDialogPlus settingsDialog = new GenericDialogPlus("Settings");
		settingsDialog.addMessage("Analysis parameters:");
		settingsDialog.addSlider("Minimum Pixel Value", 0, 255, (int) settingsValues.get(index).get("Min Value"));
		settingsDialog.addSlider("Maximum Pixel Value", 0, 255, (int) settingsValues.get(index).get("Max Value"));
		settingsDialog.addSlider("Minimum Red Value", 0, 255, (int) settingsValues.get(index).get("Min R Value"));
		settingsDialog.addSlider("Maximum Red Value", 0, 255, (int) settingsValues.get(index).get("Max R Value"));
		settingsDialog.addSlider("Minimum Green Value", 0, 255, (int) settingsValues.get(index).get("Min G Value"));
		settingsDialog.addSlider("Maximum Green Value", 0, 255, (int) settingsValues.get(index).get("Max G Value"));
		settingsDialog.addSlider("Minimum Blue Value", 0, 255, (int) settingsValues.get(index).get("Min B Value"));
		settingsDialog.addSlider("Maximum Blue Value", 0, 255, (int) settingsValues.get(index).get("Max B Value"));
		settingsDialog.addNumericField("Ratio pixel/physical unit", (double) settingsValues.get(index).get("Ratio px/u"));
		settingsDialog.addStringField​("Physical units", (String) settingsValues.get(index).get("Physical units"), 20);
		settingsDialog.addStringField​("Comments", (String) settingsValues.get(index).get("Comments"), 20);
		settingsDialog.addMessage("Segmentation classes:");
		for(int i = 0; i < numOfClasses; i++) {
			settingsDialog.addStringField("Class "+(i+1), classLabels[i], 15);
		}
		settingsDialog.showDialog();
		/** Extract the inputs and update the parameters, or cancel before that */
		if(settingsDialog.wasCanceled()) {return;}
		setComponentsEnabled(false);
		int min_Px = (int) settingsDialog.getNextNumber();
		int max_Px = (int) settingsDialog.getNextNumber();
		int min_R = (int) settingsDialog.getNextNumber();
		int max_R = (int) settingsDialog.getNextNumber();
		int min_G = (int) settingsDialog.getNextNumber();
		int max_G = (int) settingsDialog.getNextNumber();
		int min_B = (int) settingsDialog.getNextNumber();
		int max_B = (int) settingsDialog.getNextNumber();
		if(min_R != (int) settingsValues.get(index).get("Min R Value") | 
			max_R != (int) settingsValues.get(index).get("Max R Value") | 
			min_G != (int) settingsValues.get(index).get("Min G Value") |
			max_G != (int) settingsValues.get(index).get("Max G Value") | 
			min_B != (int) settingsValues.get(index).get("Min B Value") | 
			max_B != (int) settingsValues.get(index).get("Max B Value")) {
			if(thresholdProcessor.isGrayscale()) {
				IJ.showMessage(pluginTitle, "Can't use RGB thresholds, image is grayscale");
			} else {
				settingsValues.get(index).put("Min R Value", min_R);
				settingsValues.get(index).put("Max R Value", max_R);
				settingsValues.get(index).put("Min G Value", min_G);
				settingsValues.get(index).put("Max G Value", max_G);
				settingsValues.get(index).put("Min B Value", min_B);
				settingsValues.get(index).put("Max B Value", max_B);
				if(min_Px != (int) settingsValues.get(index).get("Min Value") |
					max_Px != (int) settingsValues.get(index).get("Max Value")) {
					IJ.showMessage(pluginTitle, "Min and Max pixel values take precedence over RGB limits");
				} else {
					updateThresholdProcessor();
					displayImage.setProcessor(pluginTitle, thresholdProcessor.duplicate());
					showPreview = true;
				}
			}
		}
		if(min_Px != (int) settingsValues.get(index).get("Min Value") |
			max_Px != (int) settingsValues.get(index).get("Max Value")) {
			settingsValues.get(index).put("Min Value", min_Px);
			settingsValues.get(index).put("Max Value", max_Px);	
			updateThresholdProcessor();
			displayImage.setProcessor(pluginTitle, thresholdProcessor.duplicate());
			showPreview = true;
		}
		settingsValues.get(index).put("Ratio px/u", settingsDialog.getNextNumber());
		settingsValues.get(index).put("Physical units", settingsDialog.getNextString());
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
		win.pack();
		setComponentsEnabled(true);
	}
	
	/** Create a list of settings with default values */
	@Override
	public Map<String, Object> newImageSettings() {
		Map<String, Object> defaultSettings = new HashMap();
		defaultSettings.put("Min Value", 0);
		defaultSettings.put("Max Value", 255);
		defaultSettings.put("Min R Value", 0);
		defaultSettings.put("Max R Value", 255);
		defaultSettings.put("Min G Value", 0);
		defaultSettings.put("Max G Value", 255);
		defaultSettings.put("Min B Value", 0);
		defaultSettings.put("Max B Value", 255);
		defaultSettings.put("Ratio px/u", ratio);
		defaultSettings.put("Physical units", physical_units);
		defaultSettings.put("Comments", comments);
		return defaultSettings;
	}
	
	public void updateThresholdProcessor() {
		int min_R = (int) settingsValues.get(index).get("Min R Value");
		int max_R = (int) settingsValues.get(index).get("Max R Value");
		int min_G = (int) settingsValues.get(index).get("Min G Value");
		int max_G = (int) settingsValues.get(index).get("Max G Value");
		int min_B = (int) settingsValues.get(index).get("Min B Value");
		int max_B = (int) settingsValues.get(index).get("Max B Value");
		thresholdProcessor = analyzedImage.getProcessor().duplicate().convertToColorProcessor();
		if(thresholdProcessor.isGrayscale()) {
			for(int x = 0; x < thresholdProcessor.getWidth(); x++) {
				for(int y = 0; y < thresholdProcessor.getHeight(); y++) {
					int old_pixel_value = (int) thresholdProcessor.getPixelValue(x, y);
					double new_pixel_value = (double) zeroOutliers(old_pixel_value, 0, 255);
					thresholdProcessor.putPixelValue​(x, y, new_pixel_value);
				}
			}
		} else {
			for(int x = 0; x < thresholdProcessor.getWidth(); x++) {
				for(int y = 0; y < thresholdProcessor.getHeight(); y++) {
					int[] old_pixel_value = thresholdProcessor.getPixel(x, y, new int[3]);
					int[] new_pixel_value = new int[3];
					new_pixel_value[0] = zeroOutliers(old_pixel_value[0], min_R, max_R);
					new_pixel_value[1] = zeroOutliers(old_pixel_value[1], min_G, max_G);
					new_pixel_value[2] = zeroOutliers(old_pixel_value[2], min_B, max_B);
					thresholdProcessor.putPixel​(x, y, new_pixel_value);
				}
			}
		}
	}
	
	public int zeroOutliers(int val, int min_val, int max_val) {
		int min_Px = (int) settingsValues.get(index).get("Min Value");
		int max_Px = (int) settingsValues.get(index).get("Max Value");
		if(val < min_val | val < min_Px) {val = 0;}
		else if(val > max_val | val > max_Px) {val = 0;}
		return val;
	}
	
	/**
	 * Analysis functions
	 */
	@Override
	public void customAnalysis() {
		/** There needs to be at least one segment */
		boolean segmentsEmpty = true;
		for(int i = 0; i < numOfClasses; i++) {
			if(!segmentsROIs[i].isEmpty()) {
				segmentsEmpty = false;
				break;
			}
		}
		if (segmentsEmpty == true) {addSegments(0);}
		/** Disable buttons until the analysis has finished */
		setComponentsEnabled(false);
		/** Clean the results table */
		resultsTable.set(index, newImageSection(pathnames[index]));
		/** Get an image for each segment and count the particles */
		for(int l = 0; l < numOfClasses; l++) {
			/** Trackers for calculating the total values for the class */
			int totalN = 0;
			double totalSum = 0;
			/** Iterate over each segment */
			for(int j=0; j<segmentsROIs[l].size(); j++) {
				Roi r = segmentsROIs[l].get(j);
				/** Get original coordinates */
				double ogx = r.getBounds().getX();
				double ogy = r.getBounds().getY();
				/** Make a temporal grayscale duplicate of the image */
				ImagePlus segmentToAnalyze = analyzedImage.duplicate();
				ImageProcessor ip = segmentToAnalyze.getProcessor();
				/** Select the roi */
				ip.setRoi(r);
				/** Blacken the outside of the roi */
				ip.setColor(0);
				ip.fillOutside(r);
				/** Crop the rest of the image*/
				segmentToAnalyze = new ImagePlus("", ip.crop());
				/** Calculate mean */
				double sum = 0;
				int N = 0;
				for(int x = 0; x < segmentToAnalyze.getWidth(); x++) {
					for(int y = 0; y < segmentToAnalyze.getHeight(); y++) {
						/** Consider only the pixels inside the roi */
						if (r.containsPoint(ogx + x, ogy + y)) {
							double value = segmentToAnalyze.getPixel(x, y)[0];
							sum = sum + value;
							N = N + 1;
						}
					} 
				}
				double mean = sum/N;
				totalN = totalN + N;
				totalSum = totalSum + sum;
				/** Calculate standar deviation */
				double SD = 0;
				for(int x = 0; x < segmentToAnalyze.getWidth(); x++) {
					for(int y = 0; y < segmentToAnalyze.getHeight(); y++) {
						if (r.containsPoint(ogx + x, ogy + y)) {
							double value = segmentToAnalyze.getPixel(x, y)[0];
							double dist = mean - value;
							SD = SD + dist*dist;
						}
					} 
				}
				SD = Math.sqrt(SD/(N-1));
				/** Add the count to the results table */
				addResultToTable(l, j, new Object[]{mean, SD, N});
			}
			/** Calculate the total values, this will require another pass over the segments
				for the standard deviation */
			double totalMean = totalSum/totalN;
			double totalSD = 0;
			for(int j=0; j<segmentsROIs[l].size(); j++) {
				Roi r = segmentsROIs[l].get(j);
				/** Get original coordinates for preview */
				double ogx = r.getBounds().getX();
				double ogy = r.getBounds().getY();
				/** Make a temporal grayscale duplicate of the image */
				ImagePlus segmentToAnalyze = analyzedImage.duplicate();
				ImageProcessor ip = segmentToAnalyze.getProcessor();
				/** Select the roi */
				ip.setRoi(r);
				/** Blacken the outside of the roi in the image */
				ip.setColor(0);
				ip.fillOutside(r);
				/** Count, croping the rest of the image */
				segmentToAnalyze = new ImagePlus("", ip.crop());
				/** Calculate standar deviation */
				for(int x = 0; x < segmentToAnalyze.getWidth(); x++) {
					for(int y = 0; y < segmentToAnalyze.getHeight(); y++) {
						if (r.containsPoint(ogx + x, ogy + y)) {
							double value = segmentToAnalyze.getPixel(x, y)[0];
							double dist = totalMean - value;
							totalSD = totalSD + dist*dist;
						}
					} 
				}
			}
			totalSD = Math.sqrt(totalSD/(totalN-1));
			/** Update total count */
			updateTotalCount(l, new Object[]{totalMean, totalSD, totalN});
		}
		setComponentsEnabled(true);
	}
	@Override
	public void show_hide_preview() {
		if (!showPreview) {
			displayImage.setProcessor(pluginTitle, thresholdProcessor.duplicate());
			showPreview = true;
		} else {
			displayImage.setProcessor(pluginTitle, analyzedImage.getProcessor().duplicate());
			showPreview = false;
		}
	}
}
