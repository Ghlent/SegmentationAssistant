# SegmentationAssistant

This project is centered around a framework for programming segmentation-related plugins for FIJI/ImageJ. The main code deals with a user interface that manages all the general steps that a segmentation analysis would follow, such as marking the image, keeping track of the regions of interest, and registering the results. The user then can personalize the measurements through a simpler template that overrides the automatic measurements performed on the segments.

## Tool description
### Basic features
Credits must be given to the open-source plugin Weka Trainable Segmentation (Arganda-Carreras et al., 2017), whose interface possesses the tools for manually assigning regions of an image to a certain class and served as a base for all future modifications and additions. From it, SegmentationAssistant’s base UI [Figure 1.A] preserves the screen at the center, for drawing over an image, and the right panel, where the users can save under a class, and then select them or delete them.
However, Weka’s purpose is to train models for segmentation, so in order to better support the full analysis task, the other panels or their code have been completely overhauled with new or different functions.
If the user chooses to incorporate it, a panel is added for the purpose of manually selected points called locales.
“Perform analysis” executes the procedure programmed by the user and extracts a set of variables that are appended to a pre-formated table [Figure 1.B], which can be visualized using “See results”.
“Next image” and “Previous image” operate on a directory that is selected during initialization, so users can work with multiple images, saving their segments and maintaining independent settings for each one.
“Settings” [Figure 1.C] is there to be modified accordingly to the needs of the user, but by default it allows the manual definition of the scale and units of the image when there is a problem with the metadata, add comments to the results table, and change the name of the classes.
“Preview/Hide analysis” must also be coded, but it is intended to display some middle point of the performed analysis with the current settings to debug when programming or adjust parameter when working.
For long term storage, “Save data” creates a human-readable JSON file with all the information for restoring the session with “Load data”.
Finally, “Export results” saves only the results table as a CSV file.

### The template
The users can create plugins with SegmentationAssistant by modifying a template written in Java. No change is necessary for the program to run, but users are expected to define in code the name of the plugin, whether they are using the point selection tool or not, the measured variables that will appear in the results table, the analysis that will be performed to measure those variables, and the settings used in the analysis. For more complex needs, the framework and template contemplate functions that are performed automatically every time the settings or the image change, and a section for the analysis’ preview. If that is still insufficient for the demands of the problem, all functions and classes in the framework are modifiable from the template. This all means that users need coding knowledge to make use of the framework directly, but everyone can benefit from the plugins created with it.
