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
//Borrar quizás
import java.awt.Point;
import ij.gui.Overlay;
import ij.gui.PointRoi; 
import java.awt.Color;
import ij.io.RoiDecoder;
import ij.io.RoiEncoder;
import ij.io.OpenDialog;
import ij.io.SaveDialog;
import org.json.JSONObject;
import org.json.JSONArray;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

public class Particle_Counter extends SegmentationFramework {
	
	/**
	 * New global variables
	 */
	/** Flag for the state of show_hide_preview() function */
	public boolean showPreview = false;
	/** Default values for parameters in settings */
	private String particle_type = "Black";
	private double diameter = 3;
	private double stride = 2;
	private double perimeter_th = 100;
	private double center_th = 15;
	private double min_distance = 3.0;
	private double th_correction = 5.0;
	private double ratio = 1.0;
	private String physical_units = "pixels"; 
	private String comments = ""; 
	/** List that store the total set of all particle coordinates, which are either
		local minima or maxima */
	private List<List<List<Integer>>> totalExtremaCoords = new ArrayList();
	
	/**
	 * Change of predetermined global variables and initialization of complex new ones
	 */
	public Particle_Counter() {
		/** Predetermined variables */
		super(true);
		pluginTitle = "Particle Counter";
		numOfClasses = 3;
		//MAX_NUM_TYPES = 2;
		numOfTypes = 2;
		typeLabels[0] = "Added Particles";
		typeLabels[1] = "Substracted Particles";
		nameOfStatistics = new String[]{"Number of particles", "Area (unit^2)"};
		//previewButton.setToolTipText("Description of the custom aspect of the analysis being previewed");
		numOfStatistics = nameOfStatistics.length;
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
			if (useLocales) {
				totalLocalesROIs.add(null);
				totalLocalesNames.add(null);
			}
			settingsValues.add(newImageSettings());
			totalExtremaCoords.add(null);
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
		/** Find particles and preview */
		localizeExtrema();
	}
	
	
	/**
	 * Settings functions
	 */
	/** Create a list of settings with default values */
	@Override
	public Map<String, Object> newImageSettings() {
		Map<String, Object> defaultSettings = new HashMap();
		defaultSettings.put("Type of particles", particle_type);
		defaultSettings.put("Particle diameter", diameter);
		defaultSettings.put("Search stride", stride);
		defaultSettings.put("Perimeter threshold", perimeter_th);
		defaultSettings.put("Center threshold", center_th);
		defaultSettings.put("Threshold correction", th_correction);
		defaultSettings.put("Minimum distance", min_distance);
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
		settingsDialog.addChoice​("Type of particle", new String[] {"Black", "White"}, (String) settingsValues.get(index).get("Type of particles"));
		settingsDialog.addNumericField("Particle diameter", (double) settingsValues.get(index).get("Particle diameter"));
		settingsDialog.addNumericField("Search stride", (double) settingsValues.get(index).get("Search stride"));
		settingsDialog.addNumericField("Perimeter threshold", (double) settingsValues.get(index).get("Perimeter threshold"));
		settingsDialog.addNumericField("Center threshold", (double) settingsValues.get(index).get("Center threshold"));
		settingsDialog.addNumericField("Threshold correction", (double) settingsValues.get(index).get("Threshold correction"));
		settingsDialog.addNumericField("Minimum distance", (double) settingsValues.get(index).get("Minimum distance"));
		settingsDialog.addNumericField("Ratio pixel/physical unit", (double) settingsValues.get(index).get("Ratio px/u"));
		settingsDialog.addStringField​("Physical units", (String) settingsValues.get(index).get("Physical units"), 10);
		settingsDialog.addStringField​("Comments", (String) settingsValues.get(index).get("Comments"), 10);
		settingsDialog.addMessage("Segmentation classes:");
		for(int i = 0; i < numOfClasses; i++) {
			settingsDialog.addStringField("Class "+(i+1), classLabels[i], 15);
		}
		settingsDialog.showDialog();
		/** Extract the inputs and execute any change left, or cancel before that */
		if(settingsDialog.wasCanceled()) {return;}
		settingsValues.get(index).put("Type of particle", settingsDialog.getNextChoice());
		settingsValues.get(index).put("Particle diameter", settingsDialog.getNextNumber());
		settingsValues.get(index).put("Search stride", settingsDialog.getNextNumber());
		settingsValues.get(index).put("Perimeter threshold", settingsDialog.getNextNumber());
		settingsValues.get(index).put("Center threshold", settingsDialog.getNextNumber());
		settingsValues.get(index).put("Threshold correction", settingsDialog.getNextNumber()); 
		settingsValues.get(index).put("Minimum distance ", settingsDialog.getNextNumber());
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
		localizeExtrema();
		win.pack();
	}
	
	/**
	 * Analysis functions
	 */
	/** Custom analysis function */
	@Override
	public void customAnalysis() {
		/** There needs to be at least one segment */
		boolean segmentsEmpty = true;
		for(int i = 0; i < numOfClasses; i++)
			if(!segmentsROIs[i].isEmpty()) {
				segmentsEmpty = false;
				break;
			}
		if (segmentsEmpty == true) {return;}
		/** Disable buttons until the analysis has finished */
		setComponentsEnabled(false);
		/** Clean the results table */
		resultsTable.set(index, newImageSection(pathnames[index]));
		/** Get an image for each segment and count the particles */
		for(int l = 0; l < numOfClasses; l++) {
			/** Iterate over each segment */
			/** Create lists to calculate the total values for the class */
			int totalCounts = 0;
			double totalArea = 0;
			for(int j=0; j<segmentsROIs[l].size(); j++) {
				Roi r = segmentsROIs[l].get(j);
				/** Get original coordinates */
				int ogx = (int) r.getBounds().getX();
				int ogy = (int) r.getBounds().getY();
				/** Make a temporal duplicate of the image */
				ImagePlus segmentToAnalyze = analyzedImage.duplicate();
				ImageProcessor ip = segmentToAnalyze.getProcessor();
				/** Select the roi */
				ip.setRoi(r);
				/** Crop the rest of the image*/
				segmentToAnalyze = new ImagePlus("", ip.crop());
				/** Count the number of particles by checking if 
					any pixel from the roi is in the totalExtremaList of the image.
					Do the same for the manually added and substracted points,
					discounting in the later.
					At the same time, calculate area of the segment by counting the number of pixels 
					and transform it to the correct unit */
				int ct = 0;
				double area = 0;
				for(int x = 0; x < segmentToAnalyze.getWidth(); x++) {
					for(int y = 0; y < segmentToAnalyze.getHeight(); y++) {
						if (r.containsPoint(ogx + x, ogy + y)) {
							area = area + 1;
						}
						List<Integer> coords = new ArrayList(); 
						coords.add(ogx + x); 
						coords.add(ogy + y); 
						/** Automatically detected particles */
						if (totalExtremaCoords.get(index).contains(coords)) {
							ct = ct + 1;
						}
						/** Manually added */
						for (int n = 0; n < localesROIs[0].size(); n++) {
							if (ArrayUtils.contains(localesROIs[0].get(n).getContainedPoints(), new Point(ogx + x, ogy + y))) {
								ct = ct + 1;
							}
						}
						/** Manually substracted */
						for (int n = 0; n < localesROIs[1].size(); n++) {
							if (ArrayUtils.contains(localesROIs[1].get(n).getContainedPoints(), new Point(ogx + x, ogy + y))) {
								ct = ct - 1;
							}
						}
					} 
				}
				/** Convert N to the correct scale */
				area = area * ratio*ratio;
				/** Update the total variables */
				totalCounts += ct;
				totalArea += area;
				/** Add to the results table */
				addResultToTable(l, j, new Object[]{ct, area});
			}
			/** Add total variables to the results table */
			updateTotalCount(l, new Object[]{totalCounts, totalArea});
		}
		setComponentsEnabled(true);
	}
	/** Get the extrema points of the imaget*/
	public void localizeExtrema() {
		/** Get parameters */
		String p_type = (String) settingsValues.get(index).get("Type of particles");
		int p_diameter = (int)(double) settingsValues.get(index).get("Particle diameter");
		int strid = (int)(double) settingsValues.get(index).get("Search stride");
		double p_th = (double) settingsValues.get(index).get("Perimeter threshold");
		double c_th = (double) settingsValues.get(index).get("Center threshold");
		double correction = (double) settingsValues.get(index).get("Threshold correction");
		int min_dist = (int)(double) settingsValues.get(index).get("Minimum distance");
		/** Prepare used variables */
		int width = analyzedImage.getWidth();
		int height = analyzedImage.getHeight();
		List<List<Integer>> extremaCoords = new ArrayList();
		/** Instantiate the kernel */
		int[][] kernel = new int[p_diameter+2][p_diameter+2];
		/** Iterate over the image */
		for(int x = 0; x < width; x = x + strid){
			for(int y = 0; y < height; y = y + strid){
				/**  Get the values for each position in the kernel, 
					 and the key stats for filtering.
					 Each h is the sum of values of one side of the kernel,
					 cmean is the average in the center,
					 bmean is the average of the borders  */
				double h1 = 0; double h2 = 0; double h3 = 0; double h4 = 0;
				double cmean = 0; double bmean = 0;
				for(int i = 0; i < p_diameter+2; i++) {
					for(int j = 0; j < p_diameter+2; j++) {
						kernel[i][j] = gV(width, height, x+i, y+j);
						int val = kernel[i][j];
						if ((i > 0) && (i < p_diameter+1) && (j > 0) && (j < p_diameter+1)){
							cmean = cmean + val;
						} else { 
							if (i == 0) {h1 = h1 + val; bmean = bmean + val;}
							if (j == 0) {h2 = h2 + val; bmean = bmean + val;}
							if (i == p_diameter+1) {h3 = h3 + val; bmean = bmean + val;}
							if (j == p_diameter+1) {h4 = h4 + val; bmean = bmean + val;}
						}
					}
				}
				h1 = h1/(p_diameter+2); 
				h2 = h2/(p_diameter+2); 
				h3 = h3/(p_diameter+2); 
				h4 = h4/(p_diameter+2);
				cmean = cmean/(p_diameter*p_diameter);
				bmean = bmean/((p_diameter+2)*4 - 4);
				/** Filter cuadrants with too much diference between borders */
				if ((Math.abs(h1 - h2) < p_th) && (Math.abs(h3 - h4) < p_th)){
					/** If the center is darker than the perimeter, by a certain
					    threshold, then count the minima.
						The threshold is corrected for darker and brighter perimeters,
						where black particles would have less difference 
						or gray particles would have enoguh.
						Does the opposite for white particles */
					if (p_type == "Black" && ((bmean - cmean) > (c_th + c_th*correction*(bmean)/255.0))
					||  p_type == "White" && ((cmean - bmean) > (c_th + c_th*correction*(255-bmean)/255.0))) {
						int[] extCor = new int[2];
						if (p_type == "Black") {extCor = getMinimaCor(x, y, kernel);}
						else if (p_type == "White") {extCor = getMaximaCor(x, y, kernel);}
						/** Filter the minimas that are too close */
						if (checkRepeatedExtrema(extCor, extremaCoords)) {
							List<Integer> l_extCor= new ArrayList(2);
							l_extCor.add(extCor[0]);
							l_extCor.add(extCor[1]);
							extremaCoords.add(l_extCor);
							/** Skip some coordinates */
							y = y + min_dist;
						}
					}
				}
			}
		}
		totalExtremaCoords.set(index, extremaCoords);
		/** Show all the previews */
		if(!showPreview){show_hide_preview();}
		else {drawLocales(null);}
		return;
	}
	/** Get pixel value but repeat corner values for outer coordinates */
	public int gV(int width, int height, int x, int y) {
		return analyzedImage.getPixel(Math.min(x, width-1), Math.min(y, height-1))[0];
	}
	/** Check if a Extrema is too close to one stored already */ //Optimizable
	public boolean checkRepeatedExtrema(int[] newExtCords, List<List<Integer>> oldExtCords) {
		int newX = newExtCords[0];
		int newY = newExtCords[1];
		int min_dist = (int)(double) settingsValues.get(index).get("Minimum distance");
		for (int i = 0; i < oldExtCords.size(); i++) {
			int oldX = oldExtCords.get(i).get(0);
			int oldY = oldExtCords.get(i).get(1);
			if (Math.sqrt((newX - oldX)*(newX - oldX) + (newY - oldY)*(newY - oldY)) < min_dist) {
				return false;
			}
		}
		return true;
	}
	/** Get the coordinates with the minimum or maximum value in a kernel's center */
	public int[] getMinimaCor(int x, int y, int[][] kernel){
		int minVal = 255;
		int[] minCor = new int[2];
		for(int i = 1; i < kernel.length-1; i++){
			for(int j = 1; j < kernel.length-1; j++){
				if (kernel[i][j] < minVal) {
					minVal = kernel[i][j];
					minCor[0] = x+i;
					minCor[1] = y+j;
				}
			}
		}
		return minCor;
	}
	public int[] getMaximaCor(int x, int y, int[][] kernel){
		int maxVal = 0;
		int[] maxCor = new int[2];
		for(int i = 1; i < kernel.length-1; i++){
			for(int j = 1; j < kernel.length-1; j++){
				if (kernel[i][j] > maxVal) {
					maxVal = kernel[i][j];
					maxCor[0] = x+i;
					maxCor[1] = y+j;
				}
			}
		}
		return maxCor;
	}
	
	/** Custom processes used when changing images */
	@Override
	public void applicationSpecificChanges() {
		/** Find particles and preview */
		localizeExtrema();
	}
	
	/** Custom preview function */
	@Override
	public void show_hide_preview() {
		if (!showPreview) {
			/** Insert operation necessary for the analysis that you wish to preview */
			showPreview = true;
			drawLocales(null);
		} else {
			/** Insert way to hide it */
			showPreview = false;
			displayImage.setOverlay​(new Overlay());
		}
	}
	/** Draw the stored locales on the display image. Modified to include the automatically
		identified particles from the preview */
	@Override
    public void drawLocales(PointRoi selectedLocale) {
        localesOverlay = new Overlay();
        displayImage.setOverlay​(localesOverlay);
        if (selectedLocale != null) {
            selectedLocale.setStrokeColor​(Color.white);
            selectedLocale.setStrokeWidth​(1.);
            selectedLocale.setFillColor​(Color.white);
            localesOverlay.add(selectedLocale);
        }
        PointRoi roiPreviews = new PointRoi();
		for(int i = 0; i < totalExtremaCoords.get(index).size(); i++){
			roiPreviews.addPoint((double)totalExtremaCoords.get(index).get(i).get(0), (double)totalExtremaCoords.get(index).get(i).get(1));
		}
		roiPreviews.setStrokeColor​(Color.yellow);
		roiPreviews.setStrokeWidth​(1.);
		localesOverlay.add(roiPreviews);
        localesOverlay.setDraggable(false); 
        /** Give visual priority to the manual locales over the automatic */
        for (int i = 0; i < numOfTypes; i++) {
            PointRoi rois = new PointRoi();
            for (int j = 0; j < localesROIs[i].size(); j++){
                for (Point containedPoint : localesROIs[i].get(j).getContainedPoints()) {
                    rois.addPoint(containedPoint.getX(), containedPoint.getY());
                }
            }
            rois.setStrokeColor​(localesColors[i]);
            rois.setStrokeWidth​(1.);
            rois.setFillColor​(localesColors[i]);
            localesOverlay.add(rois);
        }
        displayImage.setOverlay​(localesOverlay);
    }

}
