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

package org.cs3.prolog.ui.util;

import org.cs3.prolog.common.Option;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

public class FileListEditorAdapter extends OptionEditor {

	FileListEditor editor;
	
	
	public FileListEditorAdapter(Composite parent, Option option) {
		super(parent, option);
		editor=new FileListEditor(option.getId(),option.getLabel(),"Add File...",parent);
		boolean relative = "true".equals(option.getHint(UIUtils.RELATIVE));
		editor.setRelative(relative);
		boolean wsResource = "true".equals(option.getHint(UIUtils.IS_WORKSPACE_RESOURCE));
		editor.setWorkspaceResource(wsResource);
		IContainer c = null;
		String hint = option.getHint(UIUtils.ROOT_CONTAINER);
		if(hint!=null){
			c=(IContainer) ResourcesPlugin.getWorkspace().getRoot().findMember(new Path(hint));
		}
		editor.setRootContainer(c);
	}
	/** 
     * Gap between label and control.
     */
    protected static final int HORIZONTAL_GAP = 8;
	
	@Override
	protected void createControls(Composite composite) {
		GridLayout layout = new GridLayout();
        layout.numColumns = editor.getNumberOfControls();
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.horizontalSpacing = HORIZONTAL_GAP;
        composite.setLayout(layout);
        editor.doFillIntoGrid(composite, layout.numColumns);

        
        
	}

	@Override
	public String getValue() {
		return editor.getValue();
	}

	@Override
	public void setValue(String value) {
		editor.setValue(value);

	}

}


