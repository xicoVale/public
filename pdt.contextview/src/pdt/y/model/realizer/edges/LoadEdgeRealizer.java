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

package pdt.y.model.realizer.edges;

import java.awt.Color;

import pdt.y.model.GraphModel;
import y.view.Arrow;
import y.view.EdgeRealizer;

public class LoadEdgeRealizer extends EdgeRealizerBase {
	private GraphModel model;
	
	public LoadEdgeRealizer() {
		super();
		init();
	}
	
	public LoadEdgeRealizer(EdgeRealizer realizer){
		super(realizer);
		if(realizer instanceof LoadEdgeRealizer)
		{
			LoadEdgeRealizer sr = (LoadEdgeRealizer)realizer;
			model	= sr.model;
		}
		init();
	}
	
	public LoadEdgeRealizer(GraphModel model) {
		this.model = model;
	}

	private void init() {
		setSourceArrow(Arrow.DELTA);
		setLineColor(Color.GRAY);
		
//		byte myStyle = LineType.LINE_3.getLineStyle();
//		LineType myLineType = LineType.getLineType(1,myStyle);
//		setLineType(myLineType);
	}
	
	@Override
	public String getInfoText() {
		return model.getDataHolder().getModuleImportedPredicates(getEdge());
	}
}


