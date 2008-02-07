package org.cs3.pdt.core.internal.builder;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.cs3.pdt.core.IPrologProject;
import org.cs3.pdt.core.PDTCore;
import org.cs3.pdt.core.PDTCorePlugin;
import org.cs3.pdt.core.PDTCoreUtils;
import org.cs3.pdt.runtime.PrologRuntimePlugin;
import org.cs3.pdt.ui.util.UIUtils;
import org.cs3.pl.common.Debug;
import org.cs3.pl.cterm.CCompound;
import org.cs3.pl.cterm.CInteger;
import org.cs3.pl.cterm.CTerm;
import org.cs3.pl.prolog.AsyncPrologSession;
import org.cs3.pl.prolog.AsyncPrologSessionEvent;
import org.cs3.pl.prolog.DefaultAsyncPrologSessionListener;
import org.cs3.pl.prolog.IPrologEventDispatcher;
import org.cs3.pl.prolog.PLUtil;
import org.cs3.pl.prolog.PrologInterface2;
import org.cs3.pl.prolog.PrologInterfaceEvent;
import org.cs3.pl.prolog.PrologInterfaceException;
import org.cs3.pl.prolog.PrologInterfaceListener;
import org.cs3.pl.prolog.PrologSession;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.texteditor.MarkerUtilities;

public class UpdateMarkersJob extends Job implements PrologInterfaceListener {

	private final class _Listener extends DefaultAsyncPrologSessionListener {
		HashSet<Object> problemIds = new HashSet<Object>();

		@Override
		public void goalHasSolution(AsyncPrologSessionEvent e) {
			Map m = e.bindings;
			String id = (String)m.get("Id");
			String filename = (String) m.get("File");
			int start = Integer.parseInt(((String) m.get("Start")));
			int end = Integer.parseInt(((String) m.get("End")));
			String severity = (String) m.get("Severity");
			String message = (String) m.get("Msg");
			try {
				addMarker(id,filename, start, end, severity, message);
				if (markerMonitor == null) {
					markerMonitor = new SubProgressMonitor(monitor, 25,
							SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK);
					int work = Integer.parseInt((String) m.get("Count"));
					markerMonitor.beginTask("Creating Markers (" + tag + ")",
							work);
				}
				if (!problemIds.contains(id)) {
					problemIds.add(id);
					markerMonitor.worked(1);
				}
			} catch (CoreException e1) {
				UIUtils.logError(PDTCorePlugin.getDefault()
						.getErrorMessageProvider(), PDTCore.ERR_UNKNOWN,
						PDTCore.CX_UPDATE_MARKERS, e1);
			}
		}

		@Override
		public void goalSucceeded(AsyncPrologSessionEvent e) {
			if (markerMonitor != null) {
				markerMonitor.done();
			}
		}

		@Override
		public void goalFailed(AsyncPrologSessionEvent e) {
			// UIUtils.logAndDisplayError(PDTCorePlugin.getDefault().getErrorMessageProvider(),
			// UIUtils.getActiveShell(), PDTCore.ERR_QUERY_FAILED,
			// PDTCore.CX_UPDATE_MARKERS, new RuntimeException("Query failed:
			// "+e.query));
		}

		@Override
		public void goalRaisedException(AsyncPrologSessionEvent e) {
			if ("obsolete".equals(e.message)) {
				return;
			}
			UIUtils
					.logError(PDTCorePlugin.getDefault()
							.getErrorMessageProvider(), PDTCore.ERR_QUERY_FAILED,
							PDTCore.CX_UPDATE_MARKERS, new RuntimeException(
									"Goal raised exception: " + e.message
											+ "\n query: " + e.query
											+ "\n ticket: " + e.ticket));
		}
	}

	private final IPrologProject plProject;
	private IProgressMonitor monitor;
	private IProgressMonitor buildMonitor;
	private IProgressMonitor markerMonitor;
	private Runnable finnish;
	private String tag;

	public UpdateMarkersJob(IPrologProject plProject, String tag,
			Runnable finnish) {
		super("Updating Prolog Markers for project "
				+ plProject.getProject().getName());
		this.plProject = plProject;
		this.tag = tag;
		this.finnish = finnish;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		this.monitor = monitor;

		try {

			IPrologEventDispatcher dispatcher = PrologRuntimePlugin
					.getDefault().getPrologEventDispatcher(
							plProject.getMetadataPrologInterface());

			dispatcher.addPrologInterfaceListener(
					"builder(problems(workspace,'.'(" + tag + ",[])))", this);
			PrologInterface2 pif = ((PrologInterface2) plProject
					.getMetadataPrologInterface());
			final AsyncPrologSession s = pif.getAsyncSession();
			s.addBatchListener(new _Listener());
			monitor.beginTask("updating " + this.tag + " "
					+ s.getProcessorThreadAlias(), 100);
			buildMonitor = new SubProgressMonitor(monitor, 75,
					SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK);
			// buildMonitor.beginTask("Searching for problems", files.size());
			String query = "pdt_with_targets([problems(workspace,[" + tag
					+ "])],(pdt_problem_count(" + tag
					+ ",Count),pdt_problem(Id,File," + tag
					+ ",Start,End,Severity,Msg)))";
			s.queryAll("update_markers",
					query);
			while (!s.isIdle()) {
				if (UpdateMarkersJob.this.monitor.isCanceled()) {
					try {
						s.abort();
						UpdateMarkersJob.this.monitor.done();

					} catch (PrologInterfaceException e1) {
						Debug.rethrow(e1);
					}

				}
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {
					return UIUtils
							.createErrorStatus(PDTCorePlugin.getDefault()
									.getErrorMessageProvider(), e1,
									PDTCore.ERR_UNKNOWN);
				}
			}

			s.dispose();
			monitor.done();
			monitor = null;
			dispatcher.removePrologInterfaceListener("builder(interprete(_))",
					this);
		} catch (PrologInterfaceException e) {
			return UIUtils.createErrorStatus(PDTCorePlugin.getDefault()
					.getErrorMessageProvider(), e, PDTCore.ERR_PIF);
		} finally {

			finnish.run();
		}
		return new Status(IStatus.OK, PDTCore.PLUGIN_ID, "done");
	}

	private void addMarker(String id, String filename, int start, int end,
			String severity, String message) throws CoreException {
		IFile file = null;
		try {

			file = PDTCoreUtils.findFileForLocation(filename);

		} catch (IllegalArgumentException iae) {
			// ignore files that are not in the workspace.
			// Debug.report(iae);
			;
		} catch (IOException e) {
			Debug.rethrow(e);
		}
		if (file == null) {
			return;
		}
		IMarker marker = file.createMarker(PDTCore.PROBLEM);
		IDocument doc = PDTCoreUtils.getDocument(file);
		start = PDTCoreUtils.convertCharacterOffset(doc, start);
		end = Math
				.max(start + 1, PDTCoreUtils.convertCharacterOffset(doc, end));
		end = Math.min(doc.getLength(), end);

		MarkerUtilities.setCharStart(marker, start);
		MarkerUtilities.setCharEnd(marker, end);

		marker.setAttribute(IMarker.SEVERITY, mapSeverity(severity));
		marker.setAttribute(IMarker.MESSAGE, message);
		marker.setAttribute(PDTCore.PROBLEM_ID, id);
	}

	private int mapSeverity(String severity) {
		if ("error".equals(severity)) {
			return IMarker.SEVERITY_ERROR;
		}
		if ("warning".equals(severity)) {
			return IMarker.SEVERITY_WARNING;
		}
		if ("info".equals(severity)) {
			return IMarker.SEVERITY_INFO;
		}

		throw new IllegalArgumentException("cannot map severity constant: "
				+ severity);
	}

	public void update(PrologInterfaceEvent e) {
		if (buildMonitor == null) {
			return;
		}
		if ("expensive".equals(tag)) {
			System.out.println(e.getSubject() + " <-- " + e.getEvent());
		}
		/*
		 * if (!e.getSubject().equals("builder(problems(workspace))")) { return; }
		 */
		if (e.getEvent().equals("done")) {
			buildMonitor.done();
			return;
		}

		CTerm term = PLUtil.createCTerm(e.getEvent());
		if (!(term instanceof CCompound)) {
			Debug.warning("wunder, wunder: term ist kein Compound:"
					+ e.getEvent());
			Debug.warning("Subject: " + e.getSubject() + " , Event: "
					+ e.getEvent());
			return;
		}
		CCompound event = (CCompound) term;
		CTerm argTerm = event.getArgument(0);
		if (!(argTerm instanceof CInteger)) {
			Debug.warning("wunder, wunder: argterm ist kein Integer:"
					+ PLUtil.renderTerm(argTerm));
			Debug.warning("Subject: " + e.getSubject() + " , Event: \""
					+ e.getEvent() + "\"");
			return;
		}
		int arg = ((CInteger) argTerm).getIntValue();
		String functor = event.getFunctorValue();
		// Debug.debug("progress: "+functor+", "+arg);
		if (functor.equals("estimate")) {
			buildMonitor.beginTask("Searching for Problems (" + tag + ")", arg);
			return;
		}
		if (functor.equals("worked")) {
			buildMonitor.worked(arg);
			return;
		}
	}

}
