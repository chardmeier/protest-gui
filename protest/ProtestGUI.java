package protest;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.GridLayout;
import java.awt.FlowLayout;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

public class ProtestGUI implements Runnable, ActionListener {
	private Database db_;
	private List<AnnotationCategory> categories_;
	private int currentCategory_;

	private InstanceWindow instWindow_;
	private JFrame frame_;
	private ArrayList<JButton> catButtons_;

	public ProtestGUI() {
		db_ = new DatabaseOpener().open();
		if(db_ == null)
			System.exit(0);
		instWindow_ = new InstanceWindow(this, 0);
	}

	public ProtestGUI(String dbfile) {
		db_ = new Database(dbfile);
		instWindow_ = new InstanceWindow(this, 0);
	}
   
	public void run() {
		frame_ = new JFrame(db_.getName());
		frame_.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		((BorderLayout) frame_.getContentPane().getLayout()).setVgap(15);
		
		JLabel instructionLabel_ = new JLabel("<html><b>Select set of instances, for annotation / browsing:</b></html>");
		JPanel instructionPanel_ = new JPanel();
		instructionPanel_.setLayout(new FlowLayout(FlowLayout.LEFT));
		instructionPanel_.add(instructionLabel_, BorderLayout.LINE_START);
		frame_.getContentPane().add(instructionPanel_, BorderLayout.PAGE_START);
		
		categories_ = db_.getCategories();

		JPanel catButtonsPanel = new JPanel(new GridLayout(categories_.size() + 1, AnnotationCategory.GROUP_COUNT + 1));
		frame_.getContentPane().add(new JScrollPane(catButtonsPanel), BorderLayout.LINE_START);
		
		catButtonsPanel.add(new JLabel());
		for(int i = 0; i < AnnotationCategory.GROUP_COUNT; i++)
			catButtonsPanel.add(new JLabel(AnnotationCategory.getGroupLabel(i), JLabel.CENTER));

		catButtons_ = new ArrayList<JButton>();
		for(int i = 0; i < categories_.size(); i++) {
			AnnotationCategory cat = categories_.get(i);
			catButtonsPanel.add(new JLabel(cat.getLabel()));
			for(int j = 0; j < AnnotationCategory.GROUP_COUNT; j++) {
				int cnt = cat.getCount(j);
				JButton button = new JButton(Integer.toString(cnt));
				button.setEnabled(cnt > 0);
				button.setBackground(AnnotationCategory.getGroupColour(j));
				button.setOpaque(true);
				button.setActionCommand(String.format("%d %d", i, j));
				button.addActionListener(this);
				catButtons_.add(button);
				catButtonsPanel.add(button);
			}
		}

		JButton quitButton = new JButton("Quit");
		frame_.getContentPane().add(quitButton, BorderLayout.PAGE_END);
		quitButton.setActionCommand("quit");
		quitButton.addActionListener(this);

		frame_.setLocationByPlatform(true);
		frame_.pack();
		frame_.setVisible(true);
	}

	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();

		if(cmd.equals("quit"))
			System.exit(0);

		String[] idx = cmd.split(" ");
		currentCategory_ = Integer.parseInt(idx[0]);
		int grp = Integer.parseInt(idx[1]);

		instWindow_.setData(categories_.get(currentCategory_).getLabel(), categories_.get(currentCategory_).getExamples(grp));
		instWindow_.setVisible(true);
	}

	public void refresh() {
		categories_ = db_.getCategories();
		for(int i = 0; i < categories_.size(); i++) {
			for(int j = 0; j < AnnotationCategory.GROUP_COUNT; j++) {
				int cnt = categories_.get(i).getCount(j);
				JButton button = catButtons_.get(i * AnnotationCategory.GROUP_COUNT + j);
				button.setText(Integer.toString(cnt));
				button.setEnabled(cnt > 0);
			}
		}
	}

	public static void main(String[] args) {
		if(args.length > 1) {
			System.err.println("Usage: ProtestGUI [dbfile]");
			System.exit(1);
		}

		ProtestGUI gui;
		if(args.length == 1)
			gui = new ProtestGUI(args[0]);
		else
			gui = new ProtestGUI();

		SwingUtilities.invokeLater(gui);
	}
}
