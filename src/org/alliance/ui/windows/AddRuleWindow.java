package org.alliance.ui.windows;

import com.stendahls.XUI.XUIDialog;
import org.alliance.core.settings.Routerule;
import org.alliance.ui.UISubsystem;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;

public class AddRuleWindow extends XUIDialog {

	private UISubsystem ui;
	private ArrayList<JRadioButton> radioButtons = new ArrayList<JRadioButton>();
	private final static String[] OPTIONS = new String[] { "ip_addr1",
			"ip_addr2", "ip_addr3", "ip_addr4", "ip_addr_mask" };
	private ArrayList<JTextField> FIELDS = new ArrayList<JTextField>();
	private boolean allow = false;
	private JButton ok;
	private String humanParsed[] = new String[5];
	private boolean isEdit = false;
	private int index;

	//This constructor is used for the Edit button
	public AddRuleWindow(UISubsystem ui, int index, String human) throws Exception {
		super(ui.getMainWindow());
		this.ui = ui;
		
		//Code here to parse the human readable stuff
		String human_copy = human.substring(human.lastIndexOf(' ')+1);
		for (int i = 0; i < 4; i++) {
			int divider = human_copy.indexOf('.');
			if(divider == -1){
				divider = human_copy.indexOf('/');
			}
			humanParsed[i] = human_copy.substring(0, divider);
			human_copy = human_copy.substring(divider+1);
		}
		humanParsed[4] = human_copy;
		isEdit=true;
		this.index = index;
		init();
		
		// Remove if it's allow or deny
		if(human.charAt(0) == 'A'){
			//Allow should be checked
			JRadioButton temp = (JRadioButton) xui.getComponent("radioAllow");
			allow=true;
			temp.setSelected(true);
		} else {
			//Deny should be checked
			JRadioButton temp = (JRadioButton) xui.getComponent("radioDeny");
			temp.setSelected(true);
		}
		display();
		ui.getMainWindow().setAddRuleWindowDialogShowing(false);
	}
	
	private void init() throws Exception{
		init(ui.getRl(), ui.getRl().getResourceStream("xui/rulewindow.xui.xml"));
		radioButtons.add((JRadioButton) xui.getComponent("radioAllow"));
		radioButtons.add((JRadioButton) xui.getComponent("radioDeny"));
		
		for (int i = 0; i < OPTIONS.length; i++) {
			FIELDS.add((JTextField) xui.getComponent(OPTIONS[i]));
			FIELDS.get(i).setText(humanParsed[i]);
		}
		FIELDS.get(0).requestFocus();
		ui.getMainWindow().setAddRuleWindowDialogShowing(true);
		ok = (JButton) xui.getComponent("accept");
	}

	public AddRuleWindow(UISubsystem ui) throws Exception {
		super(ui.getMainWindow());
		this.ui = ui;
		Arrays.fill(humanParsed, "");
		init();
		ok.setEnabled(false);
		display();
		ui.getMainWindow().setAddRuleWindowDialogShowing(false);
	}

	public void EVENT_cancel(ActionEvent a) throws Exception {
		dispose();
	}

	public void EVENT_accept(ActionEvent a) throws Exception {
		String human;
		if (allow) {
			human = "ALLOW   ";
		} else {
			human = "DENY    ";
		}
		Integer temp;
		for (int i = 0; i < OPTIONS.length - 1; i++) {
			//If it's left blank, assume a 0
			if(FIELDS.get(i).getText() == ""){
				FIELDS.get(i).setText("0");
			}
			try{
			temp = Integer.parseInt(FIELDS.get(i).getText().trim());
			if(temp <0 || temp > 255){
				JOptionPane.showMessageDialog(null, "Invalid IP Bock entered - " + temp);
				return;
			}
			human += temp + ".";
			} catch(NumberFormatException e){
				JOptionPane.showMessageDialog(null, "The block " + FIELDS.get(i).getText().trim() +" Contains a invalid character");
				return;
			}
		}
		// Kinda a hack but meh :)
		Integer mask = new Integer(FIELDS.get(OPTIONS.length - 1).getText());
		if (mask < 0 || mask > 32) {
			JOptionPane.showMessageDialog(null,
					"Please enter a mask between 0 and 32");
			return;
		}
		human = human.subSequence(0, human.length() - 1) + "/" + mask;
		if(isEdit){
			Routerule rule = ui.getCore().getSettings().getRulelist().get(index);
			rule.setHumanreadable(human);
		
		} else{
		ui.getCore().getSettings().getRulelist()
				.add(human);
		}
		dispose();
	}

	public void EVENT_radioAllow(ActionEvent a) throws Exception {
		this.allow = true;
		ok.setEnabled(true);
	}

	public void EVENT_radioDeny(ActionEvent a) throws Exception {
		this.allow = false;
		ok.setEnabled(true);
	}
}
