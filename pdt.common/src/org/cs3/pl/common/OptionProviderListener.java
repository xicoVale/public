package org.cs3.pl.common;

import java.util.EventListener;

public interface OptionProviderListener extends EventListener {
	public void valuesChanged(OptionProviderEvent e);
	
}