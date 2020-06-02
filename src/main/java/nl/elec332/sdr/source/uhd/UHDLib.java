package nl.elec332.sdr.source.uhd;

import nl.elec332.sdr.lib.api.util.IDoubleArray;
import nl.elec332.sdr.lib.extensions.SDRLibraryExtensions;
import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Elec332 on 25-5-2020
 */
@Platform(include = "source_uhd.hpp", link = "source_uhd", preload = {"libusb-1.0", "uhd"})
public class UHDLib {

    @ByVal
    @Name("open")
    public static native WrappedUHDPointer open_(@ByVal DeviceAddress address);

    public static List<DeviceAddress> getDevices() {
        List<DeviceAddress> ret = new ArrayList<>();
        Addresses addresses = getDevicesNative();
        for (int i = 0; i < addresses.size(); i++) {
            ret.add(addresses.get(i));
        }
        return ret;
    }

    static native long setFrequency(@ByVal WrappedUHDPointer ptr, long freq);

    @ByVal
    public static native RXStreamer openDoubleStream(@ByVal WrappedUHDPointer ptr);

    static native void startStream(@ByVal RXStreamer streamer);

    static native void stopStream(@ByVal RXStreamer streamer);

    static native int receiveStream(@ByVal RXStreamer streamer, @ByVal RXMetadata metadata, @ByVal UHDData data, DoublePointer ref);

    static native int getStreamSamples(@ByVal RXStreamer streamer);

    @ByVal
    static native Addresses getDevicesNative();

    @ByVal
    static native UHDData createBuffer(int size);

    public static class Streamer {

        public Streamer(WrappedUHDPointer pointer, int samples) {
            this.streamer = openDoubleStream(pointer);
            this.metadata = new RXMetadata();
            int samplez = getStreamSamples(streamer);
            if (samples > samplez) {
                throw new IllegalArgumentException();
            }
            this.data = createBuffer(samples);
            this.array = SDRLibraryExtensions.createNewArray(samples * 2);
        }

        private final RXStreamer streamer;
        private final RXMetadata metadata;
        private final UHDData data;
        private final IDoubleArray array;

        public void startStream() {
            UHDLib.startStream(streamer);
        }

        public void stopStream() {
            UHDLib.stopStream(streamer);
        }

        public int receiveData() {
            return UHDLib.receiveStream(streamer, metadata, data, array.getPointer());
        }

        public IDoubleArray getData() {
            return array;
        }

    }

    @Opaque
    @Name("uhd::device_addr_t")
    static class DeviceAddress extends Pointer {

        @ByVal
        @StdString
        @Name("operator[]")
        public native String getProperty(@StdString String s);

        @ByVal
        @StdString
        public String getType() {
            return getProperty("type");
        }

        @ByVal
        @StdString
        public String getName() {
            return getProperty("name");
        }

        @ByVal
        @StdString
        public String getSerial() {
            return getProperty("serial");
        }

        @ByVal
        @StdString
        public String getProduct() {
            return getProperty("product");
        }

        @StdString
        public native String to_pp_string();

    }

    @Opaque
    @Name("std::vector<device_addr_t>")
    static class Addresses extends Pointer {

        @ByVal
        @Name("operator[]")
        public native DeviceAddress get(long n);

        public native long size();

    }

    @Opaque
    @Name("uhd::meta_range_t")
    static class Range extends Pointer {

        public native double start();

        public native double stop();

        public native double step();

        @StdString
        public native String to_pp_string();

    }

    @Opaque
    @Name("uhd::usrp::multi_usrp::sptr")
    static class WrappedUHDPointer extends Pointer {

        public UHDDevice getDevice() {
            UHDDevice ret = get();
            ret.setPointer(this);
            return ret;
        }

        private native UHDDevice get();

        @Name("reset")
        protected native void delete();

    }

    @Opaque
    @Name("uhd::rx_streamer::sptr")
    private static class RXStreamer extends Pointer {
    }

    @Opaque
    @Name("uhd::rx_metadata_t")
    private static class RXMetadata extends Pointer {

        public RXMetadata() {
            allocate();
        }

        private native void allocate();

    }

    @Opaque
    @Name("std::vector<std::complex<double>>")
    private static class UHDData extends Pointer {

    }

    static {
        Loader.load(UHDLib.class);
    }

}
