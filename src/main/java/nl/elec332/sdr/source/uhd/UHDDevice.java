package nl.elec332.sdr.source.uhd;

import nl.elec332.sdr.lib.api.datastream.ISampleDataSetter;
import nl.elec332.sdr.lib.api.filter.IFilter;
import nl.elec332.sdr.lib.dsp.filter.FilterGenerator;
import nl.elec332.sdr.lib.extensions.SDRLibraryExtensions;
import nl.elec332.sdr.lib.source.device.AbstractNativeRFDevice;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.annotation.*;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Created by Elec332 on 24-5-2020
 */
@Opaque
@Name("uhd::usrp::multi_usrp")
@Properties(inherit = UHDLib.class)
public class UHDDevice extends AbstractNativeRFDevice<double[]> {

    @ByVal
    @Override
    @StdString
    @Name("get_pp_string")
    public native String toString();

    private static final int SAMPLES = 2040;

    public static final int[] SAMPLE_RATES;

    private int bwPercentage = 50;

    public void setBwPercentage(int bwPercentage) {
        System.out.println("BW Percentage: " + bwPercentage);
        this.bwPercentage = bwPercentage;
        setBandwidth((long) (getSampleRate() * (bwPercentage / 100d)));
    }

    private UHDLib.WrappedUHDPointer pointer;
    private UHDLib.Streamer streamer;

    void setPointer(UHDLib.WrappedUHDPointer pointer) {
        this.pointer = pointer;
    }

    protected UHDLib.WrappedUHDPointer getPointer() {
        return Objects.requireNonNull(pointer);
    }

    @ByVal
    public native UHDLib.Range get_rx_gain_range();

    @ByVal
    public native UHDLib.Range get_rx_rates();

    @Name("set_rx_bandwidth")
    public native void setBandwidth(long bandwidth);

    @Override
    protected long setDeviceFrequency(long freq) {
        return UHDLib.setFrequency(pointer, freq);
    }

    @Override
    protected long setDeviceSampleRate(long sampleRate) {
        setDeviceSampleRateLink(sampleRate);
        setBandwidth((long) (sampleRate * (bwPercentage / 100d)));
        return getSampleRate();
    }

    @Name("set_rx_rate")
    private native void setDeviceSampleRateLink(long sampleRate);

    @Override
    protected int setDeviceLNAGain(int gain) {
        setDeviceLNAGainLink(gain);
        return getLNAGain();
    }

    @Name("set_rx_gain")
    private native void setDeviceLNAGainLink(int gain);

    @Name("get_rx_gain")
    public native int getLNAGain();

    @Override
    public void setSampleData(ISampleDataSetter sampleDataSetter, double[] buffer) {
        sampleDataSetter.getData().setData(buffer);
    }

    @Override
    @Name("get_rx_rate")
    public native int getSampleRate();

    @Name("get_rx_bandwidth")
    public native int getBandwidth();

    public native @StdString
    String get_rx_antenna(@Cast("size_t") long chan);

    @Override
    public int getSamplesPerBuffer() {
        return SAMPLES;
    }

    @Override
    public double[] createBuffer() {
        return new double[SAMPLES * 2];
    }

    @Override
    @Name("get_rx_freq")
    public native long getFrequency();

    @Override
    protected void startDeviceSampling(final Consumer<Consumer<double[]>> bufferGetter) {
        if (streamer != null) {
            throw new IllegalStateException();
        }
        IFilter filter = SDRLibraryExtensions.getFilterLibrary().createFilter(FilterGenerator.makeRaiseCosine(getSampleRate(), getSampleRate() * 0.75, 0.5, 24));
        streamer = new UHDLib.Streamer(pointer, SAMPLES);
        streamer.startStream();
        new Thread(() -> {
            while (UHDDevice.this.streamer != null) {
                try {
                    int dat = streamer.receiveData();
                    if (dat != 0) {
                        throw new RuntimeException("Streamer code: " + dat);
                    }

                    bufferGetter.accept(buf -> {
                        //filter.filter(streamer.getData(), streamer.getData().getLength());
                        streamer.getData().getPointer().get(buf);
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    //stop();
                }
            }
        }).start();
    }

    @Override
    protected void stopDevice() {
        if (streamer != null) {
            UHDLib.Streamer s = streamer;
            streamer = null;
            s.stopStream();
        }
    }

    @Override
    protected void closeDevice() {
    }

    static {
        Loader.load();
        int max = 60000000;
        int step = 2000000;
        SAMPLE_RATES = new int[max / step];
        for (int i = 1; i < SAMPLE_RATES.length; i++) {
            SAMPLE_RATES[i] = step * i;
        }
    }

}
