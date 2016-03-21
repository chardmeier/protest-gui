package protest.gui.instance;

import javax.swing.table.DefaultTableCellRenderer;

class TickCellRenderer extends DefaultTableCellRenderer {
	public void setValue(Object value) {
		String v = value.toString();
		String display = "X";

		if(v.equals("ok"))
			display = "+";
		else if(v.equals("bad"))
			display = "-";
		else if(v.equals("unset"))
			display = "?";
		else if(v.isEmpty())
			display = "";

		setText(display);
	}
}

