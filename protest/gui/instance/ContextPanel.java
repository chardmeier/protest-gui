package protest.gui.instance;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JPanel;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.xhtmlrenderer.layout.LayoutContext;
import org.xhtmlrenderer.layout.PaintingInfo;
import org.xhtmlrenderer.render.Box;
import org.xhtmlrenderer.swing.BasicPanel;
import org.xhtmlrenderer.swing.DefaultFSMouseListener;
import org.xhtmlrenderer.simple.FSScrollPane;
import org.xhtmlrenderer.simple.XHTMLPanel;
import org.xhtmlrenderer.simple.extend.XhtmlNamespaceHandler;

import protest.db.AnnotationRecord;
import protest.db.TestSuiteExample;
import protest.db.Sentence;

class ContextPanel extends JPanel {
	private TestSuiteExample current_;
	private AnnotationRecord currentAnnotation_;

	private XHTMLPanel sourceContext_;
	private XHTMLPanel targetContext_;

	private boolean editable_;

	public ContextPanel() {
		setPreferredSize(new Dimension(900, 750));
		
		BorderLayout bl = new BorderLayout();
		bl.setVgap(15);
		setLayout(bl);

		sourceContext_ = new XHTMLPanel();
		FSScrollPane srcctxpane = new FSScrollPane(sourceContext_);
		srcctxpane.setPreferredSize(new Dimension(900, 367));
		add(srcctxpane, BorderLayout.PAGE_START);

		targetContext_ = new XHTMLPanel();
		targetContext_.addMouseTrackingListener(new DefaultFSMouseListener() {
			public void onMouseUp(BasicPanel panel, Box box) {
				if(box == null || box.getElement() == null)
					return;
				targetWordClicked(panel, box);
			}
		});
		FSScrollPane tgtctxpane = new FSScrollPane(targetContext_);
		tgtctxpane.setPreferredSize(new Dimension(900, 367));
		add(tgtctxpane, BorderLayout.CENTER);

		editable_ = false;
	}

	public void setEditable(boolean editable) {
		editable_ = editable;
	}

	public boolean getEditable() {
		return editable_;
	}

	public void setCurrentInstance(TestSuiteExample current) {
		setCurrentInstance(current, null);
	}

	public void setCurrentInstance(TestSuiteExample current, AnnotationRecord rec) {
		current_ = current;
		currentAnnotation_ = rec;
		displayContext();
	}

	public void setCurrentAnnotation(AnnotationRecord rec) {
		currentAnnotation_ = rec;
		displayContext();
	}

	private void displayContext() {
		String XHTML_HEADER =
			"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
			"<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" " +
				"\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n" +
			"<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
			"<head>\n" +
			"<title/>\n" +
			"<style type=\"text/css\">\n" +
			"body { font-family:'Lucida Sans Unicode','Lucida Typewriter','Andale Mono',monospace;" +
				"font:1.2em/1.5em }\n" +
			".anaphor { font-weight: bold; background-color: yellow; padding: 3px }\n" +
			".antecedent { font-weight: bold; background-color: aqua; padding: 3px }\n" +
			".ant_unset, .ana_unset { color: black }\n" +
			//".ant_ok, .ana_ok { color: black; border-color: green; border-style: solid; border-width: medium; }\n" +
			".ant_ok { color: black; background-color: #1E90FF }\n" + //#1E90FF=dodgerblue
			".ana_ok { color: black; background-color: orange; }\n" +
			"</style>\n" +
			"</head>\n" +
			"<body>\n";

		StringBuilder srchtml = new StringBuilder();
		srchtml.append(XHTML_HEADER);
		current_.reset();
		while(current_.hasNext()) {
			current_.next();
			Sentence snt = current_.getSourceSentence();
			srchtml.append("<p>\n");
			while(snt.hasNext()) {
				snt.next();
				if(snt.highlightAsAnaphor())
					srchtml.append("<span class=\"anaphor\">");
				if(snt.highlightAsAntecedent())
					srchtml.append("<span class=\"antecedent\">");

				srchtml.append(escapeXml(snt.getToken()));

				if(snt.highlightAsAnaphor())
					srchtml.append("</span>");
				if(snt.highlightAsAntecedent())
					srchtml.append("</span>");

				srchtml.append(' ');
			}
			srchtml.append("</p>\n");
		}

		srchtml.append("</body></html>");

		sourceContext_.setDocumentFromString(srchtml.toString(), "", new XhtmlNamespaceHandler());

		StringBuilder tgthtml = new StringBuilder();
		tgthtml.append(XHTML_HEADER);
		current_.reset();
		while(current_.hasNext()) {
			current_.next();
			Sentence snt = current_.getTargetSentence();
			tgthtml.append("<p>\n");
			while(snt.hasNext()) {
				snt.next();

				String approval = getTokenApproval(current_.getIndex(), snt.getIndex());

				if(snt.highlightAsAnaphor())
					tgthtml.append(String.format("<span id=\"ana.%d.%d\" class=\"anaphor ana_%s\">",
								  current_.getIndex(), snt.getIndex(), approval));

				if(snt.highlightAsAntecedent())
					tgthtml.append(String.format("<span id=\"ant.%d.%d\" class=\"antecedent ant_%s\">",
								  current_.getIndex(), snt.getIndex(), approval));

				tgthtml.append(escapeXml(snt.getToken()));

				if(snt.highlightAsAnaphor())
					tgthtml.append("</span>");
				if(snt.highlightAsAntecedent())
					tgthtml.append("</span>");

				tgthtml.append(' ');
			}
			tgthtml.append("</p>\n");
		}

		tgthtml.append("</body></html>");

		targetContext_.setDocumentFromString(tgthtml.toString(), "", new XhtmlNamespaceHandler());
	}

	private String escapeXml(String s) {
		return s.replace("&", "&amp;").replace("\"", "&quot;")
			.replace("<", "&lt;").replace(">", "&gt;")
			.replace("'", "&apos;");
	}

	private String getTokenApproval(int line, int token) {
		if(currentAnnotation_ == null)
			return "unset";
		else {
			String approval = currentAnnotation_.getTokenApproval(line, token);
			if(approval.isEmpty())
				return "unset";
			else
				return approval;
		}
	}

	private void targetWordClicked(BasicPanel panel, Box box) {
		if(currentAnnotation_ == null || !editable_)
			return;

		String[] states = { "", "ok" };
		String[] antClasses = { "ant_unset", "ant_ok" };
		String[] anaClasses = { "ana_unset", "ana_ok" };

		String id = null;
		Element celem = null;
		for(Node node = box.getElement(); node.getNodeType() == Node.ELEMENT_NODE; node = node.getParentNode()) {
			celem = (Element) node;
			id = panel.getSharedContext().getNamespaceHandler().getID(celem);
			if(id != null)
				break;
		}
		if(id == null)
			return;
		String[] cc = id.split("\\.");
		if(cc[0].equals("ant")) {
			int line = Integer.parseInt(cc[1]);
			int pos = Integer.parseInt(cc[2]);
			int state = toggleHighlight(panel, box, celem, Arrays.asList(antClasses));
			currentAnnotation_.setTokenApproval(line, pos, states[state]);
		} else if(cc[0].equals("ana")) {
			int line = Integer.parseInt(cc[1]);
			int pos = Integer.parseInt(cc[2]);
			int state = toggleHighlight(panel, box, celem, Arrays.asList(anaClasses));
			currentAnnotation_.setTokenApproval(line, pos, states[state]);
		}
	}

	private int toggleHighlight(BasicPanel panel, Box box, Element e, List<String> classList) {
		ArrayList<String> classes = new ArrayList<String>(Arrays.asList(e.getAttribute("class").split(" ")));
		ArrayList<String> intersection = new ArrayList<String>(classes);
		intersection.retainAll(classList);
		if(intersection.size() > 1)
			throw new IllegalStateException("Multiple classes set: " + intersection.toString());
		int newstate;
		if(intersection.isEmpty())
			newstate = 0;
		else {
			newstate = (classList.indexOf(intersection.get(0)) + 1) % classList.size();
			classes.removeAll(intersection);
		}
		classes.add(classList.get(newstate));

		StringBuilder sb = new StringBuilder();
		for(String c : classes)
			sb.append(c).append(' ');
		sb.deleteCharAt(sb.length() - 1);
		e.setAttribute("class", sb.toString());

		Box tgt = box.getRestyleTarget();

		LayoutContext ctx = panel.getLayoutContext();
		if(ctx == null)
			return newstate;

		tgt.restyle(ctx);

		PaintingInfo pinfo = tgt.getPaintingInfo();
		if(pinfo != null)
			panel.repaint(new Rectangle(pinfo.getAggregateBounds()));
		else
			panel.repaint();

		return newstate;
	}
}
