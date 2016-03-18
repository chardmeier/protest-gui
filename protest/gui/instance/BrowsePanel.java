package protest.gui.instance;

import java.awt.AWTEventMulticaster;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public class BrowsePanel extends JPanel implements ActionListener {
	private JButton prevButton_;
	private JButton nextButton_;
	private JLabel idxField_;

	private ActionListener listener_ = null;
	private String id_;

	private int current_;
	private int total_;

	public BrowsePanel(String id) {
		super(new BorderLayout());

		id_ = id;

		setMaximumSize(new Dimension(300, 20));

		prevButton_ = new JButton("Previous");
		prevButton_.setActionCommand("prev");
		prevButton_.addActionListener(this);
		add(prevButton_, BorderLayout.LINE_START);
		
		idxField_ = new JLabel();
		idxField_.setPreferredSize(new Dimension(60, 20));
		idxField_.setHorizontalAlignment(SwingConstants.CENTER);
		add(idxField_, BorderLayout.CENTER);

		nextButton_ = new JButton("Next");
		nextButton_.setActionCommand("next");
		nextButton_.addActionListener(this);
		add(nextButton_, BorderLayout.LINE_END);
	}

	public void setTotal(int total) {
		total_ = total;
		current_ = 0;
		update();
	}

	public synchronized void addActionListener(ActionListener l) {
		listener_ = AWTEventMulticaster.add(listener_, l);
	}

	public synchronized void removeActionListener(ActionListener l) {
		listener_ = AWTEventMulticaster.remove(listener_, l);
	}

	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		if(cmd.equals("prev"))
			current_--;
		else if(cmd.equals("next"))
			current_++;

		update();

		String outcmd = String.format("%s %d", id_, current_);
		if(listener_ != null)
			listener_.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, outcmd));
	}

	private void update() {
		idxField_.setText(String.format("%d/%d", current_ + 1, total_));
		prevButton_.setEnabled(current_ > 0);
		nextButton_.setEnabled(current_ < total_ - 1);
	}
}
