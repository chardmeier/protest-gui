package ch.rax.pviewer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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

import org.xhtmlrenderer.simple.FSScrollPane;
import org.xhtmlrenderer.simple.XHTMLPanel;
import org.xhtmlrenderer.simple.extend.XhtmlNamespaceHandler;

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

	private String[] pronouns_;
	private List<Instance> instances_;
	private Instance current_;
	private int currentIdx_;

	private int nclasses_;
	private AlignedCorpus corpus_;

	public InstanceWindow(String[] pronouns, AlignedCorpus corpus) {
		pronouns_ = pronouns;
		nclasses_ = pronouns.length;
		corpus_ = corpus;

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
	}

	private void showCurrentInstance() {
		idxField_.setText(String.format("%d/%d", currentIdx_ + 1, instances_.size()));
		prevButton_.setEnabled(currentIdx_ > 0);
		nextButton_.setEnabled(currentIdx_ < instances_.size() - 1);

		setContext();
	}

	private void setContext() {
		setContext(Integer.MIN_VALUE);
	}

	private void setContext(int highlight) {
		String XHTML_HEADER =
			"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
			"<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" " +
				"\"http://www.w3.org/TR/xthml1/DTD/xhtml1-strict.dtd\">\n" +
			"<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
			"<head><title/></head>\n" +
			"<body style=\"font-family:'Lucida Sans Unicode','Lucida Typewriter','Andale Mono',monospace;font:1.2em/1.5em\">\n";

		int srcfrom = Math.max(0, current_.getFirstSourceIndex() - 10);
		int srcto = Math.min(corpus_.getSource().getSize() - 1, current_.getLastSourceIndex() + 10);
		int tgtfrom = Math.max(0, current_.getFirstTargetIndex() - 10);
		int tgtto = Math.min(corpus_.getTarget().getSize() - 1, current_.getLastTargetIndex() + 10);

		int[] ae;
		ae = corpus_.getSource().getAlignedIDs(srcfrom);
		for(int i = 0; i < ae.length; i++)
			if(ae[i] < tgtfrom)
				tgtfrom = ae[0];
		ae = corpus_.getSource().getAlignedIDs(srcto);
		for(int i = 0; i < ae.length; i++)
			if(ae[i] > tgtto)
				tgtto = ae[0];
		ae = corpus_.getTarget().getAlignedIDs(tgtfrom);
		for(int i = 0; i < ae.length; i++)
			if(ae[i] < srcfrom)
				srcfrom = ae[0];
		ae = corpus_.getTarget().getAlignedIDs(tgtto);
		for(int i = 0; i < ae.length; i++)
			if(ae[i] > srcto)
				srcto = ae[0];

		StringBuilder srchtml = new StringBuilder();
		srchtml.append(XHTML_HEADER);
		for(int i = srcfrom; i <= srcto; i++) {
			if(current_.getSourceWordLabel(i) > 0)
				srchtml.append("<span style=\"font-weight:bold;color:red\">");
			else if(current_.getSourceWordLabel(i) < 0)
				srchtml.append("<span style=\"font-weight:bold;" +
					"border-color:red;border-style:solid;border-width:medium\">");

			if(current_.getSourceWordLabel(i) == highlight)
				srchtml.append("<span style=\"border-bottom-color:black;" +
					"border-bottom-style:solid;border-bottom-width:medium\">");

			srchtml.append(escapeXml(corpus_.getSource().getElement(i)));

			if(current_.getSourceWordLabel(i) == highlight)
				srchtml.append("</span>");

			if(current_.getSourceWordLabel(i) != 0)
				srchtml.append("</span>");
			srchtml.append(' ');
		}
		srchtml.append("</body></html>");
		sourceContext_.setDocumentFromString(srchtml.toString(), "", new XhtmlNamespaceHandler());

		StringBuilder tgthtml = new StringBuilder();
		tgthtml.append(XHTML_HEADER);
		for(int i = tgtfrom; i <= tgtto; i++) {
			if(current_.getTargetWordLabel(i) > 0)
				tgthtml.append("<span style=\"font-weight:bold;color:red\">");
			else if(current_.getTargetWordLabel(i) < 0)
				tgthtml.append("<span style=\"font-weight:bold;" +
					"border-color:red;border-style:solid;border-width:medium\">");

			if(current_.getTargetWordLabel(i) == highlight)
				tgthtml.append("<span style=\"border-bottom-color:black;" +
					"border-bottom-style:solid;border-bottom-width:medium\">");

			tgthtml.append(escapeXml(corpus_.getTarget().getElement(i)));

			if(current_.getTargetWordLabel(i) == highlight)
				tgthtml.append("</span>");

			if(current_.getTargetWordLabel(i) != 0)
				tgthtml.append("</span>");
			tgthtml.append(' ');
		}
		tgthtml.append("</body></html>");
		targetContext_.setDocumentFromString(tgthtml.toString(), "", new XhtmlNamespaceHandler());
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

	public void setData(String category, List<Instance> instances) {
		instances_ = instances;
		current_ = instances_.get(0);
		currentIdx_ = 0;
		frame_.setTitle(category);
		showCurrentInstance();
	}

	public void setVisible(boolean visible) {
		frame_.setVisible(visible);
	}
};
