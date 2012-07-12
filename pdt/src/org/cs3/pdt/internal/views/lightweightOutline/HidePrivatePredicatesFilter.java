package org.cs3.pdt.internal.views.lightweightOutline;

import org.cs3.pdt.internal.structureElements.OutlineModuleElement;
import org.cs3.pdt.internal.structureElements.OutlinePredicate;
import org.cs3.pdt.internal.structureElements.PredicateOccuranceElement;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

public class HidePrivatePredicatesFilter extends ViewerFilter {

	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		if (element instanceof PredicateOccuranceElement) {
			return true;
		} else if (element instanceof OutlinePredicate) {
			OutlinePredicate p = (OutlinePredicate) element;
			return (p.isPublic() || p.isProtected() || "user".equals(p.getModule()));
		} else if (element instanceof OutlineModuleElement) {
			OutlineModuleElement m = (OutlineModuleElement) element;
			for (Object child: m.getChildren()) {
				if (select(viewer, element, child)) {
					return true;
				}
			}
			return false;
		} else {
			return true;
		}
	}

}