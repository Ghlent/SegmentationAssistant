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

public class Template extends SegmentationFramework {
	
	/**
	 * New global variables
	 */
	/** Flag for the state of show_hide_preview() function */
	public boolean showPreview = false;
	/** Default values for parameters in settings */
	public double ratio = 1.0;
	public String physical_units = "pixels"; 
	public String comments = "No comments"; 
	
	/**
	 * Change of predetermined global variables and initialization of complex new ones
	 */
	public Template() {
		/** Predetermined variables */
		super();
		pluginTitle = "Name";
		numOfClasses = 2;
		nameOfStatistics = new String[]{"custom_variable1", "custom_variable2"};
		previewButton.setToolTipText("Description of the custom aspect of the analysis being previewed");
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
			settingsValues.add(newImageSettings());
		}
		/** Create the table for the results */
		resultsTable = newResultsTable();
		/** Open the first image or return if the user canceled the dialog */
		analyzedImage = IJ.openImage(path + pathnames[index]);
		if(null == analyzedImage) {return;} 
		/** Create the processor for the analysis and the display images */
		displayImage = new ImagePlus();
		displayImage.setProcessor(pluginTitle, analyzedImage.getProcessor().duplicate());
		analyzedImage.setProcessor("", analyzedImage.getProcessor().duplicate().convertToByte(true));
		/** Create a toolbar for working with the display, selecting the most convenient tool */
		ij.gui.Toolbar.getInstance().setTool(ij.gui.Toolbar.FREEROI);
		/** Build GUI */
		win = new CustomWindow(displayImage);
		win.pack();
	}
	
	/**
	 * Settings functions
	 */
	/** Display the settings */
	@Override
	public void showSettingsDialog() {
		/** Creates the dialog */
		GenericDialogPlus settingsDialog = new GenericDialogPlus("Settings");
		settingsDialog.addMessage("Analysis parameters:");
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
		win.pack();
	}
	/** Create a list of settings with default values */
	@Override
	public Map<String, Object> newImageSettings() {
		Map<String, Object> defaultSettings = new HashMap();
		defaultSettings.put("Ratio px/u", ratio);
		defaultSettings.put("Physical units", physical_units);
		defaultSettings.put("Comments", comments);
		return defaultSettings;
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
		/** Get an image for each segment and count the particles */
		for(int l = 0; l < numOfClasses; l++) {
			/** Iterate over each segment */
			for(int j=0; j<segmentsROIs[l].size(); j++) {
				Roi r = segmentsROIs[l].get(j);
				/** Get original coordinates */
				double ogx = r.getBounds().getX();
				double ogy = r.getBounds().getY();
				/** Make a temporal duplicate of the image */
				ImagePlus segmentToAnalyze = new ImagePlus();
				segmentToAnalyze.setProcessor("", analyzedImage.getProcessor().duplicate());
				ImageProcessor ip = segmentToAnalyze.getProcessor();
				/** Select the roi */
				ip.setRoi(r);
				/** Blacken the outside of the roi */
				ip.setColor(0);
				ip.fillOutside(r);
				/** Crop the rest of the image*/
				segmentToAnalyze = new ImagePlus("", ip.crop());
				/**
				 * Insert custom analysis
				 */
				Object custom_variable1 = null;
				Object custom_variable2 = null;
				/** Add to the results table */
				addResultToTable(l, j, new Object[]{custom_variable1, custom_variable2});
			}
			/** Calculate and update total variables */
			Object total_custom_variable1 = null;
			Object total_custom_variable2 = null;
			updateTotalCount(l, new Object[]{total_custom_variable1, total_custom_variable2});
		}
		/** Remove and clean empty segments*/
		cleanSegmentRows();
		setComponentsEnabled(true);
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
