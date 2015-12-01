package protest;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.xhtmlrenderer.resource.FSEntityResolver;
import org.xhtmlrenderer.simple.FSScrollPane;
import org.xhtmlrenderer.simple.XHTMLPanel;
import org.xhtmlrenderer.simple.extend.XhtmlNamespaceHandler;

import org.xml.sax.InputSource;

public class InstanceWindow implements ActionListener {
	private JFrame frame_;
	private XHTMLPanel sourceContext_;
	private XHTMLPanel targetContext_;
	private JButton prevButton_;
	private JButton nextButton_;
	private JLabel idxField_;
	private JLabel[] outProbLabel_;
	private JPanel[] outProbBox_;
	private JTextField ngram_;
	private DefaultListModel antecedents_;
	private JList antecedentList_;

	private List<TestSuiteExample> instances_;
	private TestSuiteExample current_;
	private int currentIdx_;

	private DocumentBuilder xml_;

	public InstanceWindow() {
		frame_ = new JFrame("PROTEST Pronoun Test Suite");
		frame_.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		
		JPanel contextPanel = new JPanel();
		contextPanel.setPreferredSize(new Dimension(900, 750));
		frame_.getContentPane().add(contextPanel, BorderLayout.LINE_START);
		//contextPanel.setLayout(new BoxLayout(contextPanel, BoxLayout.PAGE_AXIS));
		BorderLayout bl = new BorderLayout();
		bl.setVgap(15);
		contextPanel.setLayout(bl);

		sourceContext_ = new XHTMLPanel();
		FSScrollPane srcctxpane = new FSScrollPane(sourceContext_);
		srcctxpane.setPreferredSize(new Dimension(900, 367));
		contextPanel.add(srcctxpane, BorderLayout.PAGE_START);

		targetContext_ = new XHTMLPanel();
		FSScrollPane tgtctxpane = new FSScrollPane(targetContext_);
		tgtctxpane.setPreferredSize(new Dimension(900, 367));
		contextPanel.add(tgtctxpane, BorderLayout.CENTER);

		bl = new BorderLayout();
		bl.setVgap(15);
		JPanel detailPanel = new JPanel(bl);
		detailPanel.setPreferredSize(new Dimension(300, 750));
		frame_.getContentPane().add(detailPanel, BorderLayout.CENTER);

		JPanel browsePanel = new JPanel(new BorderLayout());
		detailPanel.add(browsePanel, BorderLayout.PAGE_START);
		browsePanel.setMaximumSize(new Dimension(300, 20));

		prevButton_ = new JButton("Previous");
		prevButton_.setActionCommand("prev");
		prevButton_.addActionListener(this);
		browsePanel.add(prevButton_, BorderLayout.LINE_START);
		
		idxField_ = new JLabel();
		idxField_.setPreferredSize(new Dimension(60, 20));
		idxField_.setHorizontalAlignment(SwingConstants.CENTER);
		browsePanel.add(idxField_, BorderLayout.CENTER);

		nextButton_ = new JButton("Next");
		nextButton_.setActionCommand("next");
		nextButton_.addActionListener(this);
		browsePanel.add(nextButton_, BorderLayout.LINE_END);

		frame_.pack();

		try {
			DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
			fac.setValidating(false);
			fac.setFeature("http://xml.org/sax/features/validation", false);
			fac.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
			fac.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
			xml_ = fac.newDocumentBuilder();
			xml_.setEntityResolver(FSEntityResolver.instance());
		} catch(ParserConfigurationException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	private void showCurrentInstance() {
		idxField_.setText(String.format("%d/%d", currentIdx_ + 1, instances_.size()));
		prevButton_.setEnabled(currentIdx_ > 0);
		nextButton_.setEnabled(currentIdx_ < instances_.size() - 1);

		setContext();
	}

	private void setContext() {
		String XHTML_HEADER =
			"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
			"<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" " +
				"\"http://www.w3.org/TR/xthml1/DTD/xhtml1-strict.dtd\">\n" +
			"<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
			"<head><title/></head>\n" +
			"<body style=\"font-family:'Lucida Sans Unicode','Lucida Typewriter','Andale Mono',monospace;font:1.2em/1.5em\">\n";

		StringBuilder srchtml = new StringBuilder();
		srchtml.append(XHTML_HEADER);
		current_.reset();
		while(current_.hasNext()) {
			current_.next();
			Sentence snt = current_.getSourceSentence();
			while(snt.hasNext()) {
				snt.next();
				if(snt.highlightAsAnaphor())
					srchtml.append("<span style=\"font-weight:bold;" +
						"border-color:red;border-style:solid;border-width:medium\">");
				if(snt.highlightAsAntecedent())
					srchtml.append("<span style=\"font-weight:bold;color:red\">");

				srchtml.append(escapeXml(snt.getToken()));

				if(snt.highlightAsAnaphor())
					srchtml.append("</span>");
				if(snt.highlightAsAntecedent())
					srchtml.append("</span>");
			}
			srchtml.append("<br/>\n");
		}

//		for(int i = srcfrom; i <= srcto; i++) {
//			if(current_.getSourceWordLabel(i) > 0)
//				srchtml.append("<span style=\"font-weight:bold;color:red\">");
//			else if(current_.getSourceWordLabel(i) < 0)
//				srchtml.append("<span style=\"font-weight:bold;" +
//					"border-color:red;border-style:solid;border-width:medium\">");
//
//			if(current_.getSourceWordLabel(i) == highlight)
//				srchtml.append("<span style=\"border-bottom-color:black;" +
//					"border-bottom-style:solid;border-bottom-width:medium\">");
//
//			srchtml.append(escapeXml(corpus_.getSource().getElement(i)));
//
//			if(current_.getSourceWordLabel(i) == highlight)
//				srchtml.append("</span>");
//
//			if(current_.getSourceWordLabel(i) != 0)
//				srchtml.append("</span>");
//			srchtml.append(' ');
//		}

		srchtml.append("</body></html>");

		InputSource srcis = new InputSource();
		srcis.setCharacterStream(new StringReader(srchtml.toString()));
		try {
			sourceContext_.setDocument(xml_.parse(srcis));
		} catch(Exception e) {
			e.printStackTrace();
			System.err.println("Error parsing source XHTML:\n" + srchtml.toString());
			System.exit(1);
		}

		//sourceContext_.setDocumentFromString(srchtml.toString(), "", new XhtmlNamespaceHandler());

		StringBuilder tgthtml = new StringBuilder();
		tgthtml.append(XHTML_HEADER);
		current_.reset();
		while(current_.hasNext()) {
			current_.next();
			Sentence snt = current_.getSourceSentence();
			while(snt.hasNext()) {
				snt.next();
				if(snt.highlightAsAnaphor())
					tgthtml.append("<span style=\"font-weight:bold;" +
						"border-color:red;border-style:solid;border-width:medium\">");
				if(snt.highlightAsAntecedent())
					tgthtml.append("<span style=\"font-weight:bold;color:red\">");

				tgthtml.append(escapeXml(snt.getToken()));

				if(snt.highlightAsAnaphor())
					tgthtml.append("</span>");
				if(snt.highlightAsAntecedent())
					tgthtml.append("</span>");
			}
			tgthtml.append("<br/>\n");
		}

		tgthtml.append("</body></html>");

		InputSource tgtis = new InputSource();
		tgtis.setCharacterStream(new StringReader(tgthtml.toString()));
		try {
			sourceContext_.setDocument(xml_.parse(tgtis));
		} catch(Exception e) {
			e.printStackTrace();
			System.err.println("Error parsing target XHTML:\n" + tgthtml.toString());
			System.exit(1);
		}

		//targetContext_.setDocumentFromString(tgthtml.toString(), "", new XhtmlNamespaceHandler());
	}

	private String escapeXml(String s) {
		return s.replace("&", "&amp;").replace("\"", "&quot;")
			.replace("<", "&lt;").replace(">", "&gt;")
			.replace("'", "&apos;");
	}

	public void actionPerformed(ActionEvent e) {
		if(e.getActionCommand().equals("prev")) {
			currentIdx_--;
			current_ = instances_.get(currentIdx_);
			showCurrentInstance();
		} else if(e.getActionCommand().equals("next")) {
			currentIdx_++;
			current_ = instances_.get(currentIdx_);
			showCurrentInstance();
		}
	}

	public void setData(String title, List<TestSuiteExample> instances) {
		instances_ = instances;
		current_ = instances_.get(0);
		currentIdx_ = 0;
		frame_.setTitle(title);
		showCurrentInstance();
	}

	public void setVisible(boolean visible) {
		frame_.setVisible(visible);
	}
}
