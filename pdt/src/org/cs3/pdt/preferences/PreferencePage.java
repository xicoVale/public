package org.cs3.pdt.preferences;

import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.*;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.cs3.pdt.PDT;
import org.cs3.pdt.PDTPlugin;

/**
 * This class represents a preference page that is contributed to the
 * Preferences dialog. By subclassing <samp>FieldEditorPreferencePage</samp>, we
 * can use the field support built into JFace that allows us to create a page
 * that is small and knows how to save, restore and apply itself.
 * <p>
 * This page is used to modify preferences only. They are stored in the
 * preference store that belongs to the main plug-in class. That way,
 * preferences can be accessed directly via the preference store.
 */

public class PreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public PreferencePage() {
		super(GRID);
		setPreferenceStore(PDTPlugin.getDefault().getPreferenceStore());
		setDescription("Preferences for the PDT Plugin");
	}

	/**
	 * Creates the field editors. Field editors are abstractions of the common
	 * GUI blocks needed to manipulate various types of preferences. Each field
	 * editor knows how to save and restore itself.
	 */
	public void createFieldEditors() {
		// Determines the verbosity of the debug log file.		
		addField(new RadioGroupFieldEditor(PDT.PREF_DEBUG_LEVEL, "Debug Level", 4, new String[][] {
				{ "error", "ERROR" }, { "warning", "WARNING" },
				{ "info", "INFO" }, { "debug", "DEBUG" } }, getFieldEditorParent(),true));

		//A file to which debug output of the PDT will be writen
		addField(new FileFieldEditor(PDT.PREF_CLIENT_LOG_FILE, "Log file location", getFieldEditorParent()));
		
		//When i open a file in the prolog editor that does not belong to 
		//a prolog project, ask if i want to add the prolog nature.
		addField(new RadioGroupFieldEditor(PDT.PREF_ADD_NATURE_ON_OPEN, "Automatically add Prolog Nature when opening pl files", 4, new String[][] {
				{ "always", MessageDialogWithToggle.ALWAYS }, { "never", MessageDialogWithToggle.NEVER },
				{ "ask", MessageDialogWithToggle.PROMPT } }, getFieldEditorParent(),true));
		
		//When i consult a prolog file, but the active console view is not connected to the default runtime
		//of the respective prolog project, should i switch to the default runtime first?
		addField(new RadioGroupFieldEditor(PDT.PREF_SWITCH_TO_DEFAULT_PIF, "Switch to default runtime before consulting", 4, new String[][] {
				{ "always", MessageDialogWithToggle.ALWAYS }, { "never", MessageDialogWithToggle.NEVER },
				{ "ask", MessageDialogWithToggle.PROMPT } }, getFieldEditorParent(),true));
		
		
		//A comma separated list of filter ids that should be activated at startup
		StringFieldEditor sfe = new StringFieldEditor(PDT.PREF_OUTLINE_FILTERS, "Active Filters for the Prolog Outline", getFieldEditorParent());
		sfe.setEnabled(false, getFieldEditorParent()); // disabled, because Lukas made it before invisible in his implementation
		addField(sfe);
		
		
		BooleanFieldEditor bfe = new BooleanFieldEditor(PDT.PREF_OUTLINE_SORT, "Whether the Prolog Outline is to be sorted lexicographical", getFieldEditorParent());
		bfe.setEnabled(false, getFieldEditorParent()); // disabled, because Lukas made it before invisible in his implementation
		addField(bfe);

		
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}

}