package com.udacity.sercurity.application;

import com.udacity.sercurity.data.ArmingStatus;
import com.udacity.sercurity.service.SecurityService;
import com.udacity.sercurity.service.StyleService;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/** JPanel containing the buttons to manipulate arming status of the system. */
public class ControlPanel extends JPanel {

  private SecurityService securityService;
  private Map<ArmingStatus, JButton> buttonMap;

  private SensorPanel sensorPanel;

  public ControlPanel(SecurityService securityService, SensorPanel sensorPanel) {
    super();
    setLayout(new MigLayout());
    this.securityService = securityService;
    this.sensorPanel = sensorPanel;

    JLabel panelLabel = new JLabel("System Control");
    panelLabel.setFont(StyleService.HEADING_FONT);

    add(panelLabel, "span 3, wrap");

    // create a map of each status type to a corresponding JButton
    buttonMap =
        Arrays.stream(ArmingStatus.values())
            .collect(
                Collectors.toMap(status -> status, status -> new JButton(status.getDescription())));

    // add an action listener to each button that applies its arming status and recolors all the
    // buttons
    buttonMap.forEach(
        (k, v) -> {
          v.addActionListener(
              e -> {
                securityService.setArmingStatus(k);
                buttonMap.forEach(
                    (status, button) ->
                        button.setBackground(status == k ? status.getColor() : null));
              });
          sensorPanel.sensorStatusChanged();
        });

    // map order above is arbitrary, so loop again in order to add buttons in enum-order
    Arrays.stream(ArmingStatus.values()).forEach(status -> add(buttonMap.get(status)));

    ArmingStatus currentStatus = securityService.getArmingStatus();
    buttonMap.get(currentStatus).setBackground(currentStatus.getColor());
  }
}
