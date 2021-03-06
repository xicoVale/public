/*****************************************************************************
 * This file is part of the Prolog Development Tool (PDT)
 * 
 * WWW: http://sewiki.iai.uni-bonn.de/research/pdt/start
 * Mail: pdt@lists.iai.uni-bonn.de
 * Copyright (C): 2004-2012, CS Dept. III, University of Bonn
 * 
 * All rights reserved. This program is  made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 ****************************************************************************/

package org.cs3.pdt.common;

import static org.cs3.prolog.common.QueryUtils.bT;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.cs3.prolog.common.OptionProviderListener;
import org.cs3.prolog.common.Util;
import org.cs3.prolog.common.logging.Debug;
import org.cs3.prolog.connector.PrologConnectorPredicates;
import org.cs3.prolog.pif.PrologInterface;
import org.cs3.prolog.pif.PrologInterfaceException;
import org.cs3.prolog.ui.util.FileUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class PDTCommonPlugin extends AbstractUIPlugin implements BundleActivator {

	public static final QualifiedName ENTRY_POINT_KEY = new QualifiedName("pdt", "entry.point");
	
	private static BundleContext context;

	private static PDTCommonPlugin plugin;
	
	public static final String PLUGIN_ID = "org.cs3.pdt.common";
	
	public PDTCommonPlugin() {
		super();
		plugin = this;
	}
	
	static BundleContext getContext() {
		return context;
	}
	
	/**
	 * Returns the shared instance.
	 */
	public static PDTCommonPlugin getDefault() {
		return plugin;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(BundleContext bundleContext) throws Exception {
		super.stop(bundleContext);
	}
	
	
	@Override
	public void start(BundleContext bundleContext) throws Exception{
		super.start(bundleContext);
		reconfigureDebugOutput();
		IPropertyChangeListener debugPropertyChangeListener = new IPropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent e) {
				try {
					if (!"console.no.focus".equals(e.getProperty())) {
						PDTCommonPlugin.this.reconfigureDebugOutput();
					}
				} catch (FileNotFoundException e1) {
					Debug.report(e1);
				}
			}

		};	
		getPreferenceStore().addPropertyChangeListener(debugPropertyChangeListener);
	}
	
	private void reconfigureDebugOutput() throws FileNotFoundException {
		String debugLevel = getPreferenceValue(PDTCommon.PREF_DEBUG_LEVEL, "WARNING");
		String debugOutputTo = getPreferenceValue(PDTCommon.PREF_DEBUG_OUTPUT_TO, "LOGFILE");
		String logFileName = getPreferenceValue(PDTCommon.PREF_CLIENT_LOG_FILE_DIR, System.getProperty("java.io.tmpdir"));
		
		Debug.setDebugLevel(debugLevel);
		Debug.setLogDir(logFileName);	
		Debug.setOutputTo(debugOutputTo);
	}
	
	/**
	 * look up a preference value.
	 * <p>
	 * will return user settings if available or default settings if not. If a
	 * system property with the given key is defined it will overrule any
	 * existing setting in the preference store. if the key is not defined, this
	 * method returns the given default..
	 * 
	 * @param key
	 * @return the value or specified default if no such key exists..
	 */
	public String getPreferenceValue(String key, String defaultValue) {

		IPreferencesService service = Platform.getPreferencesService();
		String qualifier = getBundle().getSymbolicName();
		String value = service.getString(qualifier, key, defaultValue, null);
		return System.getProperty(key, value);
	}

	/**
	 * Returns a section in the Prolog plugin's dialog settings. If the section doesn't exist yet, it is created.
	 *
	 * @param name the name of the section
	 * @return the section of the given name
	 */
	public IDialogSettings getDialogSettingsSection(String name) {
		IDialogSettings dialogSettings= getDialogSettings();
		IDialogSettings section= dialogSettings.getSection(name);
		if (section == null) {
			section= dialogSettings.addNewSection(name);
		}
		return section;
	}
	
	/* 
	 * entry point handling
	 */
	private Set<IFile> entryPoints;

	public void addEntryPoint(IFile f) {
		collectEntryPointsIfNeeded();
		entryPoints.add(f);
	}

	public void removeEntryPoint(IFile f) {
		collectEntryPointsIfNeeded();
		entryPoints.remove(f);
	}

	public Set<IFile> getEntryPoints() {
		collectEntryPointsIfNeeded();
		return entryPoints;
	}

	private void collectEntryPointsIfNeeded() {
		if (entryPoints == null) {
			entryPoints = new HashSet<IFile>();
			try {
				ResourcesPlugin.getWorkspace().getRoot().accept(new IResourceVisitor() {
					@Override
					public boolean visit(IResource resource) throws CoreException {
						if (resource instanceof IFile) {
							IFile file = (IFile) resource;
							if ("true".equalsIgnoreCase(file.getPersistentProperty(ENTRY_POINT_KEY))) {
								entryPoints.add(file);
							}
						}
						return true;
					}
				});
			} catch (CoreException e) {
				Debug.report(e);
			}
		}
	}

	private Set<OptionProviderListener> decorators = new HashSet<OptionProviderListener>();
	
	public void addDecorator(OptionProviderListener decorator) {
		decorators.add(decorator);
	}
	
	public void removeDecorator(OptionProviderListener decorator) {
		decorators.remove(decorator);
	}
	
	public void notifyDecorators() {
		for (OptionProviderListener d : decorators) {
			d.valuesChanged(null);
		}
	}
	
	private Set<ReconsultHook> currentHooks = new HashSet<ReconsultHook>();

	public void registerReconsultHook(ReconsultHook hook) {
		currentHooks.add(hook);
	}

	public void unregisterReconsultHook(ReconsultHook hook) {
		currentHooks.remove(hook);
	}
	
	public Set<ReconsultHook> getReconsultHooks() {
		return currentHooks;
	}
	
	public void notifyReconsultHooks(PrologInterface pif) {
		for (ReconsultHook r : currentHooks) {
			r.lastFileReconsulted(pif);
		}
	}
	
	// TODO: problem with quotes
	public void reconsultFiles(PrologInterface pif, boolean onlyEntryPoints) {
		Debug.debug("Reconsult files");
		List<String> consultedFiles = pif.getConsultedFiles();
		if (consultedFiles != null) {
			synchronized (consultedFiles) {
				
				String reconsultQuery = null;
				if (onlyEntryPoints) {
					reconsultQuery = createReconsultQueryEntryPoints(consultedFiles);
				} else {
					reconsultQuery = createReconsultQuery(consultedFiles);
				}
				
				try {
					pif.queryOnce(bT(PrologConnectorPredicates.PDT_RELOAD, "[" + reconsultQuery + "]"));
				} catch (PrologInterfaceException e) {
					Debug.report(e);
				}
				
				notifyReconsultHooks(pif);
			}
		}
	}

	private String createReconsultQueryEntryPoints(List<String> consultedFiles) {
		StringBuffer buf = new StringBuffer();
		boolean first = true;
		for (String fileName : consultedFiles) {
			try {
				IFile file = FileUtils.findFileForLocation(fileName);
				if(file == null){
					System.out.println("DEBUG");
					continue;
				}
				String isEntryPoint = file.getPersistentProperty(new QualifiedName("pdt", "entry.point"));

				if (isEntryPoint != null && isEntryPoint.equalsIgnoreCase("true")) {
					if (first) {
						first = false;
					} else {
						buf.append(", ");
					}
					buf.append(Util.quoteAtom(fileName));
					Debug.debug("reload " + fileName + ", because it was consulted before");
				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
		return buf.toString();
	}

	private String createReconsultQuery(List<String> consultedFiles) {

		StringBuffer buf = new StringBuffer();
		boolean first = true;
		for (String fileName : consultedFiles) {
			if (first) {
				first = false;
			} else {
				buf.append(", ");
			}
			buf.append(Util.quoteAtom(fileName));
			Debug.debug("reload " + fileName + ", because it was consulted before");
		}
		return buf.toString();
	}
}


