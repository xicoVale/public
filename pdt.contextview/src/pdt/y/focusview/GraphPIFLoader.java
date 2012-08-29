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

package pdt.y.focusview;

import static org.cs3.prolog.common.QueryUtils.bT;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import org.cs3.prolog.common.ResourceFileLocator;
import org.cs3.prolog.common.Util;
import org.cs3.prolog.common.logging.Debug;
import org.cs3.prolog.connector.PrologInterfaceRegistry;
import org.cs3.prolog.connector.PrologRuntimePlugin;
import org.cs3.prolog.connector.Subscription;
import org.cs3.prolog.connector.ui.PrologRuntimeUIPlugin;
import org.cs3.prolog.pif.PrologException;
import org.cs3.prolog.pif.PrologInterface;
import org.cs3.prolog.pif.PrologInterfaceException;
import org.cs3.prolog.session.PrologSession;

import pdt.y.main.PDTGraphView;

public class GraphPIFLoader {
	private static final String FILE_TO_CONSULT = "pl_ast_to_graphML";
	private static final String PATH_ALIAS = "pdt_builder_graphml_creator";
	private static final String NAME_OF_HELPING_FILE = "pdt-focus-help.graphml";

	private File helpFile;
	private PDTGraphView view;
	private PrologInterface pif;
	private ExecutorService executor = Executors.newCachedThreadPool();
	private List<String> dependencies = new ArrayList<String>();

	public GraphPIFLoader(PDTGraphView view) {
		this.view = view;
		PrologRuntimeUIPlugin plugin = PrologRuntimeUIPlugin.getDefault();
		ResourceFileLocator locator = plugin.getResourceLocator();
		helpFile = locator.resolve(NAME_OF_HELPING_FILE);
	}

	public List<String> getDependencies() {
		return dependencies;
	}

	public void queryPrologForGraphFacts(String focusFileForParsing) {

		String prologNameOfFileToConsult = PATH_ALIAS + "(" + FILE_TO_CONSULT + ")";

		try {
			pif = getActivePifEnsuringFocusViewSubscription();
			if (pif != null) {

				String query = "consult(" + prologNameOfFileToConsult + ").";
				sendQueryToCurrentPiF(query);

				sendQueryToCurrentPiF(bT("ensure_generated_factbase_for_source_file", Util.quoteAtom(focusFileForParsing)));

				query = "write_focus_to_graphML('" + focusFileForParsing
						+ "','" + Util.prologFileName(helpFile)
						+ "', Dependencies).";
				Map<String, Object> output = sendQueryToCurrentPiF(query);

				dependencies.clear();
				if (output != null) {
					@SuppressWarnings("unchecked")
					Vector<String> deps = (Vector<String>) output.get("Dependencies");
					dependencies.addAll(deps);
				}

				// query =
				// "collect_ids_for_focus_file(FocusId,Files,CalledPredicates,Calls)";
				// Map<String, Object> result = sendQueryToCurrentPiF(query);
				// result.get("FocusId");

				FutureTask<?> futureTask = new FutureTask<Object>(
						new Runnable() {
							@Override
							public void run() {
								try {
									view.loadGraph(helpFile.toURI().toURL());
								} catch (MalformedURLException e) {
									Debug.rethrow(e);
								}
							};
						}, null);
				executor.execute(futureTask);
			}
		} catch (PrologException e1) {
			e1.printStackTrace();
		} catch (PrologInterfaceException e) {
			e.printStackTrace();
		}
	}
	
	public Map<String, Object> sendQueryToCurrentPiF(String query)
		throws PrologInterfaceException {
	
		PrologSession session = pif.getSession(PrologInterface.LEGACY);
		Map<String, Object> result = session.queryOnce(query);
		return result;
	}

	public PrologInterface getActivePifEnsuringFocusViewSubscription() {
		PrologInterface pif = PrologRuntimeUIPlugin.getDefault().getPrologInterfaceService().getActivePrologInterface();
		if (pif == null) {
			return null;
		}
		PrologInterfaceRegistry pifRegistry = PrologRuntimePlugin.getDefault()
				.getPrologInterfaceRegistry();
		String pifKey = pifRegistry.getKey(pif);
		Set<Subscription> subscriptions = pifRegistry
				.getSubscriptionsForPif(pifKey);

		if (ownSubscriptionMissing(subscriptions)) {
			FocusViewSubscription mySubscription = FocusViewSubscription
					.newInstance(pifKey);
			pif = PrologRuntimeUIPlugin.getDefault().getPrologInterface(
					mySubscription);
		}
		return pif;
	}

	private boolean ownSubscriptionMissing(Set<Subscription> subscriptions) {
		for (Subscription subscription : subscriptions) {
			if (subscription instanceof FocusViewSubscription)
				return false;
		}
		return true;
	}
}

