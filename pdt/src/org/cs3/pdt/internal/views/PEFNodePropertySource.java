package org.cs3.pdt.internal.views;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.PropertyDescriptor;

public class PEFNodePropertySource implements IPropertySource {

	private PEFNode node;

	public PEFNodePropertySource(PEFNode node) {
		this.node = node;
	}

	public Object getEditableValue() {

		return this;
	}

	public IPropertyDescriptor[] getPropertyDescriptors() {

		PropertyDescriptor[] r = new PropertyDescriptor[] {
				new PropertyDescriptor("id", "PEF Id"),
				new PropertyDescriptor("label", "Label"),
				new PropertyDescriptor("type", "PEF Type"),
				new PropertyDescriptor("start", "Start Offset"),
				new PropertyDescriptor("end", "End Offset"),
				new PropertyDescriptor("tags", "Tags") };
		for (int i = 0; i < r.length; i++) {
			r[i].setAlwaysIncompatible(true);
		}
		return r;

	}

	public Object getPropertyValue(Object id) {
		if ("label".equals(id)) {
			return node.getLabel();
		}
		if ("type".equals(id)) {
			return node.getType();
		}
		if ("start".equals(id)) {
			return node.getStartPosition();
		}
		if ("end".equals(id)) {
			return node.getEndPosition();
		}
		if ("tags".equals(id)) {
			return node.getTags();
		}
		if ("id".equals(id)) {
			return node.getId();
		}
		return null;
	}

	public boolean isPropertySet(Object id) {
		
		return false;
	}

	public void resetPropertyValue(Object id) {
		// TODO Auto-generated method stub

	}

	public void setPropertyValue(Object id, Object value) {
		// TODO Auto-generated method stub

	}

}
