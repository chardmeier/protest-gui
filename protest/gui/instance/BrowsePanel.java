package protest.gui.instance;

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

	private BrowsingListener listener_ = null;
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

	public void setBrowsingListener(BrowsingListener l) {
		listener_ = l;
	}

	public void actionPerformed(ActionEvent e) {
		int target = 0;

		String cmd = e.getActionCommand();
		if(cmd.equals("prev"))
			target = current_ - 1;
		else if(cmd.equals("next"))
			target = current_ + 1;

		if(listener_ == null || listener_.browseTo(id_, target)) {
			current_ = target;
			update();
		}
	}

	private void update() {
		idxField_.setText(String.format("%d/%d", current_ + 1, total_));
		prevButton_.setEnabled(current_ > 0);
		nextButton_.setEnabled(current_ < total_ - 1);
	}
}
