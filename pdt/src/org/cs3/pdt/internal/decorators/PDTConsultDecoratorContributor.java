package org.cs3.pdt.internal.decorators;

import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import org.cs3.pdt.PDTPlugin;
import org.cs3.pdt.PDTUtils;
import org.cs3.pdt.internal.ImageRepository;
import org.cs3.pdt.ui.util.UIUtils;
import org.cs3.pl.common.OptionProviderEvent;
import org.cs3.pl.common.OptionProviderListener;
import org.cs3.pl.common.Util;
import org.cs3.pl.prolog.PrologInterface;
import org.cs3.pl.prolog.PrologInterfaceException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;

public class PDTConsultDecoratorContributor implements ILightweightLabelDecorator, OptionProviderListener {

	private Vector<ILabelProviderListener> listeners = new Vector<ILabelProviderListener>();

	@Override
	public void addListener(ILabelProviderListener l) {

		synchronized (listeners) {
			if(!listeners.contains(l)){
				listeners.add(l);
			}
		}
		
	}

	@Override
	public void dispose() {
	}

	@Override
	public boolean isLabelProperty(Object element, String property) {
		return false;
	}

	@Override
	public void removeListener(ILabelProviderListener l) {
		synchronized (listeners) {
			if(listeners.contains(l)){
				listeners.remove(l);
			}
		}
		
	}

	@Override
	public void decorate(Object element, IDecoration decoration) {
		if(!(element instanceof IFile) && !(element instanceof IFolder)){
			return;
		}

		PDTPlugin.getDefault().addDecorator(this);

		// get active pif from console
		PrologInterface currentPif = PDTUtils.getActiveConsolePif();

		if (currentPif == null) {
			if (element instanceof IFile) {
				decoration.addOverlay(ImageRepository.getImageDescriptor(ImageRepository.PROLOG_FILE_UNCONSULTED), IDecoration.UNDERLAY);
			}
			return;
		}
		
		if (element instanceof IFile) {
			IFile file = (IFile) element;
			
//			final DecorationContext decorationContext = (DecorationContext) decoration.getDecorationContext();
//			decorationContext.putProperty(IDecoration.ENABLE_REPLACE, Boolean.TRUE);

			// check if file is in consulted files list (important for qlf files)
			String prologFileName = getPrologFileName(file);
			
			// XXX: don't mark qlf file if only the pl file is consulted 
			prologFileName = prologFileName.replace(".qlf'", ".pl'");

			// check if file is source_file
			try {
				Map<String, Object> result = currentPif.queryOnce("source_file(" + prologFileName + ")");
				if (result != null) {
					decoration.addSuffix(" [consulted]");
					if (file.getFileExtension().equalsIgnoreCase("QLF")) {
						decoration.addOverlay(ImageRepository.getImageDescriptor(ImageRepository.QLF_FILE_CONSULTED), IDecoration.BOTTOM_LEFT);
					} else {
						decoration.addOverlay(ImageRepository.getImageDescriptor(ImageRepository.PROLOG_FILE_CONSULTED), IDecoration.UNDERLAY);
					}
				} else {
					decoration.addOverlay(ImageRepository.getImageDescriptor(ImageRepository.PROLOG_FILE_UNCONSULTED), IDecoration.UNDERLAY);
				}
			} catch (PrologInterfaceException e) {
				e.printStackTrace();
				decoration.addOverlay(ImageRepository.getImageDescriptor(ImageRepository.PROLOG_FILE_UNCONSULTED), IDecoration.UNDERLAY);
			}
		} else {
			IFolder folder = (IFolder) element;
			String prologFilename = Util.prologFileName(folder.getRawLocation().toFile());
			
			try {
				Map<String, Object> result = currentPif.queryOnce("source_file(F), atom_concat('" + prologFilename + "', _, F)");
				if (result != null) {
					decoration.addOverlay(ImageRepository.getImageDescriptor(ImageRepository.PROLOG_FOLDER_CONSULTED), IDecoration.TOP_LEFT);
				}
			} catch (PrologInterfaceException e) {
				e.printStackTrace();
			}
		}
	}
	
	private String getPrologFileName(IFile file) {
		String enclFile = file.getRawLocation().toPortableString();
		if (Util.isWindows()) {
			enclFile = enclFile.toLowerCase();
		}

		IPath filepath = new Path(enclFile);
		return "'" + Util.prologFileName(filepath.toFile()) + "'";
	}

	@Override
	public void valuesChanged(OptionProviderEvent e) {
		fireLabelProviderChanged();
	}

	private void fireLabelProviderChanged() {
		final LabelProviderChangedEvent e = new LabelProviderChangedEvent(this);
		Vector<ILabelProviderListener> clone=new Vector<ILabelProviderListener>();
		synchronized(listeners){
			clone.addAll(listeners);
		}
		for (Iterator<ILabelProviderListener> it = clone.iterator(); it.hasNext();) {
			final ILabelProviderListener l = (ILabelProviderListener) it.next();
			UIUtils.getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					l.labelProviderChanged(e);
				}
			});
		}
	}

}