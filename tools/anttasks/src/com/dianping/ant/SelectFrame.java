package com.dianping.ant;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class SelectFrame extends Frame {
	private static final long serialVersionUID = 1L;

	String result;

	Button ok;
	TextField text;
	List list;

	public SelectFrame(final String[] options) {
		super("Select");
		setFont(new Font("Helvetica", Font.PLAIN, 14));

		this.setSize(400, 500);

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				setResult("");
			}
		});

		Panel p = new Panel();
		BorderLayout l = new BorderLayout();
		p.setLayout(l);
		ok = new Button("  Launch  ");
		ok.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setResult(text.getText());
			}
		});
		l.addLayoutComponent(ok, BorderLayout.EAST);
		p.add(ok);

		Label label = new Label("myapp://");
		l.addLayoutComponent(label, BorderLayout.WEST);
		p.add(label);

		text = new TextField();
		l.addLayoutComponent(text, BorderLayout.CENTER);
		p.add(text);

		list = new List();
		for (String str : options) {
			list.add(str);
		}
		list.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				int i = list.getSelectedIndex();
				if (i < 0) {
					return;
				}
				text.setText(list.getItem(i));
			}
		});
		list.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					int i = list.getSelectedIndex();
					if (i < 0) {
						return;
					}
					setResult(list.getItem(i));
				}
			}
		});
		list.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				int i = list.getSelectedIndex();
				if (i < 0) {
					return;
				}
				setResult(list.getItem(i));
			}
		});

		l = new BorderLayout();
		setLayout(l);
		l.addLayoutComponent(list, BorderLayout.CENTER);
		l.addLayoutComponent(p, BorderLayout.SOUTH);
		add(p);
		add(list);
	}

	public String result() {
		return result;
	}

	public String doModel() {
		setVisible(true);
		synchronized (this) {
			try {
				wait();
			} catch (Exception e) {
				e.printStackTrace(System.err);
			}
		}
		return result;
	}

	private void setResult(String r) {
		result = r;
		synchronized (SelectFrame.this) {
			SelectFrame.this.notify();
		}
		dispose();
	}
}