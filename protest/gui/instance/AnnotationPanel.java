package protest.gui.instance;

class AnnotationPanel extends JPanel {
	private JList tagList_;
	private DefaultListModel tagListModel_;
	private JButton removeTagButton_;
	private JComboBox newTag_;
	private DefaultComboBoxModel newTagModel_;
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
	
	public AnnotationPanel() implements ActionListener {
		super(new BorderLayout());

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
		
		antLabel_ = new JLabel("Antecedent head correctly translated?", JLabel.CENTER);
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

		JPanel tagPanel = new JPanel(new BorderLayout());
		tagPanel.setPreferredSize(new Dimension(300, 180));
		tagPanel.setBorder(BorderFactory.createTitledBorder("Tags:"));

		tagListModel_ = new DefaultListModel();
		tagList_ = new JList(tagListModel_);
		ListSelectionModel selModel = tagList_.getSelectionModel();
		selModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		selModel.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				if(e.getValueIsAdjusting())
					return;
				removeTagButton_.setEnabled(tagList_.getSelectedIndex() >= 0);
			}
		});
		tagPanel.add(new JScrollPane(tagList_), BorderLayout.CENTER);

		JPanel newTagPanel = new JPanel();
		newTagModel_ = new DefaultComboBoxModel();
		newTag_ = new JComboBox(newTagModel_);
		newTag_.setEditable(true);
		newTag_.setPrototypeDisplayValue("123456789");
		removeTagButton_ = new JButton("-");
		removeTagButton_.setActionCommand("remove-tag");
		removeTagButton_.addActionListener(this);
		removeTagButton_.setEnabled(false);
		JButton addTagButton = new JButton("+");
		addTagButton.setActionCommand("add-tag");
		addTagButton.addActionListener(this);
		newTagPanel.add(removeTagButton_);
		newTagPanel.add(newTag_);
		newTagPanel.add(addTagButton);
		tagPanel.add(newTagPanel, BorderLayout.PAGE_END);

		JPanel annotationAndTagPanel = new JPanel(new BorderLayout());
		annotationAndTagPanel.add(annotationPanel_, BorderLayout.PAGE_START);
		annotationAndTagPanel.add(tagPanel, BorderLayout.PAGE_END);

		// Text field for annotator's notes
		
		remarksField_ = new JTextArea(10, 30);
		remarksField_.setBorder(BorderFactory.createTitledBorder("Remarks:"));
		remarksField_.setEditable(true);
		remarksField_.setLineWrap(true);
		remarksField_.setWrapStyleWord(true);
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

		this.add(annotationAndTagPanel, BorderLayout.PAGE_START);
		this.add(remarksField_, BorderLayout.CENTER);
	}
}

