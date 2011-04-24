package pdt.y.view.modes;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Point2D;

import y.view.Graph2DView;

/**
 * Implements  zooming the yWorks graph in/out with the mouse wheel. 
 * Is used in the EditMode of the View 
 * @author jn
 */
public class WheelScroller implements MouseWheelListener
{
	protected Graph2DView view;

	public WheelScroller(Graph2DView view)
	{
		this.view = view;
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent event)
	{
		Point2D centerPoint = view.getCenter();
		Point viewpoint = view.getViewPoint();
		Dimension viewSize = view.getViewSize();
		Rectangle rectangle = view.getWorldRect();

		if (isMouseWheelRotatedUp(event))
		{
			if (rectangle.getY() + rectangle.getHeight() - 1 > viewpoint.getY() + viewSize.height / view.getZoom())
				centerPoint.setLocation(centerPoint.getX(), centerPoint.getY() + event.getScrollAmount());
		}
		else
		{
			if (rectangle.getY() + 1 < viewpoint.getY())
				centerPoint.setLocation(centerPoint.getX(), centerPoint.getY() - event.getScrollAmount());
		}

		if (rectangle.contains(centerPoint))
		{
			view.setCenter(centerPoint.getX(), centerPoint.getY());
			view.updateView();
		}
	}

	private boolean isMouseWheelRotatedUp(MouseWheelEvent event) {
		return event.getWheelRotation() >= 0;
	}
}
