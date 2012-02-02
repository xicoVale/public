/*****************************************************************************
 * This file is part of the Prolog Development Tool (PDT)
 * 
 * Author: Lukas Degener (among others) 
 * E-mail: degenerl@cs.uni-bonn.de
 * WWW: http://roots.iai.uni-bonn.de/research/pdt 
 * Copyright (C): 2004-2006, CS Dept. III, University of Bonn
 * 
 * All rights reserved. This program is  made available under the terms 
 * of the Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * In addition, you may at your option use, modify and redistribute any
 * part of this program under the terms of the GNU Lesser General Public
 * License (LGPL), version 2.1 or, at your option, any later version of the
 * same license, as long as
 * 
 * 1) The program part in question does not depend, either directly or
 *   indirectly, on parts of the Eclipse framework and
 *   
 * 2) the program part in question does not include files that contain or
 *   are derived from third-party work and are therefor covered by special
 *   license agreements.
 *   
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *   
 * ad 1: A program part is said to "depend, either directly or indirectly,
 *   on parts of the Eclipse framework", if it cannot be compiled or cannot
 *   be run without the help or presence of some part of the Eclipse
 *   framework. All java classes in packages containing the "pdt" package
 *   fragment in their name fall into this category.
 *   
 * ad 2: "Third-party code" means any code that was originaly written as
 *   part of a project other than the PDT. Files that contain or are based on
 *   such code contain a notice telling you so, and telling you the
 *   particular conditions under which they may be used, modified and/or
 *   distributed.
 ****************************************************************************/

package org.cs3.pdt.console.internal.views;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.Vector;

import org.cs3.pl.common.Debug;
import org.cs3.pl.common.Util;
import org.cs3.pl.console.CompletionResult;
import org.cs3.pl.console.ConsoleCompletionProvider;
import org.cs3.pl.metadata.Predicate;
import org.cs3.pl.prolog.PrologException;
import org.cs3.pl.prolog.PrologInterface;
import org.cs3.pl.prolog.PrologInterfaceException;
import org.cs3.pl.prolog.PrologSession;

public class PrologCompletionProvider implements ConsoleCompletionProvider {

	private class _Result implements CompletionResult {

		@Override
		public String getOriginalLineContent() {
			return line;
		}

		@Override
		public int getOriginalCaretPosition() {
			return pos;
		}

		@Override
		public String[] getOptions() {
			if (options == null) {
				return null;
			}
			String[] result = new String[options.size()];
			int i = 0;
			for (Iterator<String> it = options.iterator(); it.hasNext(); i++) {
				String o = it.next();
				result[i] = o;
			}
			return result;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.cs3.pl.views.ConsoleCompletionProvider#getCaretPosition()
		 */
		@Override
		public int getNewCaretPosition() {
			return newPos;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.cs3.pl.views.ConsoleCompletionProvider#getNewLineContent()
		 */
		@Override
		public String getNewLineContent() {
			return newLine;
		}

		String line = null;

		String newLine = null;

		int newPos = -1;

		TreeSet<String> options = null;

		int pos = -1;

	}

	TreeSet<String> completions = null;

	private PrologInterface pif;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.cs3.pl.views.ConsoleCompletionProvider#getCompletion(java.lang.String,
	 *      int)
	 */
	@Override
	public CompletionResult doCompletion(String line, int pos) {
		if (pif == null) {
			return null;
		}
		_Result r = new _Result();
		r.line = line;
		r.pos = pos;
		String head = line.substring(0, pos);
		String tail = line.substring(pos);

		String[] split = head.split("[^\\w^$]");
		String prefix = split[split.length - 1];

		Predicate[] elems = null;

		try {
			elems = getPredicatesWithPrefix(null, prefix, null);
			r.options = new TreeSet<String>();

			completions = new TreeSet<String>();
			for (int i = 0; i < elems.length; i++) {
				r.options.add(elems[i].getSignature());
				completions.add(elems[i].getFunctor());
			}
		} catch (NumberFormatException e) {
			Debug.report(e);
		} catch (PrologException e) {
			Debug.report(e);
		} catch (PrologInterfaceException e) {
			Debug.report(e);
		}

		String completion = completions == null || completions.isEmpty() ? ""
				: completions.first();
		if (elems == null || elems.length == 0) {
			r.newLine = line;
			r.newPos = pos;
		} else if (elems.length == 1) {
			r.newLine = head + completion.substring(prefix.length()) + tail;
			r.newPos = pos - prefix.length() + completion.length();
		} else {
			int commonLength = getCommonLength();
			String commonPart = completion.substring(prefix.length(),
					commonLength);
			r.newLine = head + commonPart + tail;
			r.newPos = pos - prefix.length() + commonLength;
		}
		return r;
	}

	public Predicate[] getPredicatesWithPrefix(String module, String prefix,
			String filename) throws NumberFormatException, PrologException,
			PrologInterfaceException {

		PrologSession session = pif.getSession(PrologInterface.NONE);
		try {
			if (module == null)
				module = "_";
			if (filename == null)
				filename = "_";
			String query = "pdt_search:find_pred('" + filename + "','" + prefix + "', "
					+ module + ",Name,Arity,Public,_,_)";
			List<Map<String,Object>> results = session.queryAll(query);
			List<Predicate> list = new ArrayList<Predicate>();
			for (Iterator<Map<String,Object>> it = results.iterator(); it.hasNext();) {
				Map<String,Object> result = it.next();
				boolean pub = Boolean.valueOf(result.get("Public").toString())
						.booleanValue();
				Vector<String> properties = new Vector<String>();
				if(pub) properties.add("exported");
				int arity = Integer.parseInt(result.get("Arity").toString());
				if (arity >= 0) {
					Predicate data = new Predicate(module, result.get("Name").toString(), arity, properties);
					list.add(data);
				}
			}
			return list.toArray(new Predicate[0]);
		} finally {
			if (session != null) {
				session.dispose();
			}
		}

	}

	// propably there is a smarter way of doing this...
	int getCommonLength() {
		int len = 1;
		while (true) {

			String first = Util.unquoteAtom(completions.first());
			String last = Util.unquoteAtom(completions.last());
			if (first.length() < len || last.length() < len) {
				break;
			}
			String a = first.substring(0, len);
			String b = last.substring(0, len);
			if (!a.equals(b)) {
				break;
			}
			len++;
		}
		return len - 1;
	}

	public void setPrologInterface(PrologInterface pif) {
		this.pif = pif;
	}

	public PrologInterface getPrologInterface() {
		return pif;
	}
}