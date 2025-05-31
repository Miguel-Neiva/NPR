package src.main.java;

import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.AdHocModuleConfiguration;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.CamBuilder;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedAcknowledgement;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedV2xMessage;
import org.eclipse.mosaic.fed.application.app.AbstractApplication;
import org.eclipse.mosaic.fed.application.app.api.Application;
import org.eclipse.mosaic.fed.application.app.api.CommunicationApplication;
import org.eclipse.mosaic.fed.application.app.api.VehicleApplication;
import org.eclipse.mosaic.fed.application.app.api.os.VehicleOperatingSystem;
import org.eclipse.mosaic.interactions.communication.V2xMessageTransmission;
import org.eclipse.mosaic.lib.enums.AdHocChannel;
import org.eclipse.mosaic.lib.enums.SensorType;
import org.eclipse.mosaic.lib.geo.CartesianPoint;
import org.eclipse.mosaic.lib.objects.v2x.MessageRouting;
import org.eclipse.mosaic.lib.objects.vehicle.VehicleData;
import org.eclipse.mosaic.lib.util.scheduling.Event;


import java.util.List;
import java.util.Objects;

public class CarSendsCamAPP extends AbstractApplication<VehicleOperatingSystem> implements VehicleApplication, CommunicationApplication {

    //Used for choosing a RAND id for the message that is sent intra-vehicle.
    private final static int MAX_ID = 1000;

    /*
    ##########################################################################################################################################3
    */

    @Override
    public void onStartup() {
        getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");

        getLog().infoSimTime(this, "Initialize {} application", getOs().getId());
        getOs().getAdHocModule().enable(new AdHocModuleConfiguration()
                .addRadio()
                .channel(AdHocChannel.CCH)
                .power(16)
                .create());
        getLog().infoSimTime(this, "Activated AdHoc Module");

        getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");
    }

    /*
    ##########################################################################################################################################3
    */

    @Override
    public void onVehicleUpdated(VehicleData previousVehicleData, VehicleData updatedVehicleData) {
        final List<? extends Application> applications = getOs().getApplications();

        final IntraVehicleMsg message = new IntraVehicleMsg(getOs().getId(), getRandom().nextInt(0, MAX_ID));

        // Example usage for how to detect sensor readings
        if (getOs().getStateOfEnvironmentSensor(SensorType.OBSTACLE) > 0) {
            getLog().infoSimTime(this, "Reading sensor");
        }

        for (Application application : applications) {
            final Event event = new Event(getOs().getSimulationTime() + 10, application, message);
            this.getOs().getEventManager().addEvent(event);
        }

        final MessageRouting routing = getOperatingSystem()
                .getAdHocModule()
                .createMessageRouting()
                .topoBroadCast();

        String name = Objects.requireNonNull(getOs().getVehicleData()).getName();
        CartesianPoint position = getOs().getVehicleData().getPosition().toCartesian();

        double x = position.getX();
        double y = position.getY();

        getLog().infoSimTime(this, "Sent message to others cars = '{} | {}'", name, position);

        getOs().getAdHocModule().sendV2xMessage(new InterVehicleMsg(routing, name, x, y));

        /*------------------------------------------------------------------------------------------*/
    }

    /*
    ##########################################################################################################################################3
    */

    @Override
    public void onMessageReceived(ReceivedV2xMessage receivedV2xMessage) {
        //nop
    }

    /*
    ##########################################################################################################################################3
    */

    @Override
    public void processEvent(Event event) throws Exception {
        getLog().infoSimTime(this, "Received event: {}", getOs().getSimulationTimeMs(), event.getResourceClassSimpleName());
    }

    @Override
    public void onShutdown() {
        getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");
        getLog().infoSimTime(this, "Shutdown application");
        getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");
    }

    @Override
    public void onAcknowledgementReceived(ReceivedAcknowledgement acknowledgedMessage) {
    }

    @Override
    public void onCamBuilding(CamBuilder camBuilder) {
    }

    @Override
    public void onMessageTransmitted(V2xMessageTransmission v2xMessageTransmission) {
    }

    /*
    ##########################################################################################################################################3
    */

}
