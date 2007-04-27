package org.cs3.jtransformer.internal.actions;

import java.util.Map;

import org.cs3.jtransformer.JTDebug;
import org.cs3.jtransformer.util.JTUtils;
import org.cs3.pdt.console.PrologConsolePlugin;
import org.cs3.pdt.ui.util.UIUtils;
import org.cs3.pl.prolog.PrologInterfaceException;
import org.cs3.pl.prolog.PrologSession;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;


public class OpenSelectionInEditorAction extends ConsoleSelectionAction{

//	/**
//	 * @throws CoreException
//	 * @throws PrologInterfaceException 
//	 */
//	static PrologSession getPrologSession() throws CoreException, PrologInterfaceException {
//		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
//		JTransformerNature nature = null;
//		for (int i = 0; i < projects.length; i++) {
//			if(projects[i].isAccessible() && projects[i].hasNature(JTransformer.NATURE_ID)){
//				nature = JTransformerPlugin.getNature(projects[i]);
//				break;
//			}
//		}
//		if(nature == null)
//			return null;
//		return nature.getPrologInterface().getSession();
//	}
	
	public void run(IAction action) {
		PrologSession session = null;
		try {
			session = PrologConsolePlugin.getDefault().getPrologConsoleService().getActivePrologConsole().getPrologInterface().getSession();
			Map result = session.queryOnce("sourceLocation(" + getPefId()
					+ ", File, Start, Length)");
			if (result == null) {
				result = session.queryOnce("tree(" + getPefId()
						+ ", _,Kind)");
				if (result == null) {
					UIUtils.displayErrorDialog(JTUtils.getShell(true), "JTransformer", 
						"'" + getPefId()
								+ "' is not a tree element.");
				} else {
					String kind = (String)result.get("Kind");
					result = session.queryOnce("enclClass(" + getPefId()
							+ ", EnclClass), fullQualifiedName(EnclClass,FQN)");
					if(result == null) {
						UIUtils.displayErrorDialog(JTUtils.getShell(true), "JTransformer", 
								"Could not find the source location for the tree '" + getPefId() + 
								"' of type " + kind + ".\n");
					} else {
						UIUtils.displayErrorDialog(JTUtils.getShell(true), "JTransformer", 
								"Could not find the source location for the tree '" + getPefId() + 
								"'.\nEither its enclosing class '" + result.get("FQN") + "' is an external byte code class or" +
								     "no the tree was generated. Latter could be the case it the tree was generated by a transformation or " +
								     " it is part of an implicit default constructor.");
						
					}

				}
				
			}
			else {
				String filename = result.get("File").toString();
				int start = Integer.parseInt(result.get("Start").toString());
				int length = Integer.parseInt(result.get("Length").toString());
				JTUtils.selectInEditor(start, length, filename);
			}
		} catch (CoreException e) {
			JTDebug.report(e);
		} catch (PrologInterfaceException e)
		{
			JTDebug.report(e);
		} finally {
			if(session != null) {
				session.dispose();
			}
		}
	}


}
