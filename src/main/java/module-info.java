import nl.elec332.sdr.lib.api.source.IInputHandler;
import nl.elec332.sdr.source.uhd.UHDInputHandler;

/**
 * Created by Elec332 on 2-6-2020
 */
module nl.elec332.sdr.source.uhd {

    requires org.bytedeco.javacpp;
    requires transitive nl.elec332.sdr.lib;
    requires java.desktop;

    provides IInputHandler with UHDInputHandler;

}