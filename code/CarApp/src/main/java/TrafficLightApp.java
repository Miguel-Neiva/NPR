package src.main.java;

import org.apache.commons.lang3.Validate;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.CamBuilder;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedAcknowledgement;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedV2xMessage;
import org.eclipse.mosaic.fed.application.app.AbstractApplication;
import org.eclipse.mosaic.fed.application.app.api.CommunicationApplication;
import org.eclipse.mosaic.fed.application.app.api.os.TrafficLightOperatingSystem;
import org.eclipse.mosaic.interactions.communication.V2xMessageTransmission;
import org.eclipse.mosaic.lib.util.scheduling.Event;
import org.eclipse.mosaic.rti.TIME;

public final class TrafficLightApp extends AbstractApplication<TrafficLightOperatingSystem> implements CommunicationApplication {
    public final static String SECRET = "ABRE!";
    private final static short GREEN_DURATION = 10;

    static final String DEFAULT_PROGRAM = "3";
    private static final String GREEN_PROGRAM_R0 = "0";
    private static final String GREEN_PROGRAM_R1 = "2";

    final Integer MIN_DISTANCE = 10;

    /*
    ##########################################################################################################################################3
    */

    @Override
    public void onStartup() {
        getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");

        getLog().infoSimTime(this, "Initialize application");
        getOs().getAdHocModule().enable();
        getLog().infoSimTime(this, "Activated Wifi Module");
        setRed();

        getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");
    }

    /*
    ##########################################################################################################################################3
    */

    @Override
    public void onShutdown() {
        getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");
        getLog().infoSimTime(this, "Shutdown application");
        getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");
    }

    /*
    ##########################################################################################################################################3
    */

    private void setGreen_r0() {
        getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");

        getOs().switchToProgram(GREEN_PROGRAM_R0);
        getLog().infoSimTime(this, "Setting traffic lights to GREEN in r0");

        getOs().getEventManager().addEvent(
                getOs().getSimulationTime() + GREEN_DURATION * TIME.SECOND,
                (e) -> setRed()
        );

        getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");
    }

    /*
    ##########################################################################################################################################3
    */

    private void setGreen_r1() {
        getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");

        getOs().switchToProgram(GREEN_PROGRAM_R1);
        getLog().infoSimTime(this, "Setting traffic lights to GREEN in r1");

        getOs().getEventManager().addEvent(
                getOs().getSimulationTime() + GREEN_DURATION * TIME.SECOND,
                (e) -> setRed()
        );

        getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");
    }

    /*
    ##########################################################################################################################################3
    */

    private void setRed() {
        getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");

        getOs().switchToProgram(DEFAULT_PROGRAM);
        getLog().infoSimTime(this, "Setting traffic lights to RED");

        getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");
    }

    /*
    ##########################################################################################################################################3
    */

    @Override
    public void onMessageReceived(ReceivedV2xMessage receivedV2xMessage) {

        if (!(receivedV2xMessage.getMessage() instanceof RSUMsg)) {
            return;
        }

        getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");

        getLog().infoSimTime(this, "Received message from RSU {}", receivedV2xMessage.getMessage().toString());

        Validate.notNull(receivedV2xMessage.getMessage().getRouting().getSource().getSourcePosition(),
                "The source position of the sender cannot be null");
        if (!(receivedV2xMessage.getMessage().getRouting().getSource().getSourcePosition()
                .distanceTo(getOs().getPosition()) <= MIN_DISTANCE)) {
            getLog().infoSimTime(this, "Vehicle that sent message is too far away.");
            return;
        }

        if (DEFAULT_PROGRAM.equals(getOs().getCurrentProgram().getProgramId()) && receivedV2xMessage.getMessage().toString().equals("0")) {
            getLog().infoSimTime(this, "Changing to program 0");
            setGreen_r0();
        }
        else if (DEFAULT_PROGRAM.equals(getOs().getCurrentProgram().getProgramId()) && receivedV2xMessage.getMessage().toString().equals("2")) {
            getLog().infoSimTime(this, "Changing to program 2");
            setGreen_r1();
        }

        getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");
    }

    /*
    ##########################################################################################################################################3
    */

    @Override
    public void onAcknowledgementReceived(ReceivedAcknowledgement acknowledgedMessage) {
        // nop
    }

    @Override
    public void onCamBuilding(CamBuilder camBuilder) {
        // nop
    }

    @Override
    public void onMessageTransmitted(V2xMessageTransmission v2xMessageTransmission) {
        // nop
    }

    @Override
    public void processEvent(Event event) throws Exception {
        // nop
    }

    /*
    ##########################################################################################################################################3
    */
}



