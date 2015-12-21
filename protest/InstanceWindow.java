package protest;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;

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
	private JButton saveButton_;

	private JTextArea remarksField_;
	private JRadioButton antOK_;
	private JRadioButton antBad_;
	private JRadioButton antNA_;
	private JRadioButton antUnset_;
	private JRadioButton prnOK_;
	private JRadioButton prnBad_;
	private JRadioButton prnUnset_;

	private List<TestSuiteExample> instances_;
	private TestSuiteExample current_;
	private int currentIdx_;

	public InstanceWindow() {
		frame_ = new JFrame("PROTEST Pronoun Test Suite");
		frame_.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		frame_.addWindowListener(new WindowAdapter() {
			public void windowClosing() {
				saveAnnotations();
			}
		});
		
		// Source and target context

		JPanel contextPanel = new JPanel();
		contextPanel.setPreferredSize(new Dimension(900, 750));
		frame_.getContentPane().add(contextPanel, BorderLayout.LINE_START);
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

		// Browsing buttons

		JPanel browsePanel = new JPanel(new BorderLayout());
		detailPanel.add(browsePanel, BorderLayout.PAGE_START);
		browsePanel.setMaximumSize(new Dimension(300, 20));

		prevButton_ = new JButton("Previous");
		prevButton_.setActionCommand("browse prev");
		prevButton_.addActionListener(this);
		browsePanel.add(prevButton_, BorderLayout.LINE_START);
		
		idxField_ = new JLabel();
		idxField_.setPreferredSize(new Dimension(60, 20));
		idxField_.setHorizontalAlignment(SwingConstants.CENTER);
		browsePanel.add(idxField_, BorderLayout.CENTER);

		nextButton_ = new JButton("Next");
		nextButton_.setActionCommand("browse next");
		nextButton_.addActionListener(this);
		browsePanel.add(nextButton_, BorderLayout.LINE_END);

		// Annotation buttons
		
		JPanel annotationPanel = new JPanel(new GridLayout(4,1));

		// Antecedent correctness
		JRadioButton antOK_ = new JRadioButton("yes");
		antOK_.setActionCommand("ant ok");
		antOK_.addActionListener(this);
		JRadioButton antBad_ = new JRadioButton("no");
		antBad_.setActionCommand("ant bad");
		antBad_.addActionListener(this);
		JRadioButton antNA_ = new JRadioButton("n/a");
		antNA_.setActionCommand("ant na");
		antNA_.addActionListener(this);
		JRadioButton antUnset_ = new JRadioButton("unset");
		antUnset_.setActionCommand("ant unset");
		antUnset_.addActionListener(this);
		antUnset_.setSelected(true);

		ButtonGroup antGroup = new ButtonGroup();
		antGroup.add(antOK_);
		antGroup.add(antBad_);
		antGroup.add(antNA_);
		antGroup.add(antUnset_);

		JPanel antButtonPanel = new JPanel(new FlowLayout());
		antButtonPanel.add(antOK_);
		antButtonPanel.add(antBad_);
		antButtonPanel.add(antNA_);
		antButtonPanel.add(antUnset_);

		annotationPanel.add(new JLabel("Antecedent correctly translated?", JLabel.CENTER));
		annotationPanel.add(antButtonPanel);

		// Pronoun correctness
		JRadioButton prnOK_ = new JRadioButton("yes");
		prnOK_.setActionCommand("prn ok");
		prnOK_.addActionListener(this);
		JRadioButton prnBad_ = new JRadioButton("no");
		prnBad_.setActionCommand("prn bad");
		prnBad_.addActionListener(this);
		JRadioButton prnUnset_ = new JRadioButton("unset");
		prnUnset_.setActionCommand("prn unset");
		prnUnset_.addActionListener(this);
		prnUnset_.setSelected(true);

		ButtonGroup prnGroup = new ButtonGroup();
		prnGroup.add(prnOK_);
		prnGroup.add(prnBad_);
		prnGroup.add(prnUnset_);

		JPanel prnButtonPanel = new JPanel(new FlowLayout());
		prnButtonPanel.add(prnOK_);
		prnButtonPanel.add(prnBad_);
		prnButtonPanel.add(prnUnset_);

		annotationPanel.add(new JLabel(
					"<html><div style=\"text-align:center;\">Pronoun correctly translated<br>" +
					"(given antecedent)?</div></html>",
					JLabel.CENTER));
		annotationPanel.add(prnButtonPanel);

		// Text field for annotator's notes
		
		remarksField_ = new JTextArea(10, 30);
		//remarksField_.setBorder(BorderFactory.createLineBorder(Color.black));
		remarksField_.setBorder(BorderFactory.createTitledBorder("Remarks:"));
		remarksField_.setEditable(true);
		remarksField_.getDocument().addDocumentListener(new DocumentListener() {
			public void changedUpdate(DocumentEvent e) {
				update(e);
			}
			public void insertUpdate(DocumentEvent e) {
				update(e);
			}
			public void removeUpdate(DocumentEvent e) {
				update(e);
			}
			private void update(DocumentEvent e) {
				saveButton_.setEnabled(true);
			}
		});

		saveButton_ = new JButton("Save");
		saveButton_.setEnabled(false);
		saveButton_.setActionCommand("save remarks");
		saveButton_.addActionListener(this);

		JPanel remarksPanel = new JPanel(new BorderLayout());
		remarksPanel.add(remarksField_, BorderLayout.CENTER);
		remarksPanel.add(saveButton_, BorderLayout.PAGE_END);

		JPanel wrapperPanel = new JPanel(new BorderLayout());
		wrapperPanel.add(annotationPanel, BorderLayout.PAGE_START);
		wrapperPanel.add(remarksPanel, BorderLayout.CENTER);
		detailPanel.add(wrapperPanel, BorderLayout.CENTER);

		frame_.pack();
	}

	private void showCurrentInstance() {
		idxField_.setText(String.format("%d/%d", currentIdx_ + 1, instances_.size()));
		prevButton_.setEnabled(currentIdx_ > 0);
		nextButton_.setEnabled(currentIdx_ < instances_.size() - 1);

		setContext();
		loadAnnotations();
	}

	private void loadAnnotations() {
	}

	private void saveAnnotations() {
	}

	private void setContext() {
		String XHTML_HEADER =
			"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
			"<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" " +
				"\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n" +
			"<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
			"<head><title/></head>\n" +
			"<body style=\"font-family:'Lucida Sans Unicode','Lucida Typewriter','Andale Mono',monospace;font:1.2em/1.5em\">\n";

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
					srchtml.append("<span style=\"font-weight:bold;" +
						"border-color:red;border-style:solid;border-width:medium\">");
				if(snt.highlightAsAntecedent())
					srchtml.append("<span style=\"font-weight:bold;color:red\">");

				srchtml.append(escapeXml(snt.getToken()));

				if(snt.highlightAsAnaphor())
					srchtml.append("</span>");
				if(snt.highlightAsAntecedent())
					srchtml.append("</span>");

				srchtml.append(' ');
			}
			srchtml.append("</p>\n");
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

	public void actionPerformed(ActionEvent e) {
		String[] cmd = e.getActionCommand().split(" ");
		if(cmd[0].equals("browse")) {
			saveAnnotations();
			if(cmd[1].equals("prev")) {
				currentIdx_--;
				current_ = instances_.get(currentIdx_);
				showCurrentInstance();
			} else if(cmd[1].equals("next")) {
				currentIdx_++;
				current_ = instances_.get(currentIdx_);
				showCurrentInstance();
			}
		} else if(cmd[0].equals("ant")) {
			System.err.println("Button change: " + e.getActionCommand());
		} else if(cmd[0].equals("prn")) {
			System.err.println("Button change: " + e.getActionCommand());
		} else if(cmd[0].equals("save") && cmd[1].equals("remarks")) {
			try {
				System.err.println("Save remarks:\n" +
					remarksField_.getDocument().getText(0, remarksField_.getDocument().getLength()));
			} catch(BadLocationException ex) {}
			saveButton_.setEnabled(false);
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
