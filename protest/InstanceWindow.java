package protest;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import javax.swing.BorderFactory;
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

//For message dialogs
import javax.swing.JDialog;
import javax.swing.JOptionPane;

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

public class InstanceWindow implements ActionListener {
	private JFrame frame_;
	private XHTMLPanel sourceContext_;
	private XHTMLPanel targetContext_;
	private JButton prevButton_;
	private JButton nextButton_;
	private JLabel idxField_;

	private JTextArea remarksField_;
	private JRadioButton antOK_;
	private JRadioButton antBad_;
	private JRadioButton antUnset_;
	private JRadioButton prnOK_;
	private JRadioButton prnBad_;
	private JRadioButton prnUnset_;
	
	private JPanel annotationPanel_;
	private JPanel antButtonPanel_;
	private JLabel antLabel_;
	private JLabel proLabel_;
	
	private JPanel instructionPanel_;
	private JLabel instructionLabel_;

	private List<TestSuiteExample> instances_;
	private TestSuiteExample current_;
	private int currentIdx_;

	private boolean dirty_ = false;

	public InstanceWindow() {
		frame_ = new JFrame("PROTEST Pronoun Test Suite");
		frame_.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		frame_.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				saveAnnotations();
			}
		});
		// Make sure the annotations get saved if the annotation window is open
		// and the application gets terminated without closing it first.
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			public void run() {
				if(frame_.isVisible())
					saveAnnotations();
			}
		}));
		
		// Instructions
		
		instructionPanel_ = new JPanel();
		instructionPanel_.setPreferredSize(new Dimension(1200, 60));
		instructionPanel_.setLayout(new FlowLayout(FlowLayout.LEFT));
		frame_.getContentPane().add(instructionPanel_, BorderLayout.PAGE_START);
		instructionLabel_ = new JLabel("");
		instructionLabel_.setText("<html><b>All pronouns:</b> mark whether the pronoun is correctly translated, and select the minimum number of tokens necessary for a correct translation.<br><b>Anaphoric pronouns only:</b> mark whether the antecedent is correctly translated, and whether the pronoun translation is correct given the antecedent.<br>Select the minimum number of tokens necessary for a correct translation of both antecedent and pronoun.</html>");
		instructionPanel_.add(instructionLabel_, BorderLayout.LINE_START);
		
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
		targetContext_.addMouseTrackingListener(new DefaultFSMouseListener() {
			public void onMouseUp(BasicPanel panel, Box box) {
				if(box == null || box.getElement() == null)
					return;
				targetWordClicked(panel, box);
			}
		});
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
		annotationPanel_ = new JPanel(new GridLayout(4,1));

		// Antecedent correctness
		antOK_ = new JRadioButton("yes");
		antOK_.setActionCommand("ant ok");
		antOK_.addActionListener(this);
		antBad_ = new JRadioButton("no");
		antBad_.setActionCommand("ant bad");
		antBad_.addActionListener(this);
		antUnset_ = new JRadioButton("unset");
		antUnset_.setActionCommand("ant unset");
		antUnset_.addActionListener(this);
		antUnset_.setSelected(true);

		ButtonGroup antGroup = new ButtonGroup();
		antGroup.add(antOK_);
		antGroup.add(antBad_);
		antGroup.add(antUnset_);

		antButtonPanel_ = new JPanel(new FlowLayout());
		antButtonPanel_.add(antOK_);
		antButtonPanel_.add(antBad_);
		antButtonPanel_.add(antUnset_);
		
		antLabel_ = new JLabel("Antecedent correctly translated?", JLabel.CENTER);
		annotationPanel_.add(antLabel_);
		annotationPanel_.add(antButtonPanel_);

		// Pronoun correctness
		prnOK_ = new JRadioButton("yes");
		prnOK_.setActionCommand("prn ok");
		prnOK_.addActionListener(this);
		prnBad_ = new JRadioButton("no");
		prnBad_.setActionCommand("prn bad");
		prnBad_.addActionListener(this);
		prnUnset_ = new JRadioButton("unset");
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

		proLabel_ = new JLabel("", JLabel.CENTER);
		annotationPanel_.add(proLabel_);
		annotationPanel_.add(prnButtonPanel);

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
				dirty_ = true;
			}
		});

		JPanel wrapperPanel = new JPanel(new BorderLayout());
		wrapperPanel.add(annotationPanel_, BorderLayout.PAGE_START);
		wrapperPanel.add(remarksField_, BorderLayout.CENTER);
		detailPanel.add(wrapperPanel, BorderLayout.CENTER);

		frame_.pack();
	}

	private void showCurrentInstance() {
		idxField_.setText(String.format("%d/%d", currentIdx_ + 1, instances_.size()));
		prevButton_.setEnabled(currentIdx_ > 0);
		nextButton_.setEnabled(currentIdx_ < instances_.size() - 1);
		//Make "annotationPanel_" invisible if pronoun-antetecedent agreement is not required
		setAntAgreeVisible();
		setContext();
		setAnnotations();
	}

	private void setAnnotations() {
		String antecedentAnnotation = current_.getAntecedentAnnotation();
		if(antecedentAnnotation.equals("ok"))
			antOK_.setSelected(true);
		else if(antecedentAnnotation.equals("bad"))
			antBad_.setSelected(true);
		else if(antecedentAnnotation.isEmpty())
			antUnset_.setSelected(true);
		else {
			System.err.println("Unknown antecedent annotation: " + antecedentAnnotation);
			antUnset_.setSelected(true);
		}

		String anaphorAnnotation = current_.getAnaphorAnnotation();
		if(anaphorAnnotation.equals("ok"))
			prnOK_.setSelected(true);
		else if(anaphorAnnotation.equals("bad"))
			prnBad_.setSelected(true);
		else if(anaphorAnnotation.isEmpty())
			prnUnset_.setSelected(true);
		else {
			System.err.println("Unknown anaphor annotation: " + anaphorAnnotation);
			prnUnset_.setSelected(true);
		}

		// This is going to set the dirty flag, so we clear it afterwards.
		remarksField_.setText(current_.getRemarks());

		dirty_ = false;
	}

	private void saveAnnotations() {
		if(dirty_) {
			System.err.println("Saving annotations.");
			dirty_ = false; // set this now in case we get called again from an exit hook
			current_.setRemarks(remarksField_.getText());
			current_.saveAnnotations();
		} else
			System.err.println("No need to save annotations.");
	}

	private void setContext() {
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

				String approval = current_.getTokenApproval(current_.getIndex(), snt.getIndex());
				if(approval.isEmpty())
					approval = "unset";

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
	
	private void setAntAgreeVisible() {
		boolean agree = current_.getAntecedentAgreementRequired();
		if (agree==true) {
			//annotationPanel_.setVisible(true);
			antButtonPanel_.setVisible(true);
			antLabel_.setVisible(true);
			proLabel_.setText("<html><div style=\"text-align:center;\">Pronoun correctly translated<br>" +
							  "(given antecedent)?</div></html>");
		}
		else {
			//annotationPanel_.setVisible(false);
			antButtonPanel_.setVisible(false);
			antLabel_.setVisible(false);
			proLabel_.setText("<html><div style=\"text-align:center;\">Pronoun correctly translated?</div></html>");
		}
	}

	private String escapeXml(String s) {
		return s.replace("&", "&amp;").replace("\"", "&quot;")
			.replace("<", "&lt;").replace(">", "&gt;")
			.replace("'", "&apos;");
	}

	private void targetWordClicked(BasicPanel panel, Box box) {
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
			current_.setTokenApproval(line, pos, states[state]);
			dirty_ = true;
		} else if(cc[0].equals("ana")) {
			int line = Integer.parseInt(cc[1]);
			int pos = Integer.parseInt(cc[2]);
			int state = toggleHighlight(panel, box, celem, Arrays.asList(anaClasses));
			current_.setTokenApproval(line, pos, states[state]);
			dirty_ = true;
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

	public void actionPerformed(ActionEvent e) {
		String[] cmd = e.getActionCommand().split(" ");
		String conflictMessage = "";
		if(cmd[0].equals("browse")) {
			// Check if there is an annotation conflict
			boolean stayOnPage = false;
			int[] conflictList = current_.checkAnnotationConflict(); // 0=none; 1=pronoun; 2=antecedent; 3=both
			// Check if annotator wishes to make a correction
			if (conflictList[0] != 0 || conflictList[1] != 0) {
				JDialog.setDefaultLookAndFeelDecorated(true);
				switch (conflictList[0]) {
					case 0: break;
					case 1: conflictMessage += "PRONOUN: Pronoun translation marked as OK, but no tokens selected.\n";
						break;
					case 2: conflictMessage += "PRONOUN: Tokens selected, but pronoun translation not marked as OK.\n";
						break;
					default: conflictMessage += "PRONOUN: Conflicting annotations.\n";
						break;
				}
				switch (conflictList[1]) {
					case 0: break;
					case 1: conflictMessage += "ANTECEDENT: Pronoun translation marked as OK, but no tokens selected.\n";
						break;
					case 2: conflictMessage += "ANTECEDENT: Tokens selected, but pronoun translation not marked as OK.\n";
						break;
					default: conflictMessage += "ANTECEDENT: Conflicting annotations.\n";
						break;
				}
				if (!conflictMessage.equals("")){
					conflictMessage += "Do you want to correct this?";
				}
				int response = JOptionPane.showConfirmDialog(null, conflictMessage, "Confirm", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
				if (response == JOptionPane.YES_OPTION){
					stayOnPage = true;
				}
			}
			// If the annotator doesn't wish to make a correction, continue with browsing
			if (stayOnPage == false) {
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
			}
		} else if(cmd[0].equals("ant")) {
			dirty_ = true;
			current_.setAntecedentAnnotation(cmd[1]);
			System.err.println("Button change: " + e.getActionCommand());
		} else if(cmd[0].equals("prn")) {
			dirty_ = true;
			current_.setAnaphorAnnotation(cmd[1]);
			System.err.println("Button change: " + e.getActionCommand());
		}
	}

	public void setData(String title, List<TestSuiteExample> instances) {
		saveAnnotations(); // in case the window is already open
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
