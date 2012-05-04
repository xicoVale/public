package org.cs3.pdt.internal.structureElements;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.cs3.pl.metadata.Predicate;

/**
 * used in prolog searches and outlines to represent a predicate.
 */
public class SearchPredicateElement extends Predicate implements PDTTreeElement{

	private static final long serialVersionUID = 8822257720982382862L;
	private PDTTreeElement parent;
	private List<PDTMatch> occurrences = new ArrayList<PDTMatch>();
	List<FileTreeElement> files = new ArrayList<FileTreeElement>();
	
	public SearchPredicateElement(String module, String predicateName, int arity, List<String> properties) {
		super(module,predicateName,arity, properties);
	}
	
	public SearchPredicateElement(String module, String predicateName, int arity) {
		super(module, predicateName, arity, new Vector<String>());
	}
	
	public void addOccurrence(PDTMatch occurrance) {
		occurrences.add(occurrance);
		boolean found = false;
		for (FileTreeElement fileTreeElement : files) {
			if (fileTreeElement.getFile().equals(occurrance.getFile())) {
				found = true;
				fileTreeElement.addChild(occurrance);
				break;
			}
		}
		if (!found) {
			FileTreeElement fileTreeElement = new FileTreeElement(occurrance.getFile());
			fileTreeElement.addChild(occurrance);
			files.add(fileTreeElement);
		}
	}

	@Override
	public boolean hasChildren() {
		return !files.isEmpty();
	}

	@Override
	public Object[] getChildren() {
		return files.toArray();
	}
	
	@Override
	public String getLabel() {
		return getFunctor() + "/" + getArity();
	}
	
	public int numberOfOccurences() {
		return occurrences.size();
	}
	
	public PDTMatch getFirstOccurrence() {
		if (files.isEmpty()) {
			return null;
		} else {
			return files.get(0).getFirstMatch();
		}
	}
	
	public PDTMatch[] getOccurrences() {
		return occurrences.toArray(new PDTMatch[occurrences.size()]);
	}

	public void setParent(PDTTreeElement parent) {
		this.parent = parent;
	}

	public PDTTreeElement getParent() {
		return parent;
	}
	
}
