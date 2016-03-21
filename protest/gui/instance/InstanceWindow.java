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

import protest.db.AnnotationRecord;
import protest.db.ConflictStatus;
import protest.db.TestSuiteExample;

public class InstanceWindow implements ActionListener {
	private JFrame frame_;

	private BrowsePanel browsePanel_;
	private AbstractRightHandPanel rightHandPanel_;
	private ContextPanel contextPanel_;

	private JPanel instructionPanel_;
	private JLabel instructionLabel_;

	private List<TestSuiteExample> instances_;
	private TestSuiteExample current_;
	private AnnotationRecord currentAnnotation_;

	private String title_;

	private InstanceWindowView view_;

	public InstanceWindow(InstanceWindowView view) {
		view_ = view;

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
		instructionLabel_ = new JLabel(view_.getInstructions());
		instructionPanel_.add(instructionLabel_, BorderLayout.LINE_START);
		
		// Source and target context

		contextPanel_ = new ContextPanel();
		contextPanel_.setEditable(view_.allowTokenEditing());
		view_.addNavigationListener(contextPanel_);
		frame_.getContentPane().add(contextPanel_, BorderLayout.LINE_START);
		
		BorderLayout bl = new BorderLayout();
		bl.setVgap(15);
		JPanel detailPanel = new JPanel(bl);
		detailPanel.setPreferredSize(new Dimension(300, 750));
		frame_.getContentPane().add(detailPanel, BorderLayout.CENTER);

		// Browsing buttons

		browsePanel_ = new BrowsePanel("browse");
		browsePanel_.addActionListener(this);
		detailPanel.add(browsePanel_, BorderLayout.PAGE_START);

		rightHandPanel_ = view_.getRightHandPanel();
		detailPanel.add(rightHandPanel_, BorderLayout.CENTER);

		frame_.setLocationByPlatform(true);
		frame_.pack();
	}

	private void showCurrentInstance() {
		frame_.setTitle(title_ + " - " + current_.getCandidateLocator());
		view_.setCurrentInstance(current_);
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
		if(currentAnnotation_ != null && currentAnnotation_.needsSaving()) {
			ConflictStatus conflicts = currentAnnotation_.getConflictStatus();
			if(!force && !confirmConflict(conflicts)) 
				return false;

			currentAnnotation_.saveAnnotations();
		}

		return true;
	}

	public void actionPerformed(ActionEvent e) {
		String[] cmd = e.getActionCommand().split(" ");
		if(cmd[0].equals("browse")) {
			if(saveAnnotations(false)) {
				int currentIdx = Integer.parseInt(cmd[1]);
				current_ = instances_.get(currentIdx);
				showCurrentInstance();
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

		browsePanel_.setTotal(instances_.size());

		showCurrentInstance();
	}

	public void setVisible(boolean visible) {
		frame_.setVisible(visible);
	}
}
