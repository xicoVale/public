package org.cs3.jtransformer.internal.natures;

import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import org.cs3.jtransformer.JTransformer;
import org.cs3.jtransformer.JTransformerPlugin;
import org.cs3.jtransformer.JTransformerProject;
import org.cs3.jtransformer.JTransformerProjectEvent;
import org.cs3.jtransformer.JTransformerProjectListener;
import org.cs3.jtransformer.internal.builders.FactBaseBuilder;
import org.cs3.jtransformer.regenerator.ISourceRegenerator;
import org.cs3.jtransformer.regenerator.SourceCodeRegenerator;
import org.cs3.jtransformer.util.JTUtils;
import org.cs3.pdt.runtime.PrologInterfaceRegistry;
import org.cs3.pdt.runtime.PrologRuntimePlugin;
import org.cs3.pdt.ui.util.UIUtils;
import org.cs3.pl.common.Debug;
import org.cs3.pl.common.Option;
import org.cs3.pl.common.OptionProviderListener;
import org.cs3.pl.common.SimpleOption;
import org.cs3.pl.common.Util;
import org.cs3.pl.prolog.AsyncPrologSession;
import org.cs3.pl.prolog.PrologException;
import org.cs3.pl.prolog.PrologInterface;
import org.cs3.pl.prolog.PrologInterfaceException;
import org.cs3.pl.prolog.PrologSession;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.JavaProject;

/**
 * @see IProjectNature
 */
public class JTransformerProjectNature implements IProjectNature,
		JTransformerProject, JTransformerProjectListener {

	boolean buildTriggered = false;
	

	// The project for which this nature was requested,
	// see IProject.getNature(String)
	IProject project;

	private Option[] options;

	private FactBaseBuilder builder;

	private Vector listeners = new Vector();

	private Vector optionsListener = new Vector();
	
	private Object pefInitializationMonitor = new Object();

	private boolean pefInitializationDone = false;


	static private Map projectNatureMap = new Hashtable();

	/**
	 * 
	 */
	public JTransformerProjectNature() {
		Debug.info("TJ: JTNature ID: " + this.hashCode());
	}

	/**
	 * You MUST NOT directly or indirectly retrieve this nature via the
	 * IProject.getNature(..) method. This will instantiate this class again and
	 * will at least result in a second execution of configure, getProject and
	 * the afterInit hook will be called twice on the first on nature
	 * instantiated.
	 * 
	 * @see IProjectNature#configure
	 * 
	 */
	public void configure() throws CoreException {

			Debug.debug("configure was called");
			IProjectDescription descr = project.getDescription();
			ICommand sheepBuilder = descr.newCommand();
			sheepBuilder.setBuilderName(JTransformer.BUILDER_ID);
			ICommand builders[] = descr.getBuildSpec();
			for (int i = 0; i < builders.length; i++) {
				if (builders[i].getBuilderName()
						.equals(JTransformer.BUILDER_ID)) {
					return;
				}
			}
			ICommand newBuilders[] = new ICommand[builders.length + 1];
			System.arraycopy(builders, 0, newBuilders, 0, builders.length);
			newBuilders[builders.length] = sheepBuilder;
			descr.setBuildSpec(newBuilders);

			buildTriggered = true;
			project.setDescription(descr, null);
			try {
				if (pif.isDown()) {
					pif.start();
				}
			} catch (PrologInterfaceException e1) {
				JTransformerPlugin.getDefault()
						.createPrologInterfaceExceptionCoreExceptionWrapper(e1);
			}


	}

	public IClasspathEntry getFirstSourceFolder(IJavaProject javaProject)
			throws JavaModelException {
		IClasspathEntry[] cp = javaProject.getResolvedClasspath(true);
		for (int i = 0; i < cp.length; i++) {
			if (cp[i].getEntryKind() == IClasspathEntry.CPE_SOURCE) {
				return cp[i];
			}
		}
		return null;
	}

	private String getRuntimePrologInterfaceKey() {
		return getPreferenceValue(JTransformer.PROLOG_RUNTIME_KEY, project
				.getName());
	}

	/**
	 * 
	 */
	private void initOptions() {
		try {

			String outputProject = JTUtils.getOutputProjectName(project);

			JavaProject javaProject = (JavaProject) project
					.getNature(JavaCore.NATURE_ID);
			IClasspathEntry firstSourceFolderCP = getFirstSourceFolder(javaProject);
			String firstSourceFolder;
			if (firstSourceFolderCP != null) {
				firstSourceFolder = firstSourceFolderCP.getPath()
						.removeFirstSegments(1).toPortableString();
			} else {
				firstSourceFolder = "dummySrc"; // FIXME: TRHO: Was tun?
			}

			options = new Option[] {

					new SimpleOption(
							JTransformer.PROLOG_RUNTIME_KEY,
							"Key for sharing prolog instance",
							"The key identifying the PrologInterface instance that"
									+ "will be used by default to consult prolog files into it. Any occurance of the string %project% will be"
									+ " replaced with the project name.",
							Option.STRING, project.getName()),
					new SimpleOption(
							JTransformer.PROP_OUTPUT_PROJECT,
							"Output Project",
							"The project to which generated sourcecode is written."
									+ "This option is ignored if JTransformer is operating in place.",
							Option.STRING, outputProject),
					new SimpleOption(
							JTransformer.PROP_OUTPUT_FOLDER,
							"Output source folder",
							"The source folder to which generated sourcecode is written."
									+ "This should be a relative path - it is eather appended to the"
									+ "path of the output project, or - if JTransformer operates inplace -"
									+ " to that of the original project",
							Option.STRING, firstSourceFolder),
					new SimpleOption(
							JTransformer.PROP_INPLACE,
							"Try inplace operation",
							"NOT IMPLEMENTED YET! \nIf this option is set, changes "
									+ "to existing source files will be performed in-place."
									+ "Source files for which the respective PEFs where "
									+ "deleted, are removed. \n"
									+ "Newly created source files however "
									+ "will still be created in the specified output folder.",
							Option.FLAG, "false"),
					new SimpleOption(
							JTransformer.PROP_PEF_STORE_FILE,
							"PEF store file",
							"The file where PEFs for this project should be stored",
							Option.FILE,
							JTransformerPlugin.getDefault().getPreferenceValue(
									JTransformer.PREF_DEFAULT_PEF_STORE_FILE,
									null)),

			};
		} catch (CoreException e) {
			e.printStackTrace();

		}

	}

	/**
	 * @see IProjectNature#deconfigure
	 */
	public void deconfigure() throws CoreException {
		
		// just to make sure waiting threads are notified
		onClose();

		IProjectDescription descr = project.getProject().getDescription();
		Debug.debug("deconfigure was called");
		ICommand builders[] = descr.getBuildSpec();
		int index = -1;
		for (int i = 0; i < builders.length; i++) {
			if (builders[i].getBuilderName().equals(JTransformer.BUILDER_ID)) {
				index = i;
				break;
			}
		}
		if (index != -1) {
			ICommand newBuilders[] = new ICommand[builders.length - 1];
			System.arraycopy(builders, 0, newBuilders, 0, index);
			System.arraycopy(builders, index + 1, newBuilders, index,
					builders.length - index - 1);
			descr.setBuildSpec(newBuilders);
		}
			PrologInterface pif = getPrologInterface();
			PrologSession s = null;
	
			try {
				if (pif.isUp()) {
					s = pif.getSession();
					String projectName = getProject().getName();
					String sourceFolder = Util.prologFileName(new File(
							getPreferenceValue(JTransformer.PROP_OUTPUT_FOLDER,
									null)));
					s.queryOnce("retractall(project_option('" + projectName
							+ "', _))");
					getFactBaseBuilder().clean(null);
				}
			} catch (PrologInterfaceException e) {
				JTransformerPlugin.getDefault()
						.createPrologInterfaceExceptionCoreExceptionWrapper(e);
			} finally {
				if (s != null)
					s.dispose();
			}
	}

	public void onClose() {
		pefInitializationDone();
		
		removeNatureFromRegistry();
		
		PrologInterfaceRegistry reg = PrologRuntimePlugin.getDefault()
		.getPrologInterfaceRegistry();
		reg.removeSubscription(pifSubscription);
	}

	private void removeNatureFromRegistry() {
		synchronized (projectNatureMap) {
			projectNatureMap.remove(project);
		}		
	}

	/**
	 * @see IProjectNature#getProject
	 */
	public IProject getProject() {
		return project;
	}

	/**
	 * @see IProjectNature#setProject
	 */
	public void setProject(IProject project) {
		this.project = project;
		checkIfNatureIsAlreadyAssignedToProject(project);
		try {
			JTransformerPlugin.getDefault().setNonPersistantPreferenceValue(project,
					JTransformer.FACTBASE_STATE_KEY,
					JTransformer.FACTBASE_STATE_ACTIVATED);
		} catch (CoreException e) {
			UIUtils.logAndDisplayError(JTransformerPlugin.getDefault()
					.getErrorMessageProvider(), UIUtils.getDisplay()
					.getActiveShell(), JTransformer.ERR_UNKNOWN,
					JTransformer.ERR_CONTEXT_EXCEPTION, e);
		}
		getPrologInterface();
//		if (pif.isUp()) {
//			reconfigure();
//		}

	}

	private void checkIfNatureIsAlreadyAssignedToProject(IProject project) {
		synchronized (projectNatureMap) {
			if(projectNatureMap.containsKey(project)) {
				projectNatureMap.remove(project);
				if (pifSubscription != null) {
					PrologInterfaceRegistry reg = PrologRuntimePlugin.getDefault()
					.getPrologInterfaceRegistry();
					reg.removeSubscription(pifSubscription);
				}
				RuntimeException ex = new RuntimeException(
						"Internal error: second creation of a JT nature for the project "+project.getName());
				JTUtils.logAndDisplayUnknownError(ex);
				throw ex;
			}
			projectNatureMap.put(project,this);
		}
		
	}

	PrologInterface pif = null;

	Object pifMonitor = new Object();

	JTransformerSubscription pifSubscription;

	private boolean initializingPif = false;

	private void pifKeysChanged() {
		PrologInterfaceRegistry reg = PrologRuntimePlugin.getDefault()
				.getPrologInterfaceRegistry();
		if (pifSubscription != null) {
			synchronized (pifMonitor) {
				reg.removeSubscription(pifSubscription);
				pif = null;
				Job j = new Job("building PEFs for project "
						+ project.getName()) {
					protected IStatus run(IProgressMonitor monitor) {
						try {
							getFactBaseBuilder().clean(monitor);
							project.build(IncrementalProjectBuilder.FULL_BUILD,
									monitor);
						} catch (OperationCanceledException opc) {
							return Status.CANCEL_STATUS;
						} catch (CoreException e) {
							return new Status(Status.ERROR,
									JTransformer.PLUGIN_ID, -1,
									"exception caught during build", e);
						} catch (PrologInterfaceException e) {
							Debug.report(e);
						}
						return Status.OK_STATUS;
					}

					public boolean belongsTo(Object family) {
						return family == ResourcesPlugin.FAMILY_MANUAL_BUILD;
					}

				};
				j.setRule(ResourcesPlugin.getWorkspace().getRoot());
				j.schedule();
			}
		}

	}

	public PrologInterface getPrologInterface() {
		synchronized (pifMonitor) {
			if (pif  != null) {
				return pif;
			}
			if (isInitializingPif()) {
				throw new RuntimeException(
						"nested call to JTransformerProjectNature.getPrologInterface()");
			}
			setInitializingPif(true);
			String projectInterfaceKey = getRuntimePrologInterfaceKey();
			String id = "JTransformerSubscription_" + project.getName();
			pifSubscription = new JTransformerSubscription(this,project, id,
					projectInterfaceKey,
					"JTransformer factbase of project " + getProject().getName(), "JTransformer");
			pif = PrologRuntimePlugin.getDefault().getPrologInterface(
					pifSubscription);
			
			setInitializingPif(false);
			return pif;
		}
	}

	private void setInitializingPif(boolean b) {
		initializingPif = b;
	}

	private boolean isInitializingPif() {
		return initializingPif ;
	}

	/**
	 * @return Returns the builder.
	 */
	public FactBaseBuilder getFactBaseBuilder() {
		if (builder == null) {
			builder = new FactBaseBuilder(this);
			builder.addJTransformerProjectListener(this);
		}
		return builder;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.cs3.jtransformer.JTransformerProject#addJTransformerProjectListener(org.cs3.jtransformer.JTransformerProjectListener)
	 */
	public void addJTransformerProjectListener(JTransformerProjectListener l) {
		synchronized (listeners) {
			if (!listeners.contains(l)) {
				listeners.add(l);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.cs3.jtransformer.JTransformerProject#removeJTransformerProjectListener(org.cs3.jtransformer.JTransformerProjectListener)
	 */
	public void removeJTransformerProjectListener(JTransformerProjectListener l) {
		synchronized (listeners) {
			if (listeners.contains(l)) {
				listeners.remove(l);
			}
		}
	}

	protected void fireFactBaseUpdated() {
		final JTransformerProjectNature currentProject = this;

		Runnable r = new Runnable() {
			public void run() {
				JTransformerProjectEvent e = new JTransformerProjectEvent(
						currentProject);

				JTransformerPlugin.getDefault().fireFactBaseUpdated(e);

				Vector cloned = null;
				synchronized (listeners) {
					cloned = (Vector) listeners.clone();
				}
				for (Iterator it = cloned.iterator(); it.hasNext();) {
					JTransformerProjectListener l = (JTransformerProjectListener) it
							.next();
					l.factBaseUpdated(e);
				}

			}
		};
		Thread t = new Thread(r, "JTransformerProject listener notification");
		t.setDaemon(true);
		t.start();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.cs3.jtransformer.JTransformerProject#generateFacts(org.eclipse.jdt.core.ICompilationUnit,
	 *      java.io.PrintStream)
	 */
	public void generateFacts(AsyncPrologSession session, ICompilationUnit cu)
			throws IOException, CoreException, PrologInterfaceException {
		getFactBaseBuilder().writeFacts(session, cu);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.cs3.jtransformer.JTransformerProjectListener#factBaseUpdated(org.cs3.jtransformer.JTransformerProjcetEvent)
	 */
	public void factBaseUpdated(JTransformerProjectEvent e) {
		fireFactBaseUpdated();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.cs3.jtransformer.JTransformerProject#getOptions()
	 */
	public Option[] getOptions() {
		if (options == null) {
			initOptions();
		}
		return options;
	}

	/**
	 * look up a preference value.
	 * <p>
	 * tries the following values in the given order and returns the first
	 * non-null result. If everything returns null, the given defaultValue is
	 * returned.
	 * <ul>
	 * <li>System.getProperty(key)</li>
	 * <li>getProject().getPersistentProperty(key)</li>
	 * <li>if an option with the given id exists in the array returned by
	 * getOptions(), take its default value</li>
	 * <li>the given default value
	 * </ul>
	 * 
	 * @param key
	 * @return the value or specified default if no such key exists..
	 * @throws CoreException
	 */
	public String getPreferenceValue(String key, String defaultValue) {
		String value = System.getProperty(key);
		if (value != null) {
			return value;
		}
		try {
			value = getProject().getPersistentProperty(
					new QualifiedName("", key));
		} catch (CoreException e) {
			Debug.report(e);
			throw new RuntimeException(e);
		}
		if (value != null) {
			return value;
		}
		Option[] o = getOptions();
		for (int i = 0; i < o.length; i++) {
			if (o[i].getId().equals(key)) {
				return o[i].getDefault();
			}
		}
		return defaultValue;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.cs3.jtransformer.JTransformerProject#reconfigure()
	 */
	public void reconfigure(PrologSession s) throws PrologException,
			PrologInterfaceException {
		
		
//		try {
//			// CreateOutdirUtils.getInstance().createOutputProject(project);
//			JavaProject javaProject = (JavaProject) project
//					.getNature(JavaCore.NATURE_ID);
//
//		} catch (CoreException e) {
//			Debug.report(e);
//		}
		// String projectName = getProject().getName();
		//		
		// String outputFolder = Util.prologFileName(new File(
		// getPreferenceValue(JTransformer.PROP_OUTPUT_FOLDER, null)));
		// String outputProject = Util.prologFileName(new File(
		// getPreferenceValue(JTransformer.PROP_OUTPUT_PROJECT, null)));
		// boolean inplace = Boolean.valueOf(
		// Util.prologFileName(new File(getPreferenceValue(
		// JTransformer.PROP_INPLACE, "false")))).booleanValue();
		//
		// s.queryOnce("retractall(project_option('" + projectName + "'))");
		// s.queryOnce("assert(project_option('" + projectName
		// + "', output_project('" + outputProject + "')))");
		// s.queryOnce("assert(project_option('" + projectName
		// + "', output_folder('" + outputFolder + "')))");
		// if (inplace) {
		// s.queryOnce("assert(project_option('" + projectName
		// + "',inplace))");
		// }

	}



	static private String quote(String str) {
		return "'" + str + "'";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.cs3.jtransformer.JTransformerProject#reconfigure()
	 */
	public void reconfigure() {
//		PrologSession s = null;
//		try {
//			s = getPrologInterface().getSession();
//			reconfigure(s);
//		} catch (PrologException e) {
//			Debug.report(e);
//			throw e;
//		} catch (PrologInterfaceException e) {
//			UIUtils.logAndDisplayError(JTransformerPlugin.getDefault()
//					.getErrorMessageProvider(), UIUtils.getDisplay()
//					.getActiveShell(),
//					JTransformer.ERR_PROLOG_INTERFACE_EXCEPTION,
//					JTransformer.ERR_CONTEXT_NATURE_INIT, e);
//		} finally {
//			if (s != null)
//				s.dispose();
//		}
		try {
			getPrologInterface().stop();
			getPrologInterface().start();
		} catch (PrologInterfaceException e) {
			UIUtils.logAndDisplayError(JTransformerPlugin.getDefault()
					.getErrorMessageProvider(), UIUtils.getDisplay()
					.getActiveShell(),
					JTransformer.ERR_PROLOG_INTERFACE_EXCEPTION,
					JTransformer.ERR_CONTEXT_NATURE_INIT, e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.cs3.jtransformer.JTransformerProject#getSourceRegenerator()
	 */
	public ISourceRegenerator getSourceRegenerator() {
		try {
			return new SourceCodeRegenerator(getPrologInterface());
		} catch (IOException e) {
			Debug.report(e);
			throw new RuntimeException(e);
		}
	}

	public void setPreferenceValue(String id, String value) {
		try {
			JTransformerPlugin.getDefault().setPreferenceValue(getProject(),
					id, value);
			if (id.equals(JTransformer.PROLOG_RUNTIME_KEY)) {
				pifKeysChanged();
			}
		} catch (CoreException e) {
			Debug.report(e);
			throw new RuntimeException(e);
		}

	}

	/**
	 * @param project
	 * @return
	 * @throws CoreException
	 */
	static public void removeJTransformerNature(IProject project)
			throws CoreException {
		if (project.hasNature(JTransformer.NATURE_ID)) {
			IProjectDescription ipd = project.getDescription();
			String[] oldNIDs = ipd.getNatureIds();
			String[] newNIDs;
			newNIDs = new String[oldNIDs.length - 1];
			int j = 0;
			for (int i = 0; i < newNIDs.length; i++) {
				if (oldNIDs[j].equals(JTransformer.NATURE_ID))
					j++;
				newNIDs[i] = oldNIDs[j];
				j++;
			}
			ipd.setNatureIds(newNIDs);
			if (!project.isSynchronized(IResource.DEPTH_ONE)) {
				project.refreshLocal(IResource.DEPTH_ONE, null);
			}
			project.setDescription(ipd, null);// TODO: add real
		}
	}

	public void addOptionProviderListener(OptionProviderListener l) {
		synchronized (optionsListener) {
			if (!optionsListener.contains(l)) {
				optionsListener.add(l);
			}
		}
	}

	public void removeOptionProviderListener(OptionProviderListener l) {
		synchronized (optionsListener) {
			if (optionsListener.contains(l)) {
				optionsListener.remove(l);
			}
		}

	}

	public void pefInitializationDone() {
		Debug.info("pefInitializationDone before monitor: " + project.getName());
		synchronized (pefInitializationMonitor) {
			pefInitializationDone = true;
			pefInitializationMonitor.notifyAll();
		}
		Debug.info("pefInitializationDone after monitor: " + project.getName());

	}
	
	public void ensurePefInitializationFinished() throws InterruptedException {
		
		synchronized (pefInitializationMonitor) {
			if(pefInitializationDone) {
				return;
			} else {
				pefInitializationMonitor.wait();
			}
		}
		
	}


}