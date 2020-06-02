package nl.elec332.sdr.source.uhd;

import nl.elec332.lib.java.swing.IDefaultListCellRenderer;
import nl.elec332.lib.java.swing.LinedGridBagConstraints;
import nl.elec332.sdr.lib.source.inputhandler.AbstractInputHandler;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;

/**
 * Created by Elec332 on 24-5-2020
 */
public class UHDInputHandler extends AbstractInputHandler<UHDDevice, double[]> {

    public UHDInputHandler() {
        List<UHDLib.DeviceAddress> addressList = UHDLib.getDevices();
        this.deviceAddress = addressList.isEmpty() ? null : addressList.get(0);
        this.gain = 20;
        this.sampleRate = 20000000;
        this.bwPercentage = 50;
    }

    private UHDLib.DeviceAddress deviceAddress;

    private int gain;
    private int sampleRate;
    private int bwPercentage;

    @Override
    protected UHDDevice createNewDevice() {
        return UHDLib.open_(deviceAddress).getDevice();
    }

    @Override
    protected void modifyNewDevice(UHDDevice device) {
        device.setSampleRate(sampleRate);
        //device.setBandwidth(sampleRate);
        device.setDeviceLNAGain(gain);
        device.addListener(this);
    }

    @Override
    public String getDisplayString() {
        return "UHD";
    }

    @Override
    public String getIdentifier() {
        return "sdr-ettus-uhd";
    }

    @Override
    protected boolean createNewInterface(JPanel panel) {
        List<UHDLib.DeviceAddress> addressList = UHDLib.getDevices();
        panel.setLayout(new GridBagLayout());
        int line = 0;
        SpinnerNumberModel model = new SpinnerNumberModel(bwPercentage, 0, 500, 1);

        JPanel line0 = new JPanel();
        line0.add(new JLabel("Device Name: "));
        if (addressList.size() > 0) {
            JComboBox<UHDLib.DeviceAddress> deviceChooser = new JComboBox<>(new Vector<>(UHDLib.getDevices()));
            deviceChooser.addActionListener(a -> deviceAddress = (UHDLib.DeviceAddress) deviceChooser.getSelectedItem());
            deviceChooser.setSelectedIndex(0);
            deviceAddress = (UHDLib.DeviceAddress) deviceChooser.getSelectedItem();
            deviceChooser.setRenderer(IDefaultListCellRenderer.getCustomName(UHDLib.DeviceAddress::getName));
            listeners.add(deviceChooser);
            line0.add(deviceChooser);
        } else {
            JComponent jlc = new JLabel("No UHD devices detected");
            jlc.setToolTipText("Restart the application to re-scan for devices.");
            line0.add(jlc);
        }
        panel.add(line0, new LinedGridBagConstraints(line).alignLeft());

        JPanel line1 = new JPanel();
        line1.add(new JLabel("Sample Rate: "));
        JComboBox<Integer> sampleBox = new JComboBox<>(new Vector<>(Arrays.stream(UHDDevice.SAMPLE_RATES).boxed().collect(Collectors.toList())));
        sampleBox.setRenderer(IDefaultListCellRenderer.getCustomName(sr -> sr / 1000000 + " MSPS"));
        sampleBox.addActionListener(a -> {
            int sampleRate = UHDDevice.SAMPLE_RATES[sampleBox.getSelectedIndex()];
            this.sampleRate = sampleRate;
            getCurrentDevice().ifPresent(d -> d.setSampleRate(sampleRate));
        });
        sampleBox.setSelectedItem(this.sampleRate);
        listeners.add(sampleBox);
        line1.add(sampleBox);
        panel.add(line1, new LinedGridBagConstraints(++line).alignLeft());

        JPanel line1p5 = new JPanel();
        line1p5.add(new JLabel("Bandwidth percentage"));
        JSpinner spinner = new JSpinner(model);
        spinner.addChangeListener(a -> {
            bwPercentage = (int) model.getNumber();
            getCurrentDevice().ifPresent(d -> d.setBwPercentage(bwPercentage));
        });
        line1p5.add(spinner);
        panel.add(line1p5, new LinedGridBagConstraints(++line).alignRight());

        JPanel line2 = new JPanel();
        line2.add(new JLabel("LNA Gain: "));
        int startVal = 3;
        JLabel lnaLabel = new JLabel(startVal * 8 + " dB  ");
        lnaLabel.setPreferredSize(new Dimension(40, lnaLabel.getPreferredSize().height));
        JSlider lnaSlider = new JSlider(0, 9, 3);
        lnaSlider.setMajorTickSpacing(1);
        lnaSlider.setPaintTicks(true);
        lnaSlider.addChangeListener(a -> {
            int gain = lnaSlider.getValue() * 8;
            lnaLabel.setText(gain + " dB");
            this.gain = gain;
            getCurrentDevice().ifPresent(d -> d.setLNAGain(gain));
        });
        line2.add(lnaSlider);
        line2.add(new JPanel());
        line2.add(lnaLabel);
        panel.add(line2, new LinedGridBagConstraints(++line));
        return true;
    }

}
