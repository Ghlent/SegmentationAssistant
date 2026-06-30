/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */
package sc.fiji.segmentationFramework;

/**
 *
 * @author vicentemerino
 */
import fiji.util.gui.GenericDialogPlus;
import fiji.util.gui.OverlayedImageCanvas;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.ImageWindow;
import ij.gui.Roi;
import ij.gui.Overlay;
import ij.gui.PointRoi;
import ij.io.RoiDecoder;
import ij.io.RoiEncoder;
import ij.io.OpenDialog;
import ij.io.SaveDialog;
import ij.plugin.PlugIn;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.Panel;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.io.File;
import java.io.FileWriter;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.json.JSONObject;
import org.json.JSONArray;
import trainableSegmentation.RoiListOverlay;

import java.awt.Dimension;
//import java.awt.Color;

public class SegmentationFramework implements PlugIn {

    /**
     * Name of the plugin
     */
    public String pluginTitle = "Prueba";

    /**
     * Max number of segment classes
     */
    public int MAX_NUM_CLASSES = 10;
    /**
     * Current number of classes
     */
    public int numOfClasses = 2;
    /**
     * Transparency of the segments
     */
    public Composite transparency = AlphaComposite.getInstance(3, 0.20F);
    /**
     * General name for the segments
     */
    public String generalSegmentName = "Segment";
    /**
     * Number of rows in the field for the segments
     */
    public int nrowsSegments = 3;

    /**
     * Max number of locale types
     */
    public int MAX_NUM_TYPES = 10;
    /**
     * Current number of classes
     */
    public int numOfTypes = 2;
    /**
     * General name for the segments
     */
    public String generalLocaleName = "Locale";
    /**
     * Number of rows in the field for the segments
     */
    public int nrowsLocales = 5;

    /**
     * Flags
     */
    public boolean showSegments;
    public boolean showPreview;
    public boolean showLocales;
    public boolean useLocales;

    /**
     * Names of the current classes
     */
    public String[] classLabels;
    /**
     * Available colors for available classes
     */
    public Color[] segmentsColors;
    /**
     * Array of roi list overlays to paint the transparent rois of each class
     */
    public RoiListOverlay[] segmentsOverlay;
    /**
     * Array that keeps track of the number of drawn segments per class. It's
     * just used to name them
     */
    public int[] segmentsCounter;
    /**
     * Array of segments rois for one image (these appears over the image)
     */
    public List<Roi>[] segmentsROIs;
    /**
     * List that stores the total set of all segment rois for all images
     */
    public List<List<Roi>[]> totalSegmentsROIs;
    /**
     * Array of segments names from one image (these appear on the segments
     * pannel)
     */
    public java.awt.List[] segmentsNames;
    /**
     * List that stores the total set of all segment names for all images
     */
    public List<java.awt.List[]> totalSegmentsNames;

    /**
     * Names of the current classes
     */
    public String[] typeLabels;
    /**
     * Available colors for available classes
     */
    public Color[] localesColors;
    /**
     * Overlay for the identified particles
     */
    public Overlay localesOverlay;
    /**
     * Array that keeps track of the number of drawn locales per types. It's
     * just used to name them
     */
    public int[] localesCounter;
    /**
     * Array of locale rois for one image
     */
    public List<Roi>[] localesROIs;
    /**
     * List that stores the total set of all segment rois for all images
     */
    public List<List<Roi>[]> totalLocalesROIs;
    /**
     * List of locale names for one image
     */
    public java.awt.List[] localesNames;
    /**
     * List that stores the total set of all segment names for all images
     */
    public List<java.awt.List[]> totalLocalesNames;

    /**
     * Array that stores the name of all images
     */
    public String[] pathnames;
    public String path;
    /**
     * Index that keeps track of the current image
     */
    public int index = 0;

    /**
     * List that stores the data for the results table of all images
     */
    public List<List<List<Object>>> resultsTable;
    /**
     * Array that stores the names of the statistical values added to the
     * results table
     */
    public String[] nameOfStatistics = new String[]{"Mean", "SD", "N"};
    /**
     * Number of statistical values
     */
    public int numOfStatistics = nameOfStatistics.length;

    /**
     * List that stores each image's settings values. Each image has its own Map
     * of parameters
     */
    public List<Map<String, Object>> settingsValues = new ArrayList();

    /**
     * Settings dialog
     */
    public GenericDialogPlus settingsDialog;
    /**
     * GUI window
     */
    public CustomWindow win;

    /**
     * Image that is being analyzed
     */
    public ImagePlus analyzedImage;
    /**
     * Image that is being displayed
     */
    public ImagePlus displayImage;

    /**
     * Custom analysis button
     */
    public JButton analysisButton;
    /**
     * Show/Hide segments button
     */
    public JButton segmentsButton;
    /**
     * Custom preview button
     */
    public JButton previewButton;
    /**
     * See result button
     */
    public JButton resultsButton;
    /**
     * Change image buttons
     */
    public JButton nextButton;
    public JButton previousButton;
    /**
     * Change classes and their segments buttons
     */
    public JButton addClassButton;
    public JButton deleteClassButton;
    public JButton editSegmentsButton;
    /**
     * Loading and saving buttons
     */
    public JButton exportButton;
    public JButton loadDataButton;
    public JButton saveDataButton;
    /**
     * Settings button
     */
    public JButton settingsButton;
    /**
     * Array of buttons for adding segments to each class
     */
    public JButton[] addSegmentButton;
    /**
     * Array of buttons for adding locales to each type
     */
    public JButton[] addLocaleButton;

    /**
     * Sliders for the channel and slice dimensions, and their labels
     */
    public JSlider cSlider;
    public JSlider zSlider;
    public JLabel cLabel;
    public JLabel zLabel;

    /**
     * Executor service to launch threads for the plugin methods and events
     */
    public final ExecutorService exec = Executors.newFixedThreadPool(1);

    /**
     * Basic constructor
     */
    public SegmentationFramework(boolean use_locales) {
        /**
         * Flags
         */
        showSegments = true;
        showPreview = false;
        showLocales = true;
        useLocales = use_locales;
        /**
         * Segments
         */
        classLabels = new String[MAX_NUM_CLASSES];
        for (int i = 0; i < MAX_NUM_CLASSES; i++) {
            classLabels[i] = "Class " + (i + 1);
        }
        segmentsColors = new Color[]{Color.green, Color.red, Color.blue, Color.cyan,
            Color.magenta, Color.orange, Color.pink, Color.gray, Color.darkGray, Color.white};
        segmentsOverlay = new RoiListOverlay[MAX_NUM_CLASSES];
        segmentsCounter = new int[MAX_NUM_CLASSES];
        segmentsROIs = new ArrayList[MAX_NUM_CLASSES];
        totalSegmentsROIs = new ArrayList();
        segmentsNames = new java.awt.List[MAX_NUM_CLASSES];
        totalSegmentsNames = new ArrayList();
        /**
         * Locales
         */
        if (useLocales) {
            typeLabels = new String[MAX_NUM_TYPES];
            localesColors = new Color[]{Color.green, Color.red, Color.blue, Color.cyan,
                Color.magenta, Color.orange, Color.pink, Color.gray, Color.darkGray, Color.white};
            localesOverlay = new Overlay();
            localesCounter = new int[MAX_NUM_TYPES];
            localesROIs = new ArrayList[MAX_NUM_TYPES];
            totalLocalesROIs = new ArrayList();
            localesNames = new java.awt.List[MAX_NUM_TYPES];
            totalLocalesNames = new ArrayList();
        }
        /**
         * Sliders
         */
        cSlider = new JSlider();
        cSlider.setMinimum(1);
        /*cSlider.setPaintLabels(true);
        cSlider.setPaintTicks(true);
        cSlider.setPaintTrack(true);*/
        cSlider.setPreferredSize(new Dimension(100, 5));
        cSlider.addChangeListener(sliderListener);
        zSlider = new JSlider();
        zSlider.setMinimum(1);
        zSlider.setPaintLabels(true);
        zSlider.setPaintTicks(true);
        zSlider.setPaintTrack(true);
        zSlider.setPreferredSize(new Dimension(100, 5));
        zSlider.addChangeListener(sliderListener);
        cLabel = new JLabel();
        zLabel = new JLabel();
    }

    /**
     * Listeners
     */
    /**
     * Listener for actions such as pressing buttons or double clicking a
     * segment
     */
    public ActionListener listener = new ActionListener() {
        @Override
        public void actionPerformed(final ActionEvent e) {
            /**
             * listen to the buttons on separate threads not to block the event
             * dispatch thread
             */
            exec.submit(new Runnable() {
                public void run() {
                    if (e.getSource() == analysisButton) {
                        try {
                            customAnalysis();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else if (e.getSource() == segmentsButton) {
                        show_hide_segments();
                    } else if (e.getSource() == previewButton) {
                        show_hide_preview();
                    } else if (e.getSource() == resultsButton) {
                        showResults();
                    } else if (e.getSource() == nextButton) {
                        changeImage("Next");
                    } else if (e.getSource() == previousButton) {
                        changeImage("Previous");
                    } else if (e.getSource() == exportButton) {
                        exportResults();
                    } else if (e.getSource() == addClassButton) {
                        addNewClass();
                    } else if (e.getSource() == deleteClassButton) {
                        deleteClass();
                    } else if (e.getSource() == editSegmentsButton) {
                        editSegmentsNames();
                    } else if (e.getSource() == loadDataButton) {
                        loadSessionFromJSON();
                    } else if (e.getSource() == saveDataButton) {
                        saveSessionAsJSON();
                    } else if (e.getSource() == settingsButton) {
                        showSettingsDialog();
                    } else {
                        /* Could technically select the maximum between numOfClasses
                        and numOfTypes, but this works as well */ 
                        for (int i = 0; i < numOfClasses+numOfTypes; i++) {
                            if (e.getSource() == segmentsNames[i]) {
                                deleteSelectedSegment(e, i);
                                break;
                            }
                            if (e.getSource() == addSegmentButton[i]) {
                                addSegments(i);
                                break;
                            }
                            if (useLocales) {
                                if (e.getSource() == localesNames[i]) {
                                    deleteSelectedLocale(e, i);
                                    break;
                                }
                                if (e.getSource() == addLocaleButton[i]) {
                                    addLocale(i);
                                    break;
                                }
                            }
                        }
                    }
                }
            });
        }
    };
    /**
     * Listener for highlighting a pressed segment
     */
    public ItemListener itemListener = new ItemListener() {
        @Override
        public void itemStateChanged(final ItemEvent e) {
            exec.submit(new Runnable() {
                public void run() {
                    for (int i = 0; i < numOfClasses; i++) {
                        if (e.getSource() == segmentsNames[i]) {
                            listSelectedSegment(e, i);
                        }
                    }
                }
            });
        }
    };
    /**
     * Listener for the slider that controls the coordinate in the dimensons of
     * the displayImage
     */
    public ChangeListener sliderListener = new ChangeListener() {
        @Override
        public void stateChanged(ChangeEvent e) {
            exec.submit(new Runnable() {
                public void run() {
                    if (e.getSource() == cSlider) {
                        changeCoordinates("c", cSlider.getValue());
                    } else if (e.getSource() == zSlider) {
                        changeCoordinates("z", zSlider.getValue());
                    }
                }
            });
        }
    };

    /**
     * Custom window for the UI, with all the classes for the panels
     */
    public class CustomWindow extends ImageWindow {

        /**
         * Instantiate components
         */
        public GridBagLayout winLayout = new GridBagLayout();
        public GridBagConstraints winConstraints = new GridBagConstraints();
        public OverlayedImageCanvas canvas;
        public AnalysisPanel analysisPanel = new AnalysisPanel();
        public SegmentsPanel segmentsPanel = new SegmentsPanel(true);
        public LocalesPanel localesPanel;
        public Panel winPanel = new Panel();

        /**
         * Default constructor for the window
         */
        public CustomWindow(ImagePlus imp) {
            /**
             * Instantiate canvas
             */
            super(imp, new OverlayedImageCanvas(imp));
            canvas = (OverlayedImageCanvas) this.getCanvas();
            /**
             * Add Roi overlays (one per possible class) to the total Roi
             * overlay, and the total Roi overlay to the canvas
             */
            for (int i = 0; i < MAX_NUM_CLASSES; i++) {
                segmentsOverlay[i] = new RoiListOverlay();
                segmentsOverlay[i].setComposite(transparency);
                canvas.addOverlay(segmentsOverlay[i]);
            }
            
            /**
             * Add jpanels and canvas into a container panel, and the panel to
             * the window
             */
            winPanel.setLayout(winLayout);
            winConstraints.fill = GridBagConstraints.BOTH;
            winConstraints.gridx = 0;
            winConstraints.gridy = 0;
            winPanel.add(analysisPanel, winConstraints);
            winConstraints.gridx++;
            winPanel.add(canvas, winConstraints);
            winConstraints.gridx++;
            winPanel.add(segmentsPanel, winConstraints);
            if (useLocales) {
                winConstraints.gridx++;
                localesPanel = new LocalesPanel(true);
                winPanel.add(localesPanel, winConstraints);
            }
            this.add(winPanel);
            /**
             * Set title
             */
            this.setTitle(pluginTitle);
        }

        /**
         * Zooms out of images that expand the windows too much
         */
        //Temporal fix
        public void fixZoom() {
            while (canvas.getHeight() > 1000 | canvas.getWidth() > 1000) {
                canvas.zoomOut(0, 0);
            }
        }
    }

    /**
     * Class for the analysis panel
     */
    public class AnalysisPanel extends JPanel {

        /**
         * Layout, Constraints and JPanel for the analysis panel
         */
        public GridBagLayout analysisLayout = new GridBagLayout();
        public GridBagConstraints analysisConstraints = new GridBagConstraints();
        /**
         * Layout, Constraints, JPanel and buttons for the actions subpanel
         */
        public GridBagLayout actionsLayout = new GridBagLayout();
        public GridBagConstraints actionsConstraints = new GridBagConstraints();
        public JPanel actionsPanel = new JPanel();
        /**
         * Layout, Constraints and JPanel for the classes subpanel
         */
        public GridBagLayout classesLayout = new GridBagLayout();
        public GridBagConstraints classesConstraints = new GridBagConstraints();
        public JPanel classesPanel = new JPanel();
        /**
         * Layout, Constraints and JPanel for the options subpanel
         */
        public GridBagLayout optionsLayout = new GridBagLayout();
        public GridBagConstraints optionsConstraints = new GridBagConstraints();
        public JPanel optionsPanel = new JPanel();
        /**
         * Layout, Constraints and JPanel for the dimensions subpanel
         */
        public GridBagLayout dimensionsLayout = new GridBagLayout();
        public GridBagConstraints dimensionsConstraints = new GridBagConstraints();
        public JPanel dimensionsPanel = new JPanel();

        /**
         * Default constructor
         */
        public AnalysisPanel() {
            /**
             * Initialize the buttons
             */
            analysisButton = new JButton("Perform analysis");
            analysisButton.setToolTipText("Perform the programmed analysis");
            analysisButton.addActionListener(listener);
            segmentsButton = new JButton("Show/Hide segments");
            segmentsButton.setToolTipText("Show/Hide the segments drawn over the image");
            segmentsButton.addActionListener(listener);
            previewButton = new JButton("Preview/Hide analysis");
            previewButton.setToolTipText("Preview/Hide aspects important for the analysis");
            previewButton.addActionListener(listener);
            resultsButton = new JButton("See results");
            resultsButton.setToolTipText("See the results table for all images");
            resultsButton.addActionListener(listener);
            nextButton = new JButton("Next image");
            nextButton.setToolTipText("Move to the next image");
            nextButton.addActionListener(listener);
            previousButton = new JButton("Previous image");
            previousButton.setToolTipText("Move to the previous image");
            previousButton.addActionListener(listener);
            addClassButton = new JButton("Add new class");
            addClassButton.setToolTipText("Add a new empty class");
            addClassButton.addActionListener(listener);
            deleteClassButton = new JButton("Delete class");
            deleteClassButton.setToolTipText("Remove a class and all its data");
            deleteClassButton.addActionListener(listener);
            editSegmentsButton = new JButton("Edit segment names");
            editSegmentsButton.setToolTipText("Open a dialog with all the segment names in the image for their edition");
            editSegmentsButton.addActionListener(listener);
            exportButton = new JButton("Export results");
            exportButton.setToolTipText("Export results as csv file");
            exportButton.addActionListener(listener);
            loadDataButton = new JButton("Load data");
            loadDataButton.setToolTipText("Load previous segmentation and results from a .json file");
            loadDataButton.addActionListener(listener);
            saveDataButton = new JButton("Save data");
            saveDataButton.setToolTipText("Save current segmentation and results into an .json file");
            saveDataButton.addActionListener(listener);
            settingsButton = new JButton("Settings");
            settingsButton.addActionListener(listener);
            settingsButton.setToolTipText("Display settings dialog");
            /**
             * Actions subpanel (upper left)
             */
            actionsPanel.setBorder(BorderFactory.createTitledBorder("Actions"));
            actionsConstraints.fill = GridBagConstraints.HORIZONTAL;
            actionsConstraints.gridx = 0;
            actionsConstraints.gridy = 0;
            actionsConstraints.insets = new Insets(5, 6, 5, 6);
            actionsPanel.setLayout(actionsLayout);
            /**
             * Add each button and their listeners
             */
            actionsPanel.add(analysisButton, actionsConstraints);
            actionsConstraints.gridy++;
            actionsPanel.add(segmentsButton, actionsConstraints);
            actionsConstraints.gridy++;
            actionsPanel.add(previewButton, actionsConstraints);
            actionsConstraints.gridy++;
            actionsPanel.add(resultsButton, actionsConstraints);
            actionsConstraints.gridy++;
            actionsPanel.add(nextButton, actionsConstraints);
            actionsConstraints.gridy++;
            actionsPanel.add(previousButton, actionsConstraints);
            actionsConstraints.gridy++;
            /**
             * Class subpanel (middle left)
             */
            classesPanel.setBorder(BorderFactory.createTitledBorder("Classes"));
            classesConstraints.fill = GridBagConstraints.HORIZONTAL;
            classesConstraints.gridx = 0;
            classesConstraints.gridy = 0;
            classesConstraints.insets = new Insets(5, 6, 5, 6);
            classesPanel.setLayout(classesLayout);
            /**
             * Add each button and their listeners
             */
            classesPanel.add(addClassButton, classesConstraints);
            classesConstraints.gridy++;
            classesPanel.add(deleteClassButton, classesConstraints);
            classesConstraints.gridy++;
            classesPanel.add(editSegmentsButton, classesConstraints);
            classesConstraints.gridy++;
            /**
             * Options subpanel (lower left)
             */
            optionsPanel.setBorder(BorderFactory.createTitledBorder("Options"));
            optionsConstraints.fill = GridBagConstraints.HORIZONTAL;
            optionsConstraints.gridx = 0;
            optionsConstraints.gridy = 0;
            optionsConstraints.insets = new Insets(5, 6, 5, 6);
            optionsPanel.setLayout(optionsLayout);
            /**
             * Add each button and their listeners
             */
            optionsPanel.add(exportButton, optionsConstraints);
            optionsConstraints.gridy++;
            optionsPanel.add(loadDataButton, optionsConstraints);
            optionsConstraints.gridy++;
            optionsPanel.add(saveDataButton, optionsConstraints);
            optionsConstraints.gridy++;
            optionsPanel.add(settingsButton, optionsConstraints);
            optionsConstraints.gridy++;
            /**
             * Dimensions subpanel (bottom sliders)
             */
            dimensionsPanel.setBorder(BorderFactory.createTitledBorder("Dimensions"));
            dimensionsConstraints.fill = GridBagConstraints.HORIZONTAL;
            dimensionsConstraints.gridx = 0;
            dimensionsConstraints.gridy = 0;
            dimensionsConstraints.insets = new Insets(5, 6, 5, 6);
            dimensionsPanel.setLayout(dimensionsLayout);
            /**
             * Add each slider and their listeners
             */
            dimensionsPanel.add(cLabel, dimensionsConstraints);
            dimensionsConstraints.gridx++;
            dimensionsPanel.add(cSlider, dimensionsConstraints);
            dimensionsConstraints.gridx = 0;
            dimensionsConstraints.gridy++;
            dimensionsPanel.add(zLabel, dimensionsConstraints);
            dimensionsConstraints.gridx++;
            dimensionsPanel.add(zSlider, dimensionsConstraints);
            /**
             * Wrap panels
             */
            analysisConstraints.fill = GridBagConstraints.HORIZONTAL;
            analysisConstraints.gridx = 0;
            analysisConstraints.gridy = 0;
            this.setLayout(analysisLayout);
            this.add(actionsPanel, analysisConstraints);
            analysisConstraints.gridy++;
            this.add(classesPanel, analysisConstraints);
            analysisConstraints.gridy++;
            this.add(optionsPanel, analysisConstraints);
            analysisConstraints.gridy++;
            this.add(dimensionsPanel, analysisConstraints);
        }
    }

    /**
     * Class for segments panels
     */
    public class SegmentsPanel extends JPanel {

        /**
         * Layout, Constraints and JPanel for segments panels
         */
        public GridBagLayout segmentsLayout;
        public GridBagConstraints segmentsConstraints;

        /**
         * Default constructor
         */
        public SegmentsPanel(boolean initialization) {
            /**
             * Instantiate the fields and buttons
             */
            if (initialization) {
                addSegmentButton = new JButton[MAX_NUM_CLASSES];
                for (int i = 0; i < MAX_NUM_CLASSES; i++) {
                    segmentsROIs[i] = new ArrayList<Roi>();
                    segmentsNames[i] = new java.awt.List(nrowsSegments);
                    segmentsNames[i].setForeground(segmentsColors[i]);
                    segmentsNames[i].addItemListener(itemListener);
                    segmentsNames[i].addActionListener(listener);
                    addSegmentButton[i] = new JButton("Add to " + classLabels[i]);
                    addSegmentButton[i].setToolTipText("Add markings of label '" + classLabels[i] + "'");
                    addSegmentButton[i].addActionListener(listener);
                }
            }
            /**
             * Segments panel
             */
            segmentsLayout = new GridBagLayout();
            segmentsConstraints = new GridBagConstraints();
            segmentsConstraints.gridx = 0;
            segmentsConstraints.gridy = 0;
            this.setBorder(BorderFactory.createTitledBorder("Segments"));
            this.setLayout(segmentsLayout);
            /**
             * Add one subpanel for each class, and another column every 5
             */
            for (int i = 0; i < numOfClasses; i++) {
                if (i % 5 == 0 && i != 0) {
                    segmentsConstraints.gridx++;
                    segmentsConstraints.gridy = 0;
                }
                /**
                 * Adding buttons
                 */
                segmentsConstraints.fill = GridBagConstraints.HORIZONTAL;
                segmentsConstraints.insets = new Insets(5, 6, 5, 6); // Space surrounding buttons (U, L, D, R)
                segmentsLayout.setConstraints(addSegmentButton[i], segmentsConstraints);
                this.add(addSegmentButton[i]);
                segmentsConstraints.gridy++;
                /**
                 * Adding fields for the segments
                 */
                segmentsConstraints.insets = new Insets(0, 5, 0, 5); // Space surrounding fields (U, L, D, R)
                segmentsLayout.setConstraints(segmentsNames[i], segmentsConstraints);
                this.add(segmentsNames[i]);
                segmentsConstraints.gridy++;
            }
        }
    }

    /**
     * Class for segments panels
     */
    public class LocalesPanel extends JPanel {

        /**
         * Layout, Constraints and JPanel for locales panels
         */
        public GridBagLayout localesLayout;
        public GridBagConstraints localesConstraints;

        /**
         * Default constructor
         */
        public LocalesPanel(boolean initialization) {
            /**
             * Instantiate the fields and buttons
             */
            if (initialization) {
                addLocaleButton = new JButton[MAX_NUM_TYPES];
                for (int i = 0; i < MAX_NUM_TYPES; i++) {
                    localesROIs[i] = new ArrayList<Roi>();
                    localesNames[i] = new java.awt.List(nrowsLocales);
                    localesNames[i].setForeground(localesColors[i]);
                    localesNames[i].addItemListener(itemListener);
                    localesNames[i].addActionListener(listener);
                    addLocaleButton[i] = new JButton("Add to " + typeLabels[i]);
                    addLocaleButton[i].setToolTipText("Add markings of label '" + typeLabels[i] + "'");
                    addLocaleButton[i].addActionListener(listener);
                }
            }
            /**
             * Locales panel
             */
            localesLayout = new GridBagLayout();
            localesConstraints = new GridBagConstraints();
            localesConstraints.gridx = 0;
            localesConstraints.gridy = 0;
            this.setBorder(BorderFactory.createTitledBorder("Locales"));
            this.setLayout(localesLayout);
            /**
             * Add one subpanel for each type, and another column every 5
             */
            for (int i = 0; i < numOfTypes; i++) {
                if (i % 5 == 0 && i != 0) {
                    localesConstraints.gridx++;
                    localesConstraints.gridy = 0;
                }
                /**
                 * Adding buttons
                 */
                localesConstraints.fill = GridBagConstraints.HORIZONTAL;
                localesConstraints.insets = new Insets(5, 6, 5, 6); // Space surrounding buttons (U, L, D, R)
                localesLayout.setConstraints(addLocaleButton[i], localesConstraints);
                this.add(addLocaleButton[i]);
                localesConstraints.gridy++;
                /**
                 * Adding fields for the segments
                 */
                localesConstraints.insets = new Insets(0, 5, 0, 5); // Space surrounding fields (U, L, D, R)
                localesLayout.setConstraints(localesNames[i], localesConstraints);
                this.add(localesNames[i]);
                localesConstraints.gridy++;
            }
        }
    }

    /**
     * Segmentation functions
     */
    /** Add traces defined by the user as segments to the corresponding list */
    public void addSegments(final int i) {
        /** Get selected pixels in the trace if there is any */
        Roi r = displayImage.getRoi();
        /** If not, select the whole image as a Roi */
        if (r == null) {
            r = wholeImageRoi(displayImage);
        }
        /** Add the segment to the objects keeping track of them */
        segmentsROIs[i].add(r);
        segmentsNames[i].add(generalSegmentName + " " + segmentsCounter[i]);
        segmentsCounter[i]++;
        /** Update the overlay */
        drawSegments();
        /** The trace is no longer needed */
        displayImage.killRoi();
    }

    /** Add traces (generally point-like) defined by the user as locales to the corresponding list */
    public void addLocale(int i) {
        /** Get selected pixels */
        final Roi r = displayImage.getRoi();
        if (r == null){
                return;
            }
            localesROIs[i].add(r);
        localesNames[i].add("Locale " + localesCounter[i]); 
        localesCounter[i]++;
        drawLocales(null);
        displayImage.killRoi();
    }
        
    /** Generate a roi the size of the whole image */
    public Roi wholeImageRoi(ImagePlus img) {
        return new Roi(0, 0, img.getWidth(), img.getHeight());
    }

    /** Draw the stored segments on the display image */
    public void drawSegments() {
        /** Adds the ROI of the segments to the overlay, with the color of their
            class */
        for (int i = 0; i < numOfClasses; i++) {
            segmentsOverlay[i].setColor(segmentsColors[i]);
            final ArrayList< Roi> rois = new ArrayList<Roi>();
            for (Roi r : segmentsROIs[i]) {
                rois.add(r);
            }
            segmentsOverlay[i].setRoi(rois);
        }
        /** Update the displayed imaged according to the overlay */
        displayImage.updateAndDraw();
    }
    
    /** Draw the stored locales on the display image */
    public void drawLocales(PointRoi selectedLocale) {
        localesOverlay = new Overlay();
        if (selectedLocale != null) {
            selectedLocale.setStrokeColor​(Color.white);
            selectedLocale.setStrokeWidth​(1.);
            selectedLocale.setFillColor​(Color.white);
            localesOverlay.add(selectedLocale);
        }
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
        localesOverlay.setDraggable(false); 
        displayImage.setOverlay​(localesOverlay);
    }

    /**
     * Highlight the selected segment
     */
    public void listSelectedSegment(final ItemEvent e, final int i) {
        /**
         * Set selected segment as current ROI, thus highlight its bound in the
         * display image and its name
         */
        final Roi newRoi = segmentsROIs[i].get(segmentsNames[i].getSelectedIndex());
        displayImage.setRoi(newRoi);
        /**
         * Deselect any previously highlighted segment name in other classes
         */
        for (int j = 0; j < numOfClasses; j++) {
            if (j != i) {
                segmentsNames[j].deselect(segmentsNames[j].getSelectedIndex());
            }
        }
    }
    
    /** Highlight in white the selected segment */
    void listSelectedLocale(final ItemEvent e, final int i) {
	PointRoi selectedLocale = new PointRoi();
	for (int h = 0; h < 2; h++) {
            if (h == i) {
                Roi selectedRoi = localesROIs[h].get(localesNames[h].getSelectedIndex()); 
                for (Point containedPoint : selectedRoi.getContainedPoints()) {
                    selectedLocale.addPoint(containedPoint.getX(), containedPoint.getY());
                }
            } else {
		localesNames[h].deselect(localesNames[h].getSelectedIndex());
            }
	}
	drawLocales(selectedLocale);
    }
    
    /** Delete one of the locale ROIs */
    void deleteSelectedLocale(final ActionEvent e, int i) {
	/** Get and delete the locale from the objects keeping track of them */
        int ix = localesNames[i].getSelectedIndex();
	localesROIs[i].remove(ix);
	localesNames[i].remove(ix);
        /** Update the overlay */
        drawLocales(null);
        /** Delete from global lists if it already exist on them*/
	if (totalLocalesNames.get(index) != null) {
            totalLocalesROIs.set(index, localesROIs);
            totalLocalesNames.set(index, localesNames);	
        }
    }

    /**
     * Delete the selected segment
     */
    public void deleteSelectedSegment(final ActionEvent e, final int i) {
        /**
         * Get and delete the segment from the objects keeping track of them
         */
        int ix = segmentsNames[i].getSelectedIndex();
        segmentsROIs[i].remove(ix);
        segmentsNames[i].remove(ix);
        /**
         * Kill Roi from displayed image
         */
        displayImage.killRoi(); //Revisar
        /**
         * Update the overlay
         */
        drawSegments();
        /**
         * Delete from global lists if it already exist on them (this is shorter
         * than calling globalSegmentsSave())
         */
        if (totalSegmentsNames.get(index) != null) {
            totalSegmentsROIs.set(index, segmentsROIs);
            totalSegmentsNames.set(index, segmentsNames);
        }
    }

    /**
     * Hide the displayed segments
     */
    public void hideSegments() {
        /**
         * Replace each ROI in the overlay with an empty placeholder
         */
        for (int i = 0; i < numOfClasses; i++) {
            segmentsOverlay[i].setRoi(new ArrayList<Roi>());
        }
        /**
         * Update the displayed imaged according to the overlay
         */
        displayImage.updateAndDraw();
    }

    /**
     * Show or hide the segments
     */
    public void show_hide_segments() {
        if (!showSegments) {
            drawSegments();
            showSegments = true;
        } else {
            hideSegments();
            showSegments = false;
        }
    }

    /** Show or hide the segments */
    public void show_hide_locales() {
        if (!showLocales) {
            drawLocales(null);
            showLocales = true;
        } else {
            localesOverlay.clear();
            displayImage.setOverlay​(localesOverlay);
            showLocales = false;
        }
    }
    
    /**
     * Create and open a dialog with all the segment names and the possibility
     * of editing them
     */
    public void editSegmentsNames() {
        /**
         * Omit if there are no segments
         */
        boolean segmentsEmpty = true;
        for (int i = 0; i < numOfClasses; i++) {
            if (!segmentsROIs[i].isEmpty()) {
                segmentsEmpty = false;
                break;
            }
        }
        if (segmentsEmpty == true) {
            return;
        }
        /**
         * Creates the dialog
         */
        setComponentsEnabled(false);
        GenericDialogPlus editingDialog = new GenericDialogPlus("Edit segment names");
        for (int i = 0; i < numOfClasses; i++) {
            editingDialog.addMessage(classLabels[i] + ":");
            for (int j = 0; j < segmentsNames[i].getItemCount(); j++) {
                editingDialog.addStringField​(generalSegmentName + " " + j + ":", segmentsNames[i].getItem(j), 16);
            }
        }
        editingDialog.showDialog();
        /**
         * Update the names
         */
        for (int i = 0; i < numOfClasses; i++) {
            for (int j = 0; j < segmentsNames[i].getItemCount(); j++) {
                segmentsNames[i].replaceItem(editingDialog.getNextString(), j);
            }
        }
        win.pack();
        setComponentsEnabled(true);
    }

    /**
     * Store the current marked segments into global lists
     */
    public void globalSegmentsSave() {
        /**
         * Omit if there are no segments
         */
        boolean segmentsEmpty = true;
        for (int i = 0; i < numOfClasses; i++) {
            if (!segmentsROIs[i].isEmpty()) {
                segmentsEmpty = false;
                break;
            }
        }
        if (segmentsEmpty == true) {
            return;
        }
        /**
         * Add clones of each segment's ROI to totalSegmentsROIs
         */
        List<Roi>[] cloned_segmentsROIs = new ArrayList[MAX_NUM_CLASSES];
        for (int i = 0; i < numOfClasses; i++) {
            cloned_segmentsROIs[i] = new ArrayList<Roi>(segmentsROIs[i]);
        }
        totalSegmentsROIs.set(index, cloned_segmentsROIs);
        /**
         * Add clones of each segment's name to totalSegmentsNames
         */
        java.awt.List[] cloned_segmentsNames = new java.awt.List[MAX_NUM_CLASSES];
        for (int i = 0; i < numOfClasses; i++) {
            cloned_segmentsNames[i] = new java.awt.List(nrowsSegments);
            for (String s : segmentsNames[i].getItems()) {
                cloned_segmentsNames[i].add(s);
            }
        }
        totalSegmentsNames.set(index, cloned_segmentsNames);
    }
    
    /** Store the current marked locales into global lists */
    public void globalLocalesSave() {
        /**
         * Omit if there are no locales
         */
        boolean localesEmpty = true;
        for (int i = 0; i < numOfTypes; i++) {
            if (!localesROIs[i].isEmpty()) {
                localesEmpty = false;
                break;
            }
        }
        if (localesEmpty == true) {
            return;
        }
        /**
         * Add clones of each locale's ROI to totalLocalesROIs
         */
        List<Roi>[] cloned_localesROIs = new ArrayList[MAX_NUM_TYPES];
        for (int i = 0; i < numOfTypes; i++) {
            cloned_localesROIs[i] = new ArrayList<Roi>(localesROIs[i]);
        }
        totalLocalesROIs.set(index, cloned_localesROIs);
        /**
         * Add clones of each locale's name to totalLocalesNames
         */
        java.awt.List[] cloned_localesNames = new java.awt.List[MAX_NUM_TYPES];
        for (int i = 0; i < numOfTypes; i++) {
            cloned_localesNames[i] = new java.awt.List(nrowsLocales);
            for (String s : localesNames[i].getItems()) {
                cloned_localesNames[i].add(s);
            }
        }
        totalLocalesNames.set(index, cloned_localesNames);
    }

    /**
     * Moving through images
     */
    public void changeImage(String direction) {
        setComponentsEnabled(false);
        if (direction.equals("Next") & index == (pathnames.length - 1)) {
            setComponentsEnabled(true);
            return;
        }
        if (direction.equals("Previous") & index == 0) {
            setComponentsEnabled(true);
            return;
        }
        /**
         * Save segment and locales data of the current image
         */
        globalSegmentsSave();
        if (useLocales) {
            globalLocalesSave();
        }
        /**
         * Clean segment and locale data, if there is any
         */
        if (totalSegmentsNames.get(index) != null) {
            for (int i = 0; i < numOfClasses; i++) {
                for (int j = 0; j < totalSegmentsNames.get(index)[i].getItemCount(); j++) {
                    /**
                     * Kill segments from displayed image and delete their names
                     */
                    segmentsROIs[i].remove(0);
                    segmentsNames[i].remove(0);
                }
            }
        }
        drawSegments(); //Revisar
        if (useLocales) {
            if (totalLocalesNames.get(index) != null) {
                for (int i = 0; i < numOfTypes; i++) {
                    for (int j = 0; j < totalLocalesNames.get(index)[i].getItemCount(); j++) {
                        localesROIs[i].remove(0);
                        localesNames[i].remove(0);
                    }
                }
            }
            drawLocales(null); //Revisar
        }
        /**
         * Move onto the next or previous image by changing the index and
         * loading a new file
         */
        if (direction.equals("Next")) {
            index++;
        }
        if (direction.equals("Previous")) {
            index--;
        }
        analyzedImage = IJ.openImage(path + pathnames[index]);
        displayImage = analyzedImage.duplicate();
        /**
         * Set up from the metadata if the analysis requieres it
         */
        setUpFromMetadata();
        applicationSpecificSetUp();
        win.setImage(displayImage);
        win.fixZoom();
        //analyzedImage.setProcessor("", analyzedImage.getProcessor().convertToByte(true));
        setSliders();
        updateSliderLabels();
        /**
         * Retrieve segments and locales of the next or previous image from the global list,
         * if there is any
         */
        if (totalSegmentsNames.get(index) != null) {
            for (int i = 0; i < numOfClasses; i++) {
                for (int j = 0; j < totalSegmentsNames.get(index)[i].getItemCount(); j++) {
                    segmentsROIs[i].add(totalSegmentsROIs.get(index)[i].get(j));
                    segmentsNames[i].add(totalSegmentsNames.get(index)[i].getItem(j));
                }
            }
            drawSegments();
        }
        if (useLocales) {
            if (totalLocalesNames.get(index) != null) {
                for (int i = 0; i < numOfTypes; i++) {
                    for (int j = 0; j < totalLocalesNames.get(index)[i].getItemCount(); j++) {
                        localesROIs[i].add(totalLocalesROIs.get(index)[i].get(j));
                        localesNames[i].add(totalLocalesNames.get(index)[i].getItem(j));
                    }
                }
                drawLocales(null);
            }
        }
        applicationSpecificChanges();
        setComponentsEnabled(true);
    }
    
    /**
     * Processes that should be perform every time that the image changes,
     * isolated so the user doesn't have to modify the whole changeImage()
     */
    public void applicationSpecificSetUp() {
        //Meant to be overriden
        IJ.run(analyzedImage, "8-bit", "true");
    }
    public void applicationSpecificChanges() {
        //Meant to be overriden
    }
    
    /**
     * Change the displayed coordinates according to the sliders state
     */
    public void changeCoordinates(String dimension, int coordinate) {
        if (dimension.equals("c")) {
            displayImage.setC(coordinate);
        }
        if (dimension.equals("z")) {
            displayImage.setZ(coordinate);
        }
        updateSliderLabels();
    }

    /**
     * Sets the sliders max and current values when initiating the plugin or
     * changing images
     */
    public void setSliders() {
        cSlider.setMaximum(analyzedImage.getNChannels());
        cSlider.setValue(1);
        zSlider.setMaximum(analyzedImage.getNSlices());
        zSlider.setValue(1);
    }

    /**
     * Updates the labels describing the current coordinates of the displayImage
     */
    public void updateSliderLabels() {
        cLabel.setText("c: " + displayImage.getC() + "/" + displayImage.getNChannels());
        zLabel.setText("z: " + displayImage.getZ() + "/" + displayImage.getNSlices());
    }

    public void setUpFromMetadata() {
        //Meant to be overriden
    }

    /**
     * Add a new class to the segments panel
     */
    public void addNewClass() {
        /**
         * Check the if the maximum amount has been reached
         */
        if (numOfClasses == MAX_NUM_CLASSES) {
            IJ.showMessage(pluginTitle, "Sorry, maximum number of classes has been reached");
            return;
        }
        /**
         * Increase number of current classes, update the window and results
         * table
         */
        numOfClasses++;
        updateTracingPanels();
        addClassToResultsTable();
    }

    /**
     * Display a dialog with a popup menu for choosing the class to delete
     */
    public void deleteClass() {
        /**
         * Check the if there are any classes left
         */
        if (numOfClasses == 0) {
            IJ.showMessage(pluginTitle, "Sorry, there are no classes left for deletion");
            return;
        }
        /**
         * Get the class
         */
        GenericDialogPlus deleteDialog = new GenericDialogPlus("Delete Class");
        String[] options = Arrays.copyOfRange(classLabels, 0, numOfClasses);
        deleteDialog.addChoice​("Select the class you want to delete:", options, options[0]);
        deleteDialog.showDialog();
        if (deleteDialog.wasCanceled()) {
            return;
        }
        int class_to_delete = deleteDialog.getNextChoiceIndex();
        /**
         * Reduce the number of the classes with defaul labels after the one
         * deleted, preserve the label otherwise for the classes that go down a
         * position
         */
        for (int i = class_to_delete; i < MAX_NUM_CLASSES - 1; i++) {
            if (!classLabels[i + 1].startsWith("Class ")) {
                classLabels[i] = classLabels[i + 1];
            } else {
                classLabels[i] = "Class " + (i + 1);
            }
        }
        classLabels[MAX_NUM_CLASSES - 1] = "Class " + MAX_NUM_CLASSES;
        /**
         * Reassign the properties for the classes that go down a position, so
         * they preserve their color, ROIs, segment names and buttons
         */
        Color deleted_color = segmentsColors[class_to_delete];
        for (int i = class_to_delete; i < numOfClasses - 1; i++) {
            segmentsColors[i] = segmentsColors[i + 1];
            segmentsROIs[i] = segmentsROIs[i + 1];
            segmentsNames[i] = segmentsNames[i + 1];
            segmentsNames[i].setForeground(segmentsColors[i]);
            segmentsNames[i].addItemListener(itemListener);
            segmentsNames[i].addActionListener(listener);
            addSegmentButton[i] = addSegmentButton[i + 1];
            addSegmentButton[i].setText("Add to " + classLabels[i]);
            addSegmentButton[i].addActionListener(listener);
        }
        segmentsColors[numOfClasses - 1] = deleted_color;
        segmentsROIs[numOfClasses - 1] = new ArrayList<Roi>();
        segmentsNames[numOfClasses - 1] = new java.awt.List(nrowsSegments);
        segmentsNames[numOfClasses - 1].setForeground(segmentsColors[numOfClasses - 1]);
        segmentsNames[numOfClasses - 1].addItemListener(itemListener);
        segmentsNames[numOfClasses - 1].addActionListener(listener);
        addSegmentButton[numOfClasses - 1] = new JButton("Add to " + classLabels[numOfClasses - 1]);
        addSegmentButton[numOfClasses - 1].setToolTipText("Add markings of label '" + classLabels[numOfClasses - 1] + "'");
        addSegmentButton[numOfClasses - 1].addActionListener(listener);
        /**
         * Decrease number of current classes and update the window
         */
        numOfClasses--;
        updateTracingPanels();
        drawSegments();
    }

    /**
     * Settings functions
     */
    /**
     * Display the settings
     */
    public void showSettingsDialog() {
        //Meant to be overriden
        GenericDialogPlus settingsDialog = new GenericDialogPlus("Settings");
        settingsDialog.showDialog();
    }

    /**
     * Create a list of settings with default values
     */
    public Map<String, Object> newImageSettings() {
        //Meant to be overriden
        Map<String, Object> defaultSettings = new HashMap();
        return defaultSettings;
    }

    /**
     * Results functions
     */
    /**
     * Display the results table
     */
    public void showResults() {
        /**
         * Remove one dimention to the results table, which is the separation
         * between image sections
         */
        List<List<Object>> flatterTable = new ArrayList();
        for (List<List<Object>> imageSection : resultsTable) {
            for (List<Object> row : imageSection) {
                flatterTable.add(row);
            }
        }
        /**
         * Transform the results into an array
         */
        Object[][] resultsAsArray = new Object[flatterTable.size()][flatterTable.get(0).size()];
        for (int i = 0; i < flatterTable.size(); i++) {
            resultsAsArray[i] = flatterTable.get(i).toArray();
        }
        /**
         * Create empty names for the columns
         */
        Object[] cnames = new Object[numOfClasses * (numOfStatistics + 1)];
        for (int i = 0; i < numOfClasses * (numOfStatistics + 1); i++) {
            cnames[i] = i + 1;
        }
        /**
         * Make a JTable for visualization
         */
        JTable table = new JTable(resultsAsArray, cnames);
        JScrollPane scrollPane = new JScrollPane(table);
        JFrame resultsFrame = new JFrame("Results");
        resultsFrame.setSize(500, 500);
        resultsFrame.getContentPane().add(scrollPane);
        resultsFrame.setVisible(true);
    }

    /**
     * The total results table is a list of image sections
     */
    public List<List<List<Object>>> newResultsTable() {
        List<List<List<Object>>> emptyResultsTable = new ArrayList();
        for (String imagePath : pathnames) {
            emptyResultsTable.add(newImageSection(imagePath));
        }
        return emptyResultsTable;
    }

    /**
     * An image section is the portion of the total table that corresponds to
     * each image. Rows are independent and Columns are created by adding cells
     * to all rows in all image sections
     */
    public List<List<Object>> newImageSection(String name) {
        List<List<Object>> emptyTable = new ArrayList();
        emptyTable.add(newNameRow(name));
        emptyTable.add(newClassRow());
        emptyTable.add(newStatRow());
        emptyTable.add(newTotalRow());
        /**
         * Add an empty row for space
         */
        emptyTable.add(newEmptyRow());
        return emptyTable;
    }

    /**
     * Type of row without information. Used as placeholder or base for others
     */
    public List<Object> newEmptyRow() {
        List<Object> row = new ArrayList();
        for (int i = 0; i < numOfClasses * (numOfStatistics + 1); i++) {
            row.add("");
        }
        return row;
    }

    /**
     * Row containing the name of the image
     */
    public List<Object> newNameRow(String name) {
        List<Object> row = newEmptyRow();
        row.set(0, name);
        return row;
    }

    /**
     * Row dedicated to the names of the classes
     */
    public List<Object> newClassRow() {
        List<Object> row = newEmptyRow();
        for (int i = 0; i < numOfClasses; i++) {
            row.set(i * (numOfStatistics + 1) + 1, classLabels[i]);
        }
        return row;
    }

    /**
     * Row containing the names of the statistics
     */
    public List<Object> newStatRow() {
        List<Object> row = newEmptyRow();
        for (int i = 0; i < numOfStatistics; i++) {
            for (int j = 0; j < numOfClasses; j++) {
                row.set(j * (numOfStatistics + 1) + (i + 1), nameOfStatistics[i]);
            }
        }
        return row;
    }

    /**
     * Row with the values of the statistics calculated for all segments
     */
    public List<Object> newTotalRow() {
        List<Object> row = newEmptyRow();
        for (int i = 0; i < numOfClasses; i++) {
            row.set(i * (numOfStatistics + 1), "Total");
        }
        return row;
    }

    /**
     * Add columns to the table for a new class
     */
    public void addClassToResultsTable() {
        for (List<List<Object>> imageSection : resultsTable) {
            for (int i = 0; i < imageSection.size(); i++) {
                List<Object> row = imageSection.get(i);
                /**
                 * Add new columns
                 */
                for (int j = 0; j < numOfStatistics + 1; j++) {
                    row.add("");
                }
                /**
                 * Fill the cell with the class' name
                 */
                if (i == 1) {
                    row.set(row.size() - numOfStatistics, classLabels[numOfClasses - 1]);
                }
                /**
                 * Fill the cells with the stats names
                 */
                if (i == 2) {
                    for (int j = 0; j < numOfStatistics; j++) {
                        row.set(row.size() - (numOfStatistics - j), nameOfStatistics[j]);
                    }
                }
                /**
                 * Fill the cell with "Total" written
                 */
                if (i == imageSection.size() - 2) {
                    row.set(row.size() - (numOfStatistics + 1), "Total");
                }
            }
        }
    }

    /**
     * Add the results to the corresponding segment and class
     */
    public void addResultToTable(int nclass, int nsegment, Object[] results) {
        /**
         * Check if there is a row available for the segment (the image section
         * should have the initial 5 plus one for each segment)
         */
        List<List<Object>> imageSection = resultsTable.get(index);
        if ((5 + nsegment) < imageSection.size()) {
            /**
             * The rows for the segments start at the fourth row of the image
             * section
             */
            List<Object> row = imageSection.get(3 + nsegment);
            row.set((numOfStatistics + 1) * nclass, segmentsNames[nclass].getItem(nsegment));
            for (int i = 0; i < numOfStatistics; i++) {
                row.set((numOfStatistics + 1) * nclass + (i + 1), results[i]);
            }
        } else {
            /**
             * If there is not, add a new row to the table
             */
            List<Object> row = newEmptyRow();
            row.set((numOfStatistics + 1) * nclass, segmentsNames[nclass].getItem(nsegment));
            for (int i = 0; i < numOfStatistics; i++) {
                row.set((numOfStatistics + 1) * nclass + (i + 1), results[i]);
            }
            imageSection.add(3 + nsegment, row);
        }
    }

    /**
     * Update the "Total" rows at the second to last row of the image section
     */
    public void updateTotalCount(int nclass, Object[] results) {
        List<List<Object>> imageSection = resultsTable.get(index);
        List<Object> row = imageSection.get(imageSection.size() - 2);
        for (int i = 0; i < numOfStatistics; i++) {
            row.set((numOfStatistics + 1) * nclass + (i + 1), results[i]);
        }
    }

    /**
     * Analysis functions
     */
    public void customAnalysis() {
        //Meant to be overriden
    }

    public void show_hide_preview() {
        //Meant to be overriden
    }

    /**
     * Loading and saving functions
     */
    /**
     * Accepted file formats for images
     */
    public boolean checkFormat(final String fileName) {
        if (fileName.startsWith(".")) {return false;}
        if (fileName.endsWith(".jpg") || fileName.endsWith(".tiff") || fileName.endsWith(".dicom")
                || fileName.endsWith(".fits") || fileName.endsWith(".pgm") || fileName.endsWith(".bmp")
                || fileName.endsWith(".gif") || fileName.endsWith(".nd2") || fileName.endsWith(".png")
                || fileName.endsWith(".jpeg") || fileName.endsWith(".pdf")) {
            return true;
        }
        return false;
    }

    /**
     * Export the results as a csv file
     */
    public void exportResults() {
        /**
         * Ask for name of the segments file
         */
        SaveDialog sd = new SaveDialog("Export", "results", ".csv");
        if (sd.getFileName() == null) {
            return;
        }
        /**
         * Export
         */
        try {
            /**
             * Create the csv file and fill it with the data
             */
            FileWriter csv = new FileWriter(sd.getDirectory() + sd.getFileName());
            for (List<List<Object>> imageSection : resultsTable) {
                for (List<Object> row : imageSection) {
                    for (Object cell : row) {
                        csv.write(cell.toString() + ";");
                    }
                    csv.write(System.lineSeparator());
                }
                csv.write(System.lineSeparator());
            }
            csv.close();
            IJ.log("Exported results table to: " + sd.getDirectory() + sd.getFileName());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Functions for exporting the final state of the session in a human
     * readable format
     */
    //ROIs should be made human readable in the future by using coordinates
    public void saveSessionAsJSON() {
        SaveDialog sd = new SaveDialog("Save as JSON", "session", ".json");
        if (sd.getFileName() == null) {
            return;
        }
        globalSegmentsSave();
        if (useLocales) {
        	globalLocalesSave();
        }
        try {
            FileWriter jsonFile = new FileWriter(sd.getDirectory() + sd.getFileName());
            jsonFile.write("{" + System.lineSeparator());
            /**
             * Global variables
             */
            jsonFile.write("\"classLabels\": [" + "\"" + classLabels[0] + "\"");
            for (int i = 1; i < classLabels.length; i++) {
                jsonFile.write(", \"" + classLabels[i] + "\"");
            }
            jsonFile.write("]," + System.lineSeparator());
            jsonFile.write("\"segmentsCounter\" : " + Arrays.toString(segmentsCounter) + "," + System.lineSeparator());
            jsonFile.write("\"numOfClasses\": " + numOfClasses + "," + System.lineSeparator());
            if (useLocales) {
            	jsonFile.write("\"typeLabels\": [" + "\"" + typeLabels[0] + "\"");
	            for (int i = 1; i < typeLabels.length; i++) {
	                jsonFile.write(", \"" + typeLabels[i] + "\"");
	            }
	            jsonFile.write("]," + System.lineSeparator());
	            jsonFile.write("\"localesCounter\" : " + Arrays.toString(localesCounter) + "," + System.lineSeparator());
            	jsonFile.write("\"numOfTypes\": " + numOfTypes + "," + System.lineSeparator());
            }
            /**
             * Local variables
             */
            jsonFile.write("\"images\": [" + System.lineSeparator());
            for (int i = 0; i < pathnames.length; i++) {
                jsonFile.write("{" + System.lineSeparator());
                jsonFile.write("	\"pathname\": \"" + pathnames[i] + "\"," + System.lineSeparator());
                jsonFile.write("	\"settingsValues\": {" + System.lineSeparator());
                Object[] keys = settingsValues.get(i).keySet().toArray();
                jsonFile.write("		\"" + keys[0] + "\": " + settingsValues.get(i).get(keys[0])); //notar
                for (int j = 1; j < keys.length; j++) {
                    jsonFile.write("," + System.lineSeparator() + "		\"" + keys[j] + "\": " + settingsValues.get(i).get(keys[j]));
                }
                jsonFile.write(System.lineSeparator() + "	}," + System.lineSeparator());
                /**
                 * Segment Names
                 */
                jsonFile.write("	\"segmentsNames\": [" + System.lineSeparator());
                if (totalSegmentsNames.get(i) != null) {
                    try {
                        jsonFile.write("		[" + "\"" + totalSegmentsNames.get(i)[0].getItem(0) + "\"");
                        for (int k = 1; k < totalSegmentsROIs.get(i)[0].size(); k++) {
                            jsonFile.write(", \"" + totalSegmentsNames.get(i)[0].getItem(k) + "\"");
                        }
                        jsonFile.write("]");
                    } catch (Exception e) {
                        jsonFile.write("		null");
                    }
                    for (int j = 1; j < numOfClasses; j++) {
                        try {
                            jsonFile.write("," + System.lineSeparator() + "		[" + "\"" + totalSegmentsNames.get(i)[j].getItem(0) + "\"");
                            for (int k = 1; k < totalSegmentsROIs.get(i)[j].size(); k++) {
                                jsonFile.write(", \"" + totalSegmentsNames.get(i)[j].getItem(k) + "\"");
                            }
                            jsonFile.write("]");
                        } catch (Exception e) {
                            jsonFile.write("," + System.lineSeparator() + "		null");
                        }
                    }
                }
                jsonFile.write(System.lineSeparator() + "	]," + System.lineSeparator());
                /**
                 * Segment ROIs
                 */
                jsonFile.write("	\"segmentsROIs\": [" + System.lineSeparator());
                if (totalSegmentsNames.get(i) != null) {
                    try {
                        jsonFile.write("		[" + Arrays.toString(RoiEncoder.saveAsByteArray(totalSegmentsROIs.get(i)[0].get(0))));
                        for (int k = 1; k < totalSegmentsROIs.get(i)[0].size(); k++) {
                            jsonFile.write("," + Arrays.toString(RoiEncoder.saveAsByteArray(totalSegmentsROIs.get(i)[0].get(k))));
                        }
                        jsonFile.write("]");
                    } catch (Exception e) {
                        jsonFile.write("		null");
                    }
                    for (int j = 1; j < numOfClasses; j++) {
                        try {
                            jsonFile.write("," + System.lineSeparator() + "		[" + Arrays.toString(RoiEncoder.saveAsByteArray(totalSegmentsROIs.get(i)[j].get(0))));
                            for (int k = 1; k < totalSegmentsROIs.get(i)[j].size(); k++) {
                                jsonFile.write("," + Arrays.toString(RoiEncoder.saveAsByteArray(totalSegmentsROIs.get(i)[j].get(k))));
                            }
                            jsonFile.write("]");
                        } catch (Exception e) {
                            jsonFile.write("," + System.lineSeparator() + "		null");
                        }
                    }
                }
                jsonFile.write(System.lineSeparator() + "	]," + System.lineSeparator());
                /**
                 * Locales Names
                 */
                if (useLocales) {
                        jsonFile.write("	\"localesNames\": [" + System.lineSeparator());
	                if (totalLocalesNames.get(i) != null) {
	                    try {
	                        jsonFile.write("		[" + "\"" + totalLocalesNames.get(i)[0].getItem(0) + "\"");
	                        for (int k = 1; k < totalLocalesROIs.get(i)[0].size(); k++) {
	                            jsonFile.write(", \"" + totalLocalesNames.get(i)[0].getItem(k) + "\"");
	                        }
	                        jsonFile.write("]");
	                    } catch (Exception e) {
	                        jsonFile.write("		null");
	                    }
	                    for (int j = 1; j < numOfTypes; j++) {
	                        try {
	                            jsonFile.write("," + System.lineSeparator() + "		[" + "\"" + totalLocalesNames.get(i)[j].getItem(0) + "\"");
	                            for (int k = 1; k < totalLocalesROIs.get(i)[j].size(); k++) {
	                                jsonFile.write(", \"" + totalLocalesNames.get(i)[j].getItem(k) + "\"");
	                            }
	                            jsonFile.write("]");
	                        } catch (Exception e) {
	                            jsonFile.write("," + System.lineSeparator() + "		null");
	                        }
	                    }
	                }
	                jsonFile.write(System.lineSeparator() + "	]," + System.lineSeparator());	
                }
                /**
                 * Locales ROIs
                 */
	             if (useLocales) {
	                jsonFile.write("	\"localesROIs\": [" + System.lineSeparator());
	                if (totalLocalesNames.get(i) != null) {
	                    try {
	                        jsonFile.write("		[" + Arrays.toString(RoiEncoder.saveAsByteArray(totalLocalesROIs.get(i)[0].get(0))));
	                        for (int k = 1; k < totalLocalesROIs.get(i)[0].size(); k++) {
	                            jsonFile.write("," + Arrays.toString(RoiEncoder.saveAsByteArray(totalLocalesROIs.get(i)[0].get(k))));
	                        }
	                        jsonFile.write("]");
	                    } catch (Exception e) {
	                        jsonFile.write("		null");
	                    }
	                    for (int j = 1; j < numOfTypes; j++) {
	                        try {
	                            jsonFile.write("," + System.lineSeparator() + "		[" + Arrays.toString(RoiEncoder.saveAsByteArray(totalLocalesROIs.get(i)[j].get(0))));
	                            for (int k = 1; k < totalLocalesROIs.get(i)[j].size(); k++) {
	                                jsonFile.write("," + Arrays.toString(RoiEncoder.saveAsByteArray(totalLocalesROIs.get(i)[j].get(k))));
	                            }
	                            jsonFile.write("]");
	                        } catch (Exception e) {
	                            jsonFile.write("," + System.lineSeparator() + "		null");
	                        }
	                    }
	                }
	                jsonFile.write(System.lineSeparator() + "	]," + System.lineSeparator());
                }
                /**
                 * Results table
                 */
                jsonFile.write("	\"resultsTable\": [" + System.lineSeparator());
                jsonFile.write("	[" + "\"" + resultsTable.get(i).get(0).get(0) + "\"");
                for (int k = 1; k < resultsTable.get(i).get(0).size(); k++) {
                    Object cell = resultsTable.get(i).get(0).get(k);
                    jsonFile.write(",\"" + cell + "\"");
                }
                jsonFile.write("]");
                for (int j = 1; j < resultsTable.get(i).size(); j++) {
                    Object cell = resultsTable.get(i).get(j).get(0);
                    if (cell.equals("")) {
                        jsonFile.write("," + System.lineSeparator() + "	[\"\"");
                    } else {
                        jsonFile.write("," + System.lineSeparator() + "	[" + cell);
                    }
                    for (int k = 1; k < resultsTable.get(i).get(j).size(); k++) {
                        cell = resultsTable.get(i).get(j).get(k);
                        jsonFile.write(",\"" + cell + "\"");
                    }
                    jsonFile.write("]");
                }
                jsonFile.write(System.lineSeparator() + "	]");
                jsonFile.write(System.lineSeparator());
                if (i == pathnames.length - 1) {
                    jsonFile.write("}" + System.lineSeparator());
                } else {
                    jsonFile.write("}," + System.lineSeparator());
                }
            }
            jsonFile.write("]" + System.lineSeparator());
            jsonFile.write("}");
            jsonFile.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Load all data, which requires deleting the current session and
     * recognizing which images are new and which were removed in the folder
     */
    public void loadSessionFromJSON() {
        OpenDialog od = new OpenDialog("Choose data file", "");
        if (od.getFileName() == null) {
            return;
        }
        try {
            /**
             * Clean segment data, if there is any
             */
            if (totalSegmentsNames.get(index) != null) {
                for (int i = 0; i < numOfClasses; i++) {
                    for (int j = 0; j < totalSegmentsNames.get(index)[i].getItemCount(); j++) {
                        /**
                         * Kill segments from displayed image and delete their
                         * names
                         */
                        segmentsROIs[i].remove(0);
                        segmentsNames[i].remove(0);
                    }
                }
            }
            drawSegments();
            /* Clean locale data, if there is any */
            if (useLocales) {
                if (totalLocalesNames.get(index) != null) {
                    for (int i = 0; i < numOfTypes; i++) {
                        for (int j = 0; j < totalLocalesNames.get(index)[i].getItemCount(); j++) {
                            /**
                             * Kill segments from displayed image and delete their
                             * names
                             */
                            localesROIs[i].remove(0);
                            localesNames[i].remove(0);
                        }
                    }
                }
                drawLocales(null);
            }
            /**
             * Load file
             */
            File jsonFile = new File(od.getDirectory() + od.getFileName());
            String data = new String(Files.readAllBytes(jsonFile.toPath()));
            JSONObject dataAsJSON = new JSONObject(data);
            /**
             * Global variables
             */
            JSONArray JSON_classLabels = dataAsJSON.getJSONArray("classLabels");
            for (int i = 0; i < JSON_classLabels.length(); i++) {
                classLabels[i] = JSON_classLabels.getString(i);
                addSegmentButton[i].setText("Add to " + classLabels[i]);
            }
            JSONArray JSON_segmentsCounter = dataAsJSON.getJSONArray("segmentsCounter");
            for (int i = 0; i < segmentsCounter.length; i++) {
                segmentsCounter[i] = JSON_segmentsCounter.getInt(i);
            }
            numOfClasses = dataAsJSON.getInt("numOfClasses");
            if (useLocales) {
                JSONArray JSON_typeLabels = dataAsJSON.getJSONArray("typeLabels");
                for (int i = 0; i < JSON_typeLabels.length(); i++) {
                    typeLabels[i] = JSON_typeLabels.getString(i);
                    addLocaleButton[i].setText("Add to " + typeLabels[i]);
                }
                JSONArray JSON_localesCounter = dataAsJSON.getJSONArray("localesCounter");
                for (int i = 0; i < localesCounter.length; i++) {
                    localesCounter[i] = JSON_localesCounter.getInt(i);
                }
                numOfTypes = dataAsJSON.getInt("numOfTypes");
            }
            /**
             * Global arrays and lists for local variables
             */
            JSONArray localDataAsJSON = dataAsJSON.getJSONArray("images");
            String[] old_pathnames = new String[localDataAsJSON.length()];
            settingsValues = new ArrayList();
            totalSegmentsNames = new ArrayList();
            totalSegmentsROIs = new ArrayList();
            if (useLocales) {
                totalLocalesNames = new ArrayList();
                totalLocalesROIs = new ArrayList();
            }
            resultsTable = new ArrayList();
            for (int i = 0; i < localDataAsJSON.length(); i++) {
                JSONObject JSON_image = localDataAsJSON.getJSONObject(i);
                old_pathnames[i] = JSON_image.getString("pathname");
                /**
                 * Settings values
                 */
                settingsValues.add(newImageSettings());
                JSONObject JSON_settingsValues = JSON_image.getJSONObject("settingsValues");
                for (String key : JSON_settingsValues.keySet()) {
                    if (JSON_settingsValues.get(key).getClass().getName().equals("java.math.BigDecimal")) {
                        settingsValues.get(i).put(key, ((BigDecimal) JSON_settingsValues.get(key)).doubleValue());
                    } else {
                        settingsValues.get(i).put(key, JSON_settingsValues.get(key));
                    }
                }
                /**
                 * Segment Names
                 */
                JSONArray JSON_segmentsNames = JSON_image.getJSONArray("segmentsNames");
                java.awt.List[] temp_segmentsNames = new java.awt.List[MAX_NUM_CLASSES];
                for (int j = 0; j < temp_segmentsNames.length; j++) {
                    temp_segmentsNames[j] = new java.awt.List(nrowsSegments);
                    temp_segmentsNames[j].setForeground(segmentsColors[j]);
                    temp_segmentsNames[j].addItemListener(itemListener);
                    temp_segmentsNames[j].addActionListener(listener);
                }
                for (int j = 0; j < JSON_segmentsNames.length(); j++) {
                    try {
                        JSONArray class_segmentsNames = JSON_segmentsNames.getJSONArray(j);
                        for (int k = 0; k < class_segmentsNames.length(); k++) {
                            temp_segmentsNames[j].add(class_segmentsNames.getString(k));
                        }
                    } catch (Exception e) {
                    }
                }
                totalSegmentsNames.add(temp_segmentsNames);
                /**
                 * Segment ROIs
                 */
                JSONArray JSON_segmentsROIs = JSON_image.getJSONArray("segmentsROIs");
                List<Roi>[] temp_segmentsROIs = new ArrayList[MAX_NUM_CLASSES];
                for (int j = 0; j < temp_segmentsROIs.length; j++) {
                    temp_segmentsROIs[j] = new ArrayList<Roi>();
                }
                for (int j = 0; j < JSON_segmentsROIs.length(); j++) {
                    try {
                        JSONArray class_segmentsROIs = JSON_segmentsROIs.getJSONArray(j);
                        for (int k = 0; k < class_segmentsROIs.length(); k++) {
                            JSONArray individual_roi = class_segmentsROIs.getJSONArray(k);
                            byte[] byte_roi = new byte[individual_roi.length()];
                            for (int byt = 0; byt < individual_roi.length(); byt++) {
                                byte_roi[byt] = (byte) individual_roi.getInt(byt);
                            }
                            temp_segmentsROIs[j].add(RoiDecoder.openFromByteArray(byte_roi));
                        }
                    } catch (Exception e) {
                    }
                }
                totalSegmentsROIs.add(temp_segmentsROIs);
                /**
                 * Locales Names
                 */
                if (useLocales) {
                    JSONArray JSON_localesNames = JSON_image.getJSONArray("localesNames");
                    java.awt.List[] temp_localesNames = new java.awt.List[MAX_NUM_TYPES];
                    for (int j = 0; j < temp_localesNames.length; j++) {
                        temp_localesNames[j] = new java.awt.List(nrowsLocales);
                        temp_localesNames[j].setForeground(localesColors[j]);
                        temp_localesNames[j].addItemListener(itemListener);
                        temp_localesNames[j].addActionListener(listener);
                    }
                    for (int j = 0; j < JSON_localesNames.length(); j++) {
                        try {
                            JSONArray class_localesNames = JSON_localesNames.getJSONArray(j);
                            for (int k = 0; k < class_localesNames.length(); k++) {
                                temp_localesNames[j].add(class_localesNames.getString(k));
                            }
                        } catch (Exception e) {
                        }
                    }
                    totalLocalesNames.add(temp_localesNames);
                }
                /**
                 * Locales ROIs
                 */
                if (useLocales) {
                    JSONArray JSON_localesROIs = JSON_image.getJSONArray("localesROIs");
                    List<Roi>[] temp_localesROIs = new ArrayList[MAX_NUM_TYPES];
                    for (int j = 0; j < temp_localesROIs.length; j++) {
                        temp_localesROIs[j] = new ArrayList<Roi>();
                    }
                    for (int j = 0; j < JSON_localesROIs.length(); j++) {
                        try {
                            JSONArray class_localesROIs = JSON_localesROIs.getJSONArray(j);
                            for (int k = 0; k < class_localesROIs.length(); k++) {
                                JSONArray individual_roi = class_localesROIs.getJSONArray(k);
                                byte[] byte_roi = new byte[individual_roi.length()];
                                for (int byt = 0; byt < individual_roi.length(); byt++) {
                                    byte_roi[byt] = (byte) individual_roi.getInt(byt);
                                }
                                temp_localesROIs[j].add(RoiDecoder.openFromByteArray(byte_roi));
                            }
                        } catch (Exception e) {
                        }
                    }
                    totalLocalesROIs.add(temp_localesROIs);
                }
                /**
                 * Results table
                 */
                JSONArray JSON_resultsTable = JSON_image.getJSONArray("resultsTable");
                List<List<Object>> temp_imageSection = new ArrayList();
                for (int j = 0; j < JSON_resultsTable.length(); j++) {
                    JSONArray row = JSON_resultsTable.getJSONArray(j);
                    List<Object> temp_row = new ArrayList();
                    for (int k = 0; k < row.length(); k++) {
                        temp_row.add(row.get(k));
                    }
                    temp_imageSection.add(temp_row);
                }
                resultsTable.add(temp_imageSection);
            }
            /**
             * Remove images from the data that have been deleted from the folder
             */
            for (int i = old_pathnames.length - 1; i >= 0; i = i - 1) {
                boolean absent = true;
                for (int j = pathnames.length - 1; j >= 0; j = j - 1) {
                    if (old_pathnames[i].equals(pathnames[j])) {
                        absent = false;
                        break;
                    }
                }
                if (absent) {
                    totalSegmentsNames.remove(i);
                    totalSegmentsROIs.remove(i);
                    resultsTable.remove(i);
                    settingsValues.remove(i);
                    /**
                     * Update old names
                     */
                    String[] temp_pathnames = new String[old_pathnames.length - 1];
                    for (int j = 0; j < old_pathnames.length; j++) {
                        if (j < i) {
                            temp_pathnames[j] = old_pathnames[j];
                        } else if (j > i) {
                            temp_pathnames[j - 1] = old_pathnames[j];
                        }
                    }
                    old_pathnames = temp_pathnames;
                }
            }
            /**
             * Add images to the data that have been added to the folder
             */
            for (int i = 0; i < pathnames.length; i++) {
                boolean absent = true;
                for (int j = 0; j < old_pathnames.length; j++) {
                    if (pathnames[i].equals(old_pathnames[j])) {
                        absent = false;
                        break;
                    }
                }
                if (absent) {
                    totalSegmentsNames.add(i, null);
                    totalSegmentsROIs.add(i, null);
                    resultsTable.add(i, newImageSection(pathnames[i]));
                    settingsValues.add(i, newImageSettings());
                }
            }
            /**
             * Reload the current image and the window
             */
            if (totalSegmentsNames.get(index) != null) {
                for (int i = 0; i < numOfClasses; i++) {
                    for (int j = 0; j < totalSegmentsNames.get(index)[i].getItemCount(); j++) {
                        segmentsROIs[i].add(totalSegmentsROIs.get(index)[i].get(j));
                        segmentsNames[i].add(totalSegmentsNames.get(index)[i].getItem(j));
                    }
                }
                drawSegments();
            }
            if (useLocales) {
                if (totalLocalesNames.get(index) != null) {
                    for (int i = 0; i < numOfTypes; i++) {
                        for (int j = 0; j < totalLocalesNames.get(index)[i].getItemCount(); j++) {
                            localesROIs[i].add(totalLocalesROIs.get(index)[i].get(j));
                            localesNames[i].add(totalLocalesNames.get(index)[i].getItem(j));
                        }
                    }
                    drawLocales(null);
                }
            }
            updateTracingPanels();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Other functions
     */
    /**
     * Enable/disable components
     */
    public void setComponentsEnabled(Boolean s) {
        analysisButton.setEnabled(s);
        segmentsButton.setEnabled(s);
        previewButton.setEnabled(s);
        resultsButton.setEnabled(s);
        nextButton.setEnabled(s);
        previousButton.setEnabled(s);
        addClassButton.setEnabled(s);
        deleteClassButton.setEnabled(s);
        editSegmentsButton.setEnabled(s);
        exportButton.setEnabled(s);
        loadDataButton.setEnabled(s);
        saveDataButton.setEnabled(s);
        settingsButton.setEnabled(s);
        for (int i = 0; i < numOfClasses; i++) {
            segmentsNames[i].setEnabled(s);
            addSegmentButton[i].setEnabled(s);
        }
    }

    /** Recreates the segments and locales panels when they can't be updated */
    public void updateTracingPanels() {
        if (useLocales) {
            win.winPanel.remove(3);
        }
        win.winPanel.remove(2);
        win.segmentsPanel = new SegmentsPanel(false);
        win.winConstraints.gridx++;
        win.winPanel.add(win.segmentsPanel, win.winConstraints);
        if (useLocales) {
            win.localesPanel = new LocalesPanel(false);
            win.winConstraints.gridx++;
            win.winPanel.add(win.localesPanel, win.winConstraints);
        }
        win.revalidate();
        win.repaint();
    }

    public void run(String arg) {
        //Meant to be overriden
    }
}
