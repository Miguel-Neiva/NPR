package src.main.java;

import org.eclipse.mosaic.fed.application.app.AbstractApplication;
import org.eclipse.mosaic.fed.application.app.api.os.VehicleOperatingSystem;
import org.eclipse.mosaic.lib.util.scheduling.Event;

/**
 * Receiving application that reacts on events passed from another
 * app running on the same vehicle.
 */
public class EventProcessingApp extends AbstractApplication<VehicleOperatingSystem> {

    @Override
    public void processEvent(Event event) {
        getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");

        Object resource = event.getResource();
        if (resource != null) {
            if (resource instanceof IntraVehicleMsg) {
                final IntraVehicleMsg message = (IntraVehicleMsg) resource;
                // message was passed from another app on the same vehicle
                if (message.getOrigin().equals(getOs().getId())) {
                    getLog().infoSimTime(this, "Received message from another application: {}", message.toString());
                }
            }
        }
        getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");
    }

    /*
    ##########################################################################################################################################3
    */

    @Override
    public void onStartup() {
        getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");
        getLog().infoSimTime(this, "Initialize application");
        getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");
    }

    @Override
    public void onShutdown() {
        getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");
        getLog().infoSimTime(this, "Shutdown application");
        getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");
    }

    /*
    ##########################################################################################################################################3
    */

}
