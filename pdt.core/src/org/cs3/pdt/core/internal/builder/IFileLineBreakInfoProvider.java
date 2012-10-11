/*****************************************************************************
 * This file is part of the Prolog Development Tool (PDT)
 * 
 * Author: Lukas Degener (among others)
 * WWW: http://sewiki.iai.uni-bonn.de/research/pdt/start
 * Mail: pdt@lists.iai.uni-bonn.de
 * Copyright (C): 2004-2012, CS Dept. III, University of Bonn
 * 
 * All rights reserved. This program is  made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 ****************************************************************************/

/*
 */
package org.cs3.pdt.core.internal.builder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.cs3.pl.common.logging.Debug;
import org.cs3.pl.parser.StringLineBreakInfoProvider;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;

/**
 */
public class IFileLineBreakInfoProvider extends StringLineBreakInfoProvider {
    
    public static String getText(IFile file) {
    	try {
    		InputStream in = file.getContents();
    		ByteArrayOutputStream out = new ByteArrayOutputStream();
    		byte[] buf = new byte[1024];
    		int read = in.read(buf);
    		while (read > 0) {
    			out.write(buf, 0, read);
    			read = in.read(buf);
    		}
    		return out.toString();
    	} catch (CoreException e) {
    	    Debug.report(e);
    	} catch (IOException e) {
    	    Debug.report(e);
    	}
    	return "";
    }
    public IFileLineBreakInfoProvider(IFile file){                
       super(getText(file));
    }
 
}


