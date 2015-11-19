package protest;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class PronounViewer implements Runnable {
	private AlignedCorpus corpus_;
	private ArrayList<Instance> instances_;
	private String[] pronouns_;
	private ArrayList<ArrayList<Instance>> confusionMatrix_;

	private InstanceWindow instWindow_;

	public PronounViewer(String corpus, String testsetfile, String dumpfile, String nnoutfile) throws IOException {
		System.err.print("Loading aligned corpus...");
		corpus_ = AlignedCorpus.loadCorpus(corpus);
		System.err.println("done.");
		System.err.print("Loading classifier data...");
		loadInstances(testsetfile, dumpfile, nnoutfile);
		System.err.println("done.");
		System.err.print("Initialising GUI...");
		instWindow_ = new InstanceWindow(pronouns_, corpus_);
		System.err.println("done.");
	}

	private double[] readMatrixRow(BufferedReader rd) throws IOException {
		String s = rd.readLine();
		if(s == null)
			throw new EOFException();
		String[] f = s.trim().split("\\p{Space}");
		double[] out = new double[f.length];
		for(int i = 0; i < f.length; i++)
			out[i] = Double.parseDouble(f[i]);
		return out;
	}

	private void loadInstances(String testsetfile, String dumpfile, String nnoutfile) throws IOException {
		ObjectInputStream dump = new ObjectInputStream(new BufferedInputStream(new FileInputStream(dumpfile)));
		ArrayList<Instance> fullcorpus = new ArrayList<Instance>();
		for(;;) {
			Object o;
			try {
				o = dump.readObject();
			} catch(EOFException e) {
				break;
			} catch(ClassNotFoundException e) {
				throw new RuntimeException(e);
			}
			fullcorpus.add((Instance) o);
		}
		dump.close();

		BufferedReader testset = new BufferedReader(new FileReader(testsetfile));
		String line;
		instances_ = new ArrayList<Instance>();
		while((line = testset.readLine()) != null)
			instances_.add(fullcorpus.get(Integer.parseInt(line) - 1));
		testset.close();
		fullcorpus = null;

		BufferedReader nnout = new BufferedReader(new FileReader(nnoutfile));

		line = nnout.readLine();
		if(line == null)
			throw new EOFException();
		pronouns_ = line.trim().split("\\p{Space}");
		
		int cmatsize = pronouns_.length * pronouns_.length;
		confusionMatrix_ = new ArrayList<ArrayList<Instance>>(cmatsize);
		for(int i = 0; i < cmatsize; i++)
			confusionMatrix_.add(new ArrayList<Instance>());

		if(!"SOLUTIONS".equals(nnout.readLine()))
			throw new IOException("Excepted SOLUTIONS header not found.");

		for(int i = 0; i < instances_.size(); i++) {
			double[] tgtp = readMatrixRow(nnout);
			instances_.get(i).setSolution(tgtp);
		}

		if(!"PREDICTIONS".equals(nnout.readLine()))
			throw new IOException("Excepted PREDICTIONS header not found.");

		for(int i = 0; i < instances_.size(); i++) {
			Instance inst = instances_.get(i);

			double[] outp = readMatrixRow(nnout);
			inst.setPredictedOutput(outp);

			double maxp = Double.NEGATIVE_INFINITY;
			int maxidx = -1;
			for(int j = 0; j < pronouns_.length; j++)
				if(outp[j] > maxp) {
					maxp = outp[j];
					maxidx = j;
				}

			confusionMatrix_.get(confusionIndex(inst.getCorrectClass(), maxidx)).add(inst);
		}

		if(!"ANTECEDENT SCORES".equals(nnout.readLine()))
			throw new IOException("Excepted ANTECEDENT SCORES header not found.");

		for(int i = 0; i < instances_.size(); i++) {
			double[] anap = readMatrixRow(nnout);
			instances_.get(i).setPredictedAnaphoraProbs(anap);
		}

		nnout.close();
	}

	private int confusionIndex(int correct, int predicted) {
		return correct * pronouns_.length + predicted;
	}

	public void run() {
		JFrame frame = new JFrame("Pronoun classifier evaluation");
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		((BorderLayout) frame.getContentPane().getLayout()).setVgap(15);

		JPanel matrix = new JPanel();
		frame.getContentPane().add(matrix, BorderLayout.PAGE_START);
		matrix.setLayout(new GridLayout(pronouns_.length + 1, pronouns_.length + 1));

		for(int i = 0; i <= pronouns_.length; i++) {
			for(int j = 0; j <= pronouns_.length; j++) {
				if(i == 0) {
					if(j == 0)
						matrix.add(new JLabel());
					else
						matrix.add(new JLabel(pronouns_[j-1]));
				} else if(j == 0)
					matrix.add(new JLabel(pronouns_[i-1]));
				else {
					int idx = confusionIndex(i-1, j-1);
					int s = confusionMatrix_.get(idx).size();
					JButton b = new JButton(Integer.toString(s));
					if(s == 0)
						b.setEnabled(false);
					b.setActionCommand(Integer.toString(idx));
					b.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							int idx = Integer.parseInt(e.getActionCommand());
							int correct = idx / pronouns_.length;
							int predicted = idx % pronouns_.length;
							instWindow_.setData(pronouns_[correct], pronouns_[predicted],
								confusionMatrix_.get(idx));
							instWindow_.setVisible(true);
						}
					});
					matrix.add(b);
				}
			}
		}

		JButton quitButton = new JButton("Quit");
		frame.getContentPane().add(quitButton, BorderLayout.CENTER);
		quitButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});

		frame.pack();
		frame.setVisible(true);
	}

	public static void main(String[] args) throws IOException {
		if(args.length != 4) {
			System.err.println("Usage: PronounViewer corpus testsetfile dumpfile nnoutfile");
			System.exit(1);
		}

		SwingUtilities.invokeLater(new PronounViewer(args[0], args[1], args[2], args[3]));
	}
}
