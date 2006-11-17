package org.cs3.jtransformer;

import org.cs3.jtransformer.util.JTUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;

public class JTransformerProjectChangeListener implements
		IResourceChangeListener {

	/**
	 * @see IResourceChangeListener#resourceChanged(org.eclipse.core.resources.IResourceChangeEvent)
	 */
	public void resourceChanged(IResourceChangeEvent event) {

		IResourceDelta root = event.getDelta();

		// Get all the affected Children. One of them would be the newly

		// created project
		if (root != null) {
			IResourceDelta[] projectDeltas = root.getAffectedChildren();

			for (int i = 0; i < projectDeltas.length; i++) {

				// Get individual delta's

				IResourceDelta delta = projectDeltas[i];

				IResource resource = delta.getResource();

				if (resource instanceof IProject) {
					IProject project = (IProject) resource;
					try {
						if (delta.getFlags() == IResourceDelta.OPEN) {
							if (!project.isOpen()) {
								removeNatureIfAssigned(project);
							} else {
								if (project.hasNature(JTransformer.NATURE_ID)) {
									JTransformerPlugin.getNature(project);
								}
							}
						} else if (delta.getKind() == IResourceDelta.REMOVED) {
							removeNatureIfAssigned(project);
						}
					} catch (CoreException e) {
						 JTUtils.logAndDisplayUnknownError(e);
					}
				}

			}
		} 
	}

	private void removeNatureIfAssigned(IProject project) throws CoreException {
		if (JTransformerPlugin.getNatureIfAvailable(project) != null) {
			JTransformerPlugin.getNatureIfAvailable(project).onClose();
			JTransformerPlugin.removeNatureFromRegistry(project);
		}
	}

}
