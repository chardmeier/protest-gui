package protest.gui.instance;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
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

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

import protest.db.ConflictStatus;
import protest.db.TestSuiteExample;

public class InstanceWindow implements ActionListener {
	private JFrame frame_;
	private JButton prevButton_;
	private JButton nextButton_;
	private JLabel idxField_;

	private AbstractRightHandPanel rightHandPanel_;
	private ContextPanel contextPanel_;

	private JPanel instructionPanel_;
	private JLabel instructionLabel_;

	private List<TestSuiteExample> instances_;
	private TestSuiteExample current_;
	private int currentIdx_;

	private String title_;
	private int annotator_;

	public InstanceWindow(int annotator) {
		annotator_ = annotator;

		frame_ = new JFrame("PROTEST Pronoun Test Suite");
		frame_.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame_.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				if(saveAnnotations(false))
					frame_.setVisible(false);
			}
		});
		// Make sure the annotations get saved if the annotation window is open
		// and the application gets terminated without closing it first.
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			public void run() {
				// We can't stop the shutdown at this point, so we force saving
				if(frame_.isVisible())
					saveAnnotations(true);
			}
		}));
		
		// Instructions
		
		instructionPanel_ = new JPanel();
		instructionPanel_.setPreferredSize(new Dimension(1200, 60));
		instructionPanel_.setLayout(new FlowLayout(FlowLayout.LEFT));
		frame_.getContentPane().add(instructionPanel_, BorderLayout.PAGE_START);
		instructionLabel_ = new JLabel("");
		instructionLabel_.setText("<html><b>All pronouns:</b> mark whether the pronoun is correctly translated, and select the minimum number of tokens necessary for a correct translation.<br><b>Anaphoric pronouns only:</b> mark whether the antecedent head is correctly translated, and whether the pronoun translation is correct given the antecedent head.<br>Select the minimum number of tokens necessary for a correct translation of both antecedent and pronoun.</html>");
		instructionPanel_.add(instructionLabel_, BorderLayout.LINE_START);
		
		// Source and target context

		contextPanel_ = new ContextPanel();
		frame_.getContentPane().add(contextPanel_, BorderLayout.LINE_START);
		
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

		rightHandPanel_ = new AnnotationPanel();
		detailPanel.add(rightHandPanel_, BorderLayout.CENTER);

		frame_.setLocationByPlatform(true);
		frame_.pack();
	}

	private void showCurrentInstance() {
		frame_.setTitle(title_ + " - " + current_.getCandidateLocator());
		idxField_.setText(String.format("%d/%d", currentIdx_ + 1, instances_.size()));
		prevButton_.setEnabled(currentIdx_ > 0);
		nextButton_.setEnabled(currentIdx_ < instances_.size() - 1);
		rightHandPanel_.setCurrentInstance(current_);
		contextPanel_.setCurrentInstance(current_);
	}

	private boolean confirmConflict(ConflictStatus conflicts) {
		if(!conflicts.hasConflict())
			return true;

		String conflictMessage = conflicts.explain() + "Do you want to correct this?";
		int response = JOptionPane.showConfirmDialog(null, conflictMessage, "Confirm",
				JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
		return (response != JOptionPane.YES_OPTION);
	}

	private boolean saveAnnotations(boolean force) {
		if(current_ != null && current_.needsSaving()) {
			ConflictStatus conflicts = current_.checkAnnotationConflict();
			if(!force && !confirmConflict(conflicts)) 
				return false;

			current_.saveAnnotations(annotator_, conflicts.encode());
		}

		return true;
	}

	public void actionPerformed(ActionEvent e) {
		String[] cmd = e.getActionCommand().split(" ");
		if(cmd[0].equals("browse")) {
			if(saveAnnotations(false)) {
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
		}
	}

	public void setData(String title, List<TestSuiteExample> instances) {
		// save and check for conflicts if window already open
		if(!saveAnnotations(false))
			return;

		title_ = title;
		instances_ = instances;
		current_ = instances_.get(0);
		currentIdx_ = 0;
		showCurrentInstance();
	}

	public void setVisible(boolean visible) {
		frame_.setVisible(visible);
	}
}
